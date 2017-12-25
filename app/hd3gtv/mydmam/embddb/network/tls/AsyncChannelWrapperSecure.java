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

package hd3gtv.mydmam.embddb.network.tls;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;

import org.apache.log4j.Logger;

import hd3gtv.tools.ThreadPoolExecutorFactory;

// TODO rename + check same passwords (with hash + random salt)...

/**
 * @see https://github.com/Oreste-Luci/apache-tomcat-8.0.26-src/blob/master/java/org/apache/tomcat/websocket/AsyncChannelWrapperSecure.java
 */
public class AsyncChannelWrapperSecure {
	
	private static Logger log = Logger.getLogger(AsyncChannelWrapperSecure.class);
	public static final int MAX_PAYLOAD_SIZE = 0xFFF; // XXX test more
	
	private static final ByteBuffer DUMMY = ByteBuffer.allocate(8192);
	private final AsynchronousSocketChannel socket_channel;
	private final SSLEngine ssl_engine;
	private final ByteBuffer socket_received_buffer;
	private final ByteBuffer socket_sended_buffer;
	private final ByteBuffer data_payload_received_buffer;
	private final ReceiveDataEvent on_get_datas_event;
	
	private final ThreadPoolExecutorFactory executor;
	private AtomicBoolean writing = new AtomicBoolean(false);
	private AtomicBoolean reading = new AtomicBoolean(false);
	
	public AsyncChannelWrapperSecure(AsynchronousSocketChannel socket_channel, SSLEngine ssl_engine, ReceiveDataEvent on_get_datas_event, ThreadPoolExecutorFactory executor) {
		this.socket_channel = socket_channel;
		this.ssl_engine = ssl_engine;
		this.on_get_datas_event = on_get_datas_event;
		this.executor = executor;
		
		int socketBufferSize = ssl_engine.getSession().getPacketBufferSize();
		socket_received_buffer = ByteBuffer.allocateDirect(socketBufferSize);
		socket_sended_buffer = ByteBuffer.allocateDirect(socketBufferSize);
		data_payload_received_buffer = ByteBuffer.allocateDirect(ssl_engine.getSession().getApplicationBufferSize() + MAX_PAYLOAD_SIZE);
	}
	
	/**
	 * Non blocking, only callback next data block.
	 */
	private void readNext() {
		if (!reading.compareAndSet(false, true)) {
			throw new IllegalStateException("Concurrent read operations are not permitted");
		}
		
		final AsyncChannelWrapperSecure _this = this;
		
		// System.out.println("BEFORE " + socket_received_buffer.position() + " " + socket_received_buffer.limit() + " " + socket_received_buffer.remaining() + " " + socket_received_buffer.capacity());
		
		socket_received_buffer.clear();
		
		// log.info("wait");
		
		socket_channel.read(socket_received_buffer, null, new CompletionHandler<Integer, Void>() {
			public void completed(Integer size, Void _void) {
				try {
					// log.info("get raw: " + size);
					data_payload_received_buffer.clear();
					
					decrypt();
					
					reading.set(false);
					
					int actual_end = data_payload_received_buffer.position();
					data_payload_received_buffer.position(0);
					data_payload_received_buffer.limit(actual_end);
					
					boolean restart_read = on_get_datas_event.onGetDatas(_this, data_payload_received_buffer);
					if (restart_read) {
						readNext();
					}
					
				} catch (Exception e) {
					log.error("Can't read from socket", e);
					reading.set(false);
				}
			}
			
			public void failed(Throwable exc, Void kit) {
				log.error("Can't read", exc);
				reading.set(false);
			}
		});
	}
	
	private void decrypt() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		
		// int read = 0;
		// boolean force_read = false;
		
		// while (read == 0 | force_read) {
		
		// System.out.println("AFTER " + socket_received_buffer.position() + " " + socket_received_buffer.limit() + " " + socket_received_buffer.remaining() + " " + socket_received_buffer.capacity());
		socket_received_buffer.flip();
		while (true) {
			
			boolean force_read = false;
			
			if (socket_received_buffer.hasRemaining()) {
				// Decrypt the data in the buffer
				SSLEngineResult r = ssl_engine.unwrap(socket_received_buffer, data_payload_received_buffer);
				Status s = r.getStatus();
				
				switch (s) {
				case OK:
					// Bytes available for reading and there may be
					// sufficient data in the socketReadBuffer to
					// support further reads without reading from the
					// socket
					// log.info("ok: " + r.bytesProduced());
					
					break;
				case BUFFER_UNDERFLOW:
					// log.info("under: " + r.bytesProduced());
					// There is partial data in the socketReadBuffer
					if (r.bytesProduced() == 0) {
						// Need more data before the partial data can be
						// processed and some output generated
						force_read = true;
					}
					
					// else return the data we have and deal with the
					// partial data on the next read
					break;
				case BUFFER_OVERFLOW:
					/**
					 * Not enough space in the destination buffer to store all of the data. We could use a bytes read value of -bufferSizeRequired to signal the new buffer size required but an explicit exception is clearer.
					 */
					throw new IOException("ReadBufferOverflowException: " + data_payload_received_buffer.capacity());
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
			} else {
				force_read = true;
			}
			
			if (force_read) {
				log.info("Force read");
				socket_received_buffer.compact();
				if (socket_channel.read(socket_received_buffer).get(1, TimeUnit.MINUTES) == -1) {
					throw new EOFException("Unexpected end of stream");
				}
				socket_received_buffer.flip();
			} else if (socket_received_buffer.hasRemaining() == false) {
				return;
			}
		}
	}
	
