/*
 * This file is part of MyDMAM.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 28 nov. 2017
 * 
*/
package hd3gtv.mydmam.embddb.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.security.GeneralSecurityException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import javax.net.ssl.SSLEngine;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.embddb.store.Item;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.tools.Hexview;
import hd3gtv.tools.ThreadPoolExecutorFactory;

@GsonIgnore
abstract class NodeIO extends TLSSocketHandler {
	
	// private static Logger log = Logger.getLogger(NodeIO.class);
	private final Object send_lock;
	
	/**
	 * EmbMYD
	 */
	public static final byte[] APP_EMBDDB_SOCKET_HEADER_TAG = "EmbMYD".getBytes(MyDMAM.US_ASCII);
	/**
	 * EndPayload
	 */
	public static final byte[] APP_EMBDDB_SOCKET_FOOTER_TAG = "EndPayload".getBytes(MyDMAM.US_ASCII);
	
	private static final int FRAME_HEADER_SIZE = APP_EMBDDB_SOCKET_HEADER_TAG.length /** Generic Headers */
			+ 1 /** VERSION */
			+ 8 /** Session id */
			+ 1 /** compress_format */
			+ HandleName.SIZE /** Handle name ref */
			+ 8 /** Create date */
			+ 4 /** payload_size */
	;
	private static final int FRAME_FOOTER_SIZE = APP_EMBDDB_SOCKET_FOOTER_TAG.length /** Generic Footer */
			+ 8 /** Session id */
	;
	
	private volatile ByteBuffer current_recevied_payload;
	private volatile long current_session_id;
	private volatile CompressionFormat current_compress_format;
	private volatile HandleName current_handle_name;
	private volatile long current_create_date;
	private volatile ByteBuffer previous_data_payload_received_buffer;
	
	/**
	 * Don't forget to call handshake
	 */
	NodeIO(AsynchronousSocketChannel socket_channel, SSLEngine ssl_engine, ThreadPoolExecutorFactory executor) {
		super(socket_channel, ssl_engine, executor);
		send_lock = new Object();
	}
	
	private void resetCurrentReceviedDatas() {
		current_recevied_payload = null;
		current_session_id = 0;
		current_compress_format = null;
		current_handle_name = null;
		current_create_date = 0;
	}
	
	int syncSend(ByteBuffer payload, HandleName request_handle_name, long create_date, boolean close_channel_after_send) throws IOException, GeneralSecurityException {
		CompressionFormat compress_format = CompressionFormat.NONE;
		if (payload.remaining() > 0xFFF) {
			compress_format = CompressionFormat.GZIP;
		}
		
		long session_id = ThreadLocalRandom.current().nextLong();
		
		byte[] b_source_to_send = new byte[payload.remaining()];
		payload.get(b_source_to_send);
		byte[] prepared_source_to_send = compress_format.shrink(b_source_to_send);
		
		int payload_size = prepared_source_to_send.length;
		ByteBuffer result = ByteBuffer.allocateDirect(FRAME_HEADER_SIZE + payload_size + FRAME_FOOTER_SIZE);
		
		result.put(APP_EMBDDB_SOCKET_HEADER_TAG);/** Generic Headers */
		result.put(Protocol.VERSION);/** Generic Headers */
		result.putLong(session_id);
		result.put(compress_format.getReference());
		request_handle_name.toByteBuffer(result);
		result.putLong(create_date);
		result.putInt(payload_size);
		
		result.put(prepared_source_to_send); /** Payload */
		
		result.put(APP_EMBDDB_SOCKET_FOOTER_TAG);/** Generic Footer */
		result.putLong(session_id);
		
		if (result.hasRemaining()) {
			throw new IOException("Invalid payload size: remaining = " + result.remaining());
		}
		
		result.flip();
		onBeforeSendRawDatas(request_handle_name.name, b_source_to_send.length, result.remaining(), compress_format);
		
		synchronized (send_lock) {
			try {
				onAfterSend(asyncWrite(result).get());
			} catch (InterruptedException | ExecutionException e) {
				throw new IOException(e);
			}
		}
		
		if (close_channel_after_send) {
			onManualCloseAfterSend();
			close();
		}
		
		return result.capacity();
	}
	
