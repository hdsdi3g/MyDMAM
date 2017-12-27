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
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ClosedChannelException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.net.ssl.SSLEngine;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.embddb.store.Item;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.tools.Hexview;
import hd3gtv.tools.ThreadPoolExecutorFactory;

@GsonIgnore
abstract class NodeIO extends TLSSocketHandler {
	
	private static Logger log = Logger.getLogger(NodeIO.class);
	
	private static final int SOCKET_BUFFER_SIZE = 0xFFFF;
	private static final int MAX_SIZE_CHUNK = SOCKET_BUFFER_SIZE - FramePayload.HEADER_SIZE;
	private static final long GRACE_PERIOD_TO_KEEP_OLD_RECEVIED_FRAMES = TimeUnit.HOURS.toMillis(8);
	private static final long MAX_RECEVIED_FRAMES_TO_KEEP_BEFORE_DO_GC = 100l;
	
	@Deprecated
	private final ConcurrentHashMap<Long, FrameContainer> recevied_frames;
	private final Object send_lock;
	
	/**
	 * Don't forget to call handshake
	 */
	NodeIO(AsynchronousSocketChannel socket_channel, SSLEngine ssl_engine, ThreadPoolExecutorFactory executor) {
		super(socket_channel, ssl_engine, executor);
		send_lock = new Object();
		
		// handler_reader = new SocketHandlerReader();
		recevied_frames = new ConcurrentHashMap<>();
	}
	