	public AsynchronousSocketChannel getChannel() {
		return socket_channel;
	}
	
	/**
	 * @param src remaining <= MAX_PAYLOAD_SIZE
	 */
	public CompletableFuture<Integer> asyncWrite(ByteBuffer src) {
		if (!writing.compareAndSet(false, true)) {
			throw new IllegalStateException("Concurrent write operations are not permitted");
		}
		if (src.remaining() > MAX_PAYLOAD_SIZE) {
			throw new IndexOutOfBoundsException("src remaining > " + MAX_PAYLOAD_SIZE);
		}
		
		return CompletableFuture.supplyAsync(() -> {
			int written = 0;
			try {
				while (src.hasRemaining()) {
					socket_sended_buffer.compact();
					
					// Encrypt the data
					SSLEngineResult r = ssl_engine.wrap(src, socket_sended_buffer);
					written += r.bytesConsumed();
					Status s = r.getStatus();
					
					if (s != Status.OK & s != Status.BUFFER_OVERFLOW) {
						// Status.BUFFER_UNDERFLOW - only happens on unwrap
						// Status.CLOSED - unexpected
						throw new IllegalStateException("Unexpected Status of SSLEngineResult after a wrap() operation");
					}
					
					// Check for tasks
					if (r.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
						Runnable runnable = ssl_engine.getDelegatedTask();
						while (runnable != null) {
							runnable.run();
							runnable = ssl_engine.getDelegatedTask();
						}
					}
					
					socket_sended_buffer.flip();
					
					// Do the write
					int toWrite = r.bytesProduced();
					while (toWrite > 0) {
						int size = socket_channel.write(socket_sended_buffer).get(1, TimeUnit.MINUTES);
						toWrite -= size;
						// log.info("Writed raw " + size);
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
	
	public void close() throws IOException {
		socket_channel.close();
	}
	
	public CompletableFuture<Void> asyncHandshake() {
		CompletableFuture<Void> cf = CompletableFuture.runAsync(() -> {
			try {
				ssl_engine.beginHandshake();
				// So the first compact does the right thing
				socket_received_buffer.position(socket_received_buffer.limit());
				
				HandshakeStatus handshake_status = ssl_engine.getHandshakeStatus();
				Status result_status = Status.OK;
				
				boolean handshaking = true;
				
				while (handshaking) {
					switch (handshake_status) {
					case NEED_WRAP: {
						socket_sended_buffer.clear();
						SSLEngineResult result = ssl_engine.wrap(DUMMY, socket_sended_buffer);
						
						/** checkResult */
						handshake_status = result.getHandshakeStatus();
						result_status = result.getStatus();
						if (result_status != Status.OK) {
							throw new SSLException("TODO");
						} else if (result.bytesConsumed() != 0) {
							throw new SSLException("TODO");
						}
						
						socket_sended_buffer.flip();
						Future<Integer> fWrite = socket_channel.write(socket_sended_buffer);
						fWrite.get();
						break;
					}
					case NEED_UNWRAP: {
						socket_received_buffer.compact();
						if (socket_received_buffer.position() == 0 || result_status == Status.BUFFER_UNDERFLOW) {
							Future<Integer> fRead = socket_channel.read(socket_received_buffer);
							fRead.get();
						}
						socket_received_buffer.flip();
						SSLEngineResult result = ssl_engine.unwrap(socket_received_buffer, DUMMY);
						
						/** checkResult */
						handshake_status = result.getHandshakeStatus();
						result_status = result.getStatus();
						
						if (result_status != Status.OK && result_status != Status.BUFFER_UNDERFLOW) {
							throw new SSLException("TODO");
						} else if (result.bytesProduced() != 0) {
							throw new SSLException("TODO");
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
						throw new SSLException("TODO");
					}
					}
				}
			} catch (Exception e) {
				throw new RuntimeException("Can't do handshake", e);
			}
			
			readNext();
		}, executor);
		return cf;
	}
	
}