	protected boolean onGetDatas(boolean partial) {
		onAfterReceviedDatas(data_payload_received_buffer.remaining());
		
		Supplier<ByteBuffer> current_data_buffer = () -> {
			if (previous_data_payload_received_buffer != null) {
				if (previous_data_payload_received_buffer.hasRemaining()) {
					return previous_data_payload_received_buffer;
				} else {
					previous_data_payload_received_buffer = null;
				}
			}
			return data_payload_received_buffer;
		};
		
		try {
			while (current_data_buffer.get().hasRemaining()) {
				if (current_recevied_payload == null) {
					/**
					 * Frame header is not yet sended
					 */
					if (current_data_buffer.get().remaining() < FRAME_HEADER_SIZE) {
						/**
						 * Wait the next data bulk
						 */
						break;
					}
					
					Item.readAndEquals(current_data_buffer.get(), APP_EMBDDB_SOCKET_HEADER_TAG, b -> {
						return new IOException("Protocol error with app_socket_header_tag: " + Hexview.tracelog(b));
					});
					Item.readByteAndEquals(current_data_buffer.get(), Protocol.VERSION, version -> {
						return new IOException("Protocol error with version, this = " + Protocol.VERSION + " and dest = " + version);
					});
					
					current_session_id = current_data_buffer.get().getLong();
					
					current_compress_format = CompressionFormat.fromReference(current_data_buffer.get().get());
					if (current_compress_format == null) {
						throw new IOException("\"current_compress_format\" can't to be null");
					}
					
					current_handle_name = new HandleName(current_data_buffer.get());
					current_create_date = current_data_buffer.get().getLong();
					
					int playload_size = current_data_buffer.get().getInt();
					if (playload_size < 0) {
						throw new IOException("\"playload_size\" is invalid: " + playload_size);
					}
					current_recevied_payload = ByteBuffer.allocate(playload_size);
					
				} else if (current_recevied_payload.hasRemaining()) {
					/**
					 * Get next data bulk and transfert to current buffer.
					 */
					int max_to_transfert = Math.min(current_recevied_payload.remaining(), current_data_buffer.get().remaining());
					
					for (int pos = 0; pos < max_to_transfert; pos++) {
						current_recevied_payload.put(current_data_buffer.get().get());
					}
				} else {
					/**
					 * The whole data has been recevied.
					 */
					if (current_data_buffer.get().remaining() < FRAME_FOOTER_SIZE) {
						/**
						 * Wait the next data bulk
						 */
						break;
					}
					/**
					 * Now, read the footer.
					 */
					Item.readAndEquals(current_data_buffer.get(), APP_EMBDDB_SOCKET_FOOTER_TAG, b -> {
						return new IOException("Protocol error with app_socket_footer_tag: " + Hexview.tracelog(b));
					});
					
					long actual_session_id = current_data_buffer.get().getLong();
					if (current_session_id != actual_session_id) {
						throw new IOException("Invalid session_id: " + actual_session_id + " instead of " + current_session_id);
					}
					
					/**
					 * Expand payload
					 */
					current_recevied_payload.position(0);
					current_recevied_payload.limit(current_recevied_payload.capacity());
					byte[] compressed_content = new byte[current_recevied_payload.remaining()];
					current_recevied_payload.get(compressed_content);
					byte[] uncompressed_content = current_compress_format.expand(compressed_content);
					
					/**
					 * Callback payload
					 */
					if (onGetPayload(ByteBuffer.wrap(uncompressed_content), current_handle_name, current_create_date) == false) {
						onManualCloseAfterRecevied();
						return false;
					}
					
					resetCurrentReceviedDatas();
				}
			}
			
			if (data_payload_received_buffer.hasRemaining()) {
				/**
				 * Not all recevied data was consumed. The leftovers will be kept.
				 */
				if (previous_data_payload_received_buffer != null) {
					byte[] actual_previous = new byte[previous_data_payload_received_buffer.capacity()];
					previous_data_payload_received_buffer.position(0);
					previous_data_payload_received_buffer.limit(actual_previous.length);
					previous_data_payload_received_buffer.get(actual_previous);
					previous_data_payload_received_buffer.clear();
					
					previous_data_payload_received_buffer = ByteBuffer.allocate(actual_previous.length + data_payload_received_buffer.remaining());
					previous_data_payload_received_buffer.put(actual_previous);
					previous_data_payload_received_buffer.put(data_payload_received_buffer);
				} else {
					previous_data_payload_received_buffer = ByteBuffer.allocate(data_payload_received_buffer.remaining());
					previous_data_payload_received_buffer.put(data_payload_received_buffer);
				}
			} else {
				previous_data_payload_received_buffer = null;
			}
		} catch (IOException e) {
			resetCurrentReceviedDatas();
			onCantExtractFrame(e);
			return false;
		}
		
		return true;
	}
	
	/**
	 * @return false for close socket
	 */
	protected abstract boolean onGetPayload(ByteBuffer payload, HandleName handle_name, long create_date);
	
	protected abstract void onCantExtractFrame(IOException e);
	
	protected abstract void onBeforeSendRawDatas(String request_name, int length, int total_size, CompressionFormat compress_format);
	
	protected abstract void onRemoveOldStoredDataFrame(long session_id);
	
	protected abstract void onManualCloseAfterSend();
	
	protected abstract void onManualCloseAfterRecevied();
	
	protected abstract void onAfterSend(Integer size);
	
	protected abstract void onAfterReceviedDatas(Integer size);
	
}