/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package hd3gtv.mydmam.embddb.network;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.tools.ThreadPoolExecutorFactory;

/**
 * @see https://github.com/Oreste-Luci/apache-tomcat-8.0.26-src/blob/master/java/org/apache/tomcat/websocket/AsyncChannelWrapperSecure.java
 */
@GsonIgnore
public abstract class TLSSocketHandler {
	
	private static Logger log = Logger.getLogger(TLSSocketHandler.class);
	
	private static final int MAX_PAYLOAD_SIZE = 0xFFFF;
	private static final long TIME_TO_GET_SYNC_NETWORK_IO = TimeUnit.SECONDS.toMillis(10);
	
	private static final ByteBuffer HAND_SHAKE_DUMMY = ByteBuffer.allocate(8192);
	private final AsynchronousSocketChannel socket_channel;
	private final SSLEngine ssl_engine;
	private final ByteBuffer socket_received_buffer;
	private final ByteBuffer socket_sended_buffer;
	protected final ByteBuffer data_payload_received_buffer;
	
	private final ThreadPoolExecutorFactory executor;
	private AtomicBoolean writing = new AtomicBoolean(false);
	private AtomicBoolean reading = new AtomicBoolean(false);
	
	public TLSSocketHandler(AsynchronousSocketChannel socket_channel, SSLEngine ssl_engine, ThreadPoolExecutorFactory executor) {
		this.socket_channel = socket_channel;
		this.ssl_engine = ssl_engine;
		this.executor = executor;
		
		/**
		 * 1 for read and 1 for write, client and server side.
		 */
		executor.setMinimumPoolSize(4);
		
		int socket_buffer_size = ssl_engine.getSession().getPacketBufferSize();
		socket_received_buffer = ByteBuffer.allocateDirect(socket_buffer_size);
		socket_sended_buffer = ByteBuffer.allocateDirect(socket_buffer_size);
		data_payload_received_buffer = ByteBuffer.allocateDirect(ssl_engine.getSession().getApplicationBufferSize() + MAX_PAYLOAD_SIZE);
	}
	
	/**
	 * Non blocking, only callback the next data block.
	 */
	private void readNext() {
		if (!reading.compareAndSet(false, true)) {
			throw new IllegalStateException("Concurrent read operations are not permitted");
		}
		
		socket_received_buffer.clear();
		
		socket_channel.read(socket_received_buffer, null, new CompletionHandler<Integer, Void>() {
			public void completed(Integer size, Void _void) {
				try {
					decrypt();
					reading.set(false);
					
					boolean restart_read = onGetDatas(false);
					if (restart_read) {
						readNext();
					}
					
				} catch (IOException e) {
					failed(e, null);
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					log.error("General process error with socket", e);
				}
				reading.set(false);
			}
			
			public void failed(Throwable e, Void kit) {
				reading.set(false);
				if (e instanceof AsynchronousCloseException) {
					onIOButClosed((AsynchronousCloseException) e);
				} else {
					onIOExceptionCauseClosing(e);
					close();
				}
			}
		});
	}
	
	private void decrypt() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		socket_received_buffer.flip();
		data_payload_received_buffer.clear();
		while (true) {
			
			boolean force_read = false;
			
			if (socket_received_buffer.hasRemaining()) {
				SSLEngineResult r = ssl_engine.unwrap(socket_received_buffer, data_payload_received_buffer);
				Status s = r.getStatus();
				
				switch (s) {
				case OK:
					/** Bytes available for reading and there may be sufficient data in the socketReadBuffer to support further reads without reading from the socket */
					log.trace("ok: " + r.bytesProduced() + " > " + data_payload_received_buffer.position());
					break;
				case BUFFER_UNDERFLOW:
					/**
					 * There is partial data in the socketReadBuffer
					 */
					if (r.bytesProduced() == 0) {
						/** Need more data before the partial data can be processed and some output generated */
						force_read = true;
					} else {
						log.trace("under: " + r.bytesProduced() + " > " + data_payload_received_buffer.position());
					}
					
					/**
					 * Else return the data we have and deal with the partial data on the next read
					 */
					break;
				case BUFFER_OVERFLOW:
					/**
					 * Not enough space in the destination buffer to store all of the data. We could use a bytes read value of -bufferSizeRequired to signal the new buffer size required but an explicit exception is clearer.
					 */
					if (r.bytesProduced() == 0) {
						log.trace("over: " + data_payload_received_buffer.position() + " > " + data_payload_received_buffer.remaining());
						
						data_payload_received_buffer.flip();
						onGetDatas(true);
						data_payload_received_buffer.clear();
					} else {
						throw new IOException("ReadBufferOverflowException: " + data_payload_received_buffer.position() + " " + data_payload_received_buffer.capacity());
					}
					
					break;
				default:
					throw new IOException("Unexpected Status: " + s + " of SSLEngineResult after an unwrap() operation");
				}
				
				if (r.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
					Runnable runnable = ssl_engine.getDelegatedTask();
					while (runnable != null) {
						runnable.run();
						runnable = ssl_engine.getDelegatedTask();
					}
				}
				
				if (s == Status.BUFFER_OVERFLOW) {
					/**
					 * Restart unwrap for current buffer
					 */
					continue;
				}
			} else {
				force_read = true;
			}
			
			if (force_read) {
				log.trace("Force read");
				socket_received_buffer.compact();
				if (socket_channel.read(socket_received_buffer).get(TIME_TO_GET_SYNC_NETWORK_IO, TimeUnit.MILLISECONDS) == -1) {
					throw new EOFException("Unexpected end of stream");
				}
				socket_received_buffer.flip();
			} else if (socket_received_buffer.hasRemaining() == false) {
				data_payload_received_buffer.flip();
				return;
			}
		}
	}
	
	InetSocketAddress getLocalAddress() {
		try {
			return (InetSocketAddress) socket_channel.getLocalAddress();
		} catch (IOException e) {
			throw new RuntimeException("Can't get address", e);
		}
	}
	
	InetSocketAddress getRemoteAddress() {
		try {
			return (InetSocketAddress) socket_channel.getRemoteAddress();
		} catch (IOException e) {
			throw new RuntimeException("Can't get address", e);
		}
	}
	
	protected CompletableFuture<Integer> asyncWrite(ByteBuffer src) {
		if (!writing.compareAndSet(false, true)) {
			throw new IllegalStateException("Concurrent write operations are not permitted");
		}
		
		return CompletableFuture.supplyAsync(() -> {
			int written = 0;
			try {
				while (src.hasRemaining()) {
					socket_sended_buffer.compact();
					
					SSLEngineResult r = ssl_engine.wrap(src, socket_sended_buffer);
					written += r.bytesConsumed();
					Status s = r.getStatus();
					
					if (s != Status.OK & s != Status.BUFFER_OVERFLOW) {
						/**
						 * Status.BUFFER_UNDERFLOW - only happens on unwrap
						 * Status.CLOSED - unexpected
						 */
						throw new IllegalStateException("Unexpected Status of SSLEngineResult after a wrap() operation");
					}
					
					if (r.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
						Runnable runnable = ssl_engine.getDelegatedTask();
						while (runnable != null) {
							runnable.run();
							runnable = ssl_engine.getDelegatedTask();
						}
					}
					
					socket_sended_buffer.flip();
					
					int toWrite = r.bytesProduced();
					while (toWrite > 0) {
						int size = socket_channel.write(socket_sended_buffer).get(TIME_TO_GET_SYNC_NETWORK_IO, TimeUnit.MILLISECONDS);
						toWrite -= size;
					}
				}
				
				if (writing.compareAndSet(true, false)) {
					return written;
				} else {
					throw new IllegalStateException("Flag that indicates a write is in progress was found to be false (it should have been true) when trying to complete a write operation");
				}
			} catch (Exception e) {
				throw new RuntimeException("Can't push to socket", e);
			}
		}, executor);
	}
	
	/*private void closeNotifySSL() { // XXX retry close_notify
		if (socket_channel.isOpen() == false) {
			return;
		}
		try {
			ssl_engine.closeOutbound();
			socket_sended_buffer.clear();
			SSLEngineResult r = ssl_engine.wrap(HAND_SHAKE_DUMMY, socket_sended_buffer);
			
			if (r.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
				Runnable runnable = ssl_engine.getDelegatedTask();
				while (runnable != null) {
					runnable.run();
					runnable = ssl_engine.getDelegatedTask();
				}
			}
			socket_sended_buffer.flip();
			socket_channel.write(socket_sended_buffer).get(1, TimeUnit.SECONDS);
			ssl_engine.getSession().invalidate();
			Thread.sleep(1000);
			// ssl_engine.closeInbound();
		} catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
			log.warn("Can't close TLS Session", e);
		}
	}*/
	
	public void close() {
		// closeNotifySSL();
		
		if (socket_channel.isOpen()) {
			try {
				socket_channel.shutdownInput();
			} catch (IOException e) {
				onCloseException(e);
			}
			try {
				socket_channel.shutdownOutput();
			} catch (IOException e) {
				onCloseException(e);
			}
			
			try {
				socket_channel.close();
			} catch (ClosedChannelException e) {
				onCloseButChannelWasClosed(e);
			} catch (IOException e) {
				onCloseException(e);
			}
		}
		onAfterClose();
	}
	
	public int hashCode() {
		return socket_channel.hashCode();
	}
	
	public boolean isOpen() {
		if (socket_channel.isOpen() == false) {
			if (ssl_engine.isOutboundDone() == false | ssl_engine.isInboundDone() == false) {
				try {
					ssl_engine.closeOutbound();
					ssl_engine.closeInbound();
				} catch (SSLException e) {
					log.warn("Can't close TLS Session", e);
				}
			}
		}
		
		return socket_channel.isOpen();
	}
	
	/**
	 * @param kt_tool for check security policies
	 */
	public void handshake(KeystoreTool kt_tool) throws IOException {
		ssl_engine.beginHandshake();
		socket_received_buffer.position(socket_received_buffer.limit());
		HandshakeStatus handshake_status = ssl_engine.getHandshakeStatus();
		Status result_status = Status.OK;
		
		boolean handshaking = true;
		
		while (handshaking) {
			
			switch (handshake_status) {
			case NEED_WRAP: {
				socket_sended_buffer.clear();
				SSLEngineResult result = ssl_engine.wrap(HAND_SHAKE_DUMMY, socket_sended_buffer);
				
				/** checkResult */
				handshake_status = result.getHandshakeStatus();
				result_status = result.getStatus();
				if (result_status != Status.OK) {
					throw new SSLException("Invalid status: " + result_status);
				} else if (result.bytesConsumed() != 0) {
					throw new SSLException("Non empty bytesProduced result: " + result.bytesProduced());
				}
				
				socket_sended_buffer.flip();
				try {
					socket_channel.write(socket_sended_buffer).get(TIME_TO_GET_SYNC_NETWORK_IO, TimeUnit.MILLISECONDS);
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					if (e.getCause() instanceof IOException) {
						throw (IOException) e.getCause();
					} else {
						throw new RuntimeException("Execution error during handshake", e);
					}
				}
				break;
			}
			case NEED_UNWRAP: {
				socket_received_buffer.compact();
				if (socket_received_buffer.position() == 0 || result_status == Status.BUFFER_UNDERFLOW) {
					try {
						socket_channel.read(socket_received_buffer).get(TIME_TO_GET_SYNC_NETWORK_IO, TimeUnit.MILLISECONDS);
					} catch (InterruptedException | ExecutionException | TimeoutException e) {
						if (e.getCause() instanceof IOException) {
							throw (IOException) e.getCause();
						} else {
							throw new RuntimeException("Execution error during handshake", e);
						}
					}
				}
				socket_received_buffer.flip();
				SSLEngineResult result = ssl_engine.unwrap(socket_received_buffer, HAND_SHAKE_DUMMY);
				
				/** checkResult */
				handshake_status = result.getHandshakeStatus();
				result_status = result.getStatus();
				
				if (result_status != Status.OK && result_status != Status.BUFFER_UNDERFLOW) {
					throw new SSLException("Invalid status: " + result_status);
				} else if (result.bytesProduced() != 0) {
					throw new SSLException("Non empty bytesProduced result: " + result.bytesProduced());
				}
				
				break;
			}
			case NEED_TASK: {
				Runnable r = null;
				while ((r = ssl_engine.getDelegatedTask()) != null) {
					r.run();
				}
				handshake_status = ssl_engine.getHandshakeStatus();
				break;
			}
			case FINISHED: {
				handshaking = false;
				break;
			}
			default: {
				throw new SSLException("Invalid handshake_status: " + handshake_status);
			}
			}
		}
		
		log.debug("Correct handshake for " + socket_channel.getRemoteAddress());
		kt_tool.checkSecurity(ssl_engine);
		
		readNext();
	}
	
	protected void onAfterClose() {
	}
	
	protected void onCloseException(IOException e) {
		log.warn(e);
	}
	
	protected void onCloseButChannelWasClosed(ClosedChannelException e) {
		log.warn(e);
	}
	
	protected void onIOButClosed(AsynchronousCloseException e) {
		log.error(e);
	}
	
	protected void onIOExceptionCauseClosing(Throwable e) {
		log.error(e);
	}
	
	/**
	 * Executed in socket (read) Thread, get datas from data_payload_received_buffer. It was flipped before.
	 * @return true if restart the next pending read
	 */
	protected abstract boolean onGetDatas(boolean partial);
	
}