	/**
	 * Pack source_to_send to Frames, compress it, and send it.
	 */
	int syncSend(ByteBuffer source_to_send, String request_name, boolean close_channel_after_send) throws IOException, GeneralSecurityException {
		CompressionFormat compress_format = CompressionFormat.NONE;
		if (source_to_send.remaining() > 0xFFF) {
			compress_format = CompressionFormat.GZIP;
		}
		
		byte[] b_source_to_send = new byte[source_to_send.remaining()];
		source_to_send.get(b_source_to_send);
		byte[] prepared_source_to_send = compress_format.shrink(b_source_to_send);
		
		long session_id = ThreadLocalRandom.current().nextLong();
		
		/** == ceilDiv */
		int chunk_count = -Math.floorDiv(-prepared_source_to_send.length, MAX_SIZE_CHUNK);
		
		ArrayList<FramePayload> frame_payloads = new ArrayList<>(chunk_count);
		for (int i = 0; i < chunk_count; i++) {
			int pos = i * MAX_SIZE_CHUNK;
			int size = Math.min(prepared_source_to_send.length - pos, MAX_SIZE_CHUNK);
			frame_payloads.add(new FramePayload(session_id, i, prepared_source_to_send, pos, size));
		}
		
		FramePrologue frame_prologue = new FramePrologue(session_id, prepared_source_to_send.length, chunk_count, compress_format);
		FrameEpilogue frame_epilogue = new FrameEpilogue(session_id);
		
		ByteBuffer prologue_to_send = /*cipher_engine.encrypt*/frame_prologue.output();// encrypt
		ByteBuffer epilogue_to_send = /*cipher_engine.encrypt*/frame_epilogue.output();
		
		/**
		 * XXX HOW ABOUT NO ?
		 */
		List<ByteBuffer> frame_payloads_to_send = frame_payloads.stream().map(frame_payload -> {
			return frame_payload.output();
		}).map(buffer -> {
			try {
				return buffer /*cipher_engine.encrypt(buffer)*/;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}).collect(Collectors.toList());
		
		int total_size = prologue_to_send.remaining() + frame_payloads_to_send.stream().mapToInt(payload_to_send -> payload_to_send.remaining()).sum() + epilogue_to_send.remaining();
		
		onBeforeSendRawDatas(request_name, b_source_to_send.length, total_size, compress_format);
		
		ByteBuffer all_frames_to_send = ByteBuffer.allocate(total_size);
		all_frames_to_send.put(prologue_to_send);
		frame_payloads_to_send.forEach(payload_to_send -> {
			all_frames_to_send.put(payload_to_send);
		});
		all_frames_to_send.put(epilogue_to_send);
		
		if (all_frames_to_send.hasRemaining() == true) {
			throw new IOException("Invalid remaining: " + all_frames_to_send.remaining());
		}
		all_frames_to_send.flip();
		
		try {
			synchronized (send_lock) {
				onAfterSend(asyncWrite(all_frames_to_send).get());
				all_frames_to_send.clear();
			}
			
			if (close_channel_after_send) {
				onManualCloseAfterSend();
				close();
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new IOException(e);
		}
		
		return total_size;
	}
	
	private void receviedFramesGC() {
		if (recevied_frames.mappingCount() < MAX_RECEVIED_FRAMES_TO_KEEP_BEFORE_DO_GC) {
			return;
		}
		
		List<Long> delete_list_too_old = recevied_frames.reduceEntries(1, entry -> {
			Long key = entry.getKey();
			FramePrologue p = entry.getValue().prologue;
			
			if (p.create_date + GRACE_PERIOD_TO_KEEP_OLD_RECEVIED_FRAMES < System.currentTimeMillis()) {
				return Arrays.asList(key);
			}
			return new ArrayList<Long>(0);
		}, (l, r) -> {
			int l_size = l.size();
			int r_size = r.size();
			
			if (l_size + r_size == 0) {
				return Collections.emptyList();
			}
			List<Long> result = new ArrayList<Long>();
			result.addAll(l);
			result.addAll(r);
			return result;
		});
		
		delete_list_too_old.forEach(session_id -> {
			recevied_frames.remove(session_id);
			onRemoveOldStoredDataFrame(session_id);
		});
	}
	
	protected boolean onGetDatas(boolean partial) {
		/*if (data_payload_received_buffer.hasRemaining() == false) {
			close();
		} else if (isOpen() == false) {
			return false;
		}*/
		
		onAfterReceviedDatas(data_payload_received_buffer.remaining());
		
		try {
			while (data_payload_received_buffer.hasRemaining()) {
				/**
				 * Extract data frames headers
				 */
				int initial_pos = data_payload_received_buffer.position();
				if (data_payload_received_buffer.remaining() < Protocol.FRAME_HEADER_SIZE) {
					throw new IOException("Invalid header remaining size: " + data_payload_received_buffer.remaining());
				}
				Item.readAndEquals(data_payload_received_buffer, Protocol.APP_EMBDDB_SOCKET_HEADER_TAG, b -> {
					return new IOException("Protocol error with app_socket_header_tag: " + Hexview.tracelog(b));
				});
				Item.readByteAndEquals(data_payload_received_buffer, Protocol.VERSION, version -> {
					return new IOException("Protocol error with version, this = " + Protocol.VERSION + " and dest = " + version);
				});
				int frame_type = data_payload_received_buffer.get();
				data_payload_received_buffer.position(initial_pos);
				
				if (frame_type == Protocol.FRAME_TYPE_PROLOGUE) {
					FramePrologue prologue = new FramePrologue(data_payload_received_buffer);
					recevied_frames.putIfAbsent(prologue.session_id, new FrameContainer(prologue));
					
					receviedFramesGC();
				} else if (frame_type == Protocol.FRAME_TYPE_PAYLOAD) {
					FramePayload frame_payload = new FramePayload(data_payload_received_buffer);
					FrameContainer f_container = recevied_frames.get(frame_payload.session_id);
					if (f_container == null) {
						throw new IOException("Can't found frame with session id: " + frame_payload.session_id);
					}
					f_container.appendPayload(frame_payload);
					
				} else if (frame_type == Protocol.FRAME_TYPE_EPILOGUE) {
					FrameEpilogue epilogue = new FrameEpilogue(data_payload_received_buffer);
					FrameContainer old_f_container = recevied_frames.remove(epilogue.session_id);
					if (old_f_container == null) {
						throw new IOException("Can't found frame with session id: " + epilogue.session_id);
					}
					
					ByteBuffer raw_datas = old_f_container.close();
					if (raw_datas != null) {
						if (onGetDataBlock(new DataBlock(raw_datas), old_f_container.prologue.create_date)) {
							onManualCloseAfterRecevied();
							return false;
						}
					}
				} else {
					throw new IOException("Invalid header, unknown frame_type: " + frame_type);
				}
			}
		} catch (IOException e) {
			onCantExtractFrame(e);
		}
		
		return true;
	}
	
	protected void onAfterClose() {
		recevied_frames.clear();
	}
	
	/**
	 * @return false for close socket
	 */
	protected abstract boolean onGetDataBlock(DataBlock data_block, long create_date);
	
	protected abstract void onIOButClosed(AsynchronousCloseException e);
	
	protected abstract void onIOExceptionCauseClosing(Throwable e);
	
	protected abstract void onCloseException(IOException e);
	
	protected abstract void onCloseButChannelWasClosed(ClosedChannelException e);
	
	protected abstract void onCantExtractFrame(IOException e);
	
	protected abstract void onBeforeSendRawDatas(String request_name, int length, int total_size, CompressionFormat compress_format);
	
	protected abstract void onRemoveOldStoredDataFrame(long session_id);
	
	protected abstract void onManualCloseAfterSend();
	
	protected abstract void onManualCloseAfterRecevied();
	
	protected abstract void onAfterSend(Integer size);
	
	protected abstract void onAfterReceviedDatas(Integer size);
	
}
