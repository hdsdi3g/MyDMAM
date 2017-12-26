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
import java.net.InetSocketAddress;
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
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.net.ssl.SSLEngine;

import hd3gtv.mydmam.embddb.store.Item;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.tools.Hexview;
import hd3gtv.tools.ThreadPoolExecutorFactory;

@GsonIgnore
class NodeIO extends TLSSocketHandler {
	
	private static final int SOCKET_BUFFER_SIZE = 0xFFFF;
	private static final int MAX_SIZE_CHUNK = SOCKET_BUFFER_SIZE - FramePayload.HEADER_SIZE;
	private static final long GRACE_PERIOD_TO_KEEP_OLD_RECEVIED_FRAMES = TimeUnit.HOURS.toMillis(8);
	private static final long MAX_RECEVIED_FRAMES_TO_KEEP_BEFORE_DO_GC = 100l;
	
	@Deprecated
	private final ConcurrentHashMap<Long, FrameContainer> recevied_frames;
	// private final SocketHandlerReader handler_reader;
	private final BiFunction<DataBlock, Long, Boolean> onGetDataBlock;
	private final NodeIOEvent event_handler;
	
	private final Object send_lock;
	
	/**
	 * @param onGetDataBlock (recevied block, create date), return boolean: should close channel after this
	 */
	NodeIO(AsynchronousSocketChannel socket_channel, SSLEngine ssl_engine, BiFunction<DataBlock, Long, Boolean> onGetDataBlock, NodeIOEvent event_handler, ThreadPoolExecutorFactory executor) {
		super(socket_channel, ssl_engine, executor);
		
		this.onGetDataBlock = onGetDataBlock;
		if (onGetDataBlock == null) {
			throw new NullPointerException("\"onGetDataBlock\" can't to be null");
		}
		this.event_handler = event_handler;
		if (event_handler == null) {
			throw new NullPointerException("\"event_handler\" can't to be null");
		}
		send_lock = new Object();
		
		// handler_reader = new SocketHandlerReader();
		recevied_frames = new ConcurrentHashMap<>();
	}
	
	InetSocketAddress getRemoteAddress() {
		return (InetSocketAddress) getChannelRemoteAddress();
	}
	
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
		
		event_handler.onBeforeSendRawDatas(request_name, b_source_to_send.length, total_size, compress_format);
		
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
				event_handler.onAfterSend(asyncWrite(all_frames_to_send).get());
				all_frames_to_send.clear();
			}
			
			if (close_channel_after_send) {
				event_handler.onManualCloseAfterSend();
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
			event_handler.onRemoveOldStoredDataFrame(session_id);
		});
	}
	
	private void readNextFrame(ByteBuffer sended_content) throws IOException, GeneralSecurityException {
		int initial_pos = sended_content.position();
		if (sended_content.remaining() < Protocol.FRAME_HEADER_SIZE) {
			throw new IOException("Invalid header remaining size: " + sended_content.remaining());
		}
		Item.readAndEquals(sended_content, Protocol.APP_EMBDDB_SOCKET_HEADER_TAG, b -> {
			return new IOException("Protocol error with app_socket_header_tag: " + Hexview.tracelog(b));
		});
		Item.readByteAndEquals(sended_content, Protocol.VERSION, version -> {
			return new IOException("Protocol error with version, this = " + Protocol.VERSION + " and dest = " + version);
		});
		int frame_type = sended_content.get();
		sended_content.position(initial_pos);
		
		if (frame_type == Protocol.FRAME_TYPE_PROLOGUE) {
			FramePrologue prologue = new FramePrologue(sended_content);
			recevied_frames.putIfAbsent(prologue.session_id, new FrameContainer(prologue));
			
			receviedFramesGC();
		} else if (frame_type == Protocol.FRAME_TYPE_PAYLOAD) {
			FramePayload frame_payload = new FramePayload(sended_content);
			FrameContainer f_container = recevied_frames.get(frame_payload.session_id);
			if (f_container == null) {
				throw new IOException("Can't found frame with session id: " + frame_payload.session_id);
			}
			f_container.appendPayload(frame_payload);
			
		} else if (frame_type == Protocol.FRAME_TYPE_EPILOGUE) {
			FrameEpilogue epilogue = new FrameEpilogue(sended_content);
			FrameContainer old_f_container = recevied_frames.remove(epilogue.session_id);
			if (old_f_container == null) {
				throw new IOException("Can't found frame with session id: " + epilogue.session_id);
			}
			
			ByteBuffer raw_datas = old_f_container.close();
			if (raw_datas != null) {
				if (onGetDataBlock.apply(new DataBlock(raw_datas), old_f_container.prologue.create_date)) {
					event_handler.onManualCloseAfterRecevied();
					close();
				}
			}
		} else {
			throw new IOException("Invalid header, unknown frame_type: " + frame_type);
		}
		
	}
	
	// XXX re-implement reader logic
	/*private class SocketHandlerReader implements CompletionHandler<Integer, ByteBuffer> {
		
		public void completed(Integer size, ByteBuffer read_buffer) {
			read_buffer.flip();
			
			if (size < 1) {
				close();
			} else if (read_buffer.remaining() != size) {
				failed(new IOException("Invalid remaining datas: " + size + " buffer: " + read_buffer.remaining()), read_buffer);
				if (isOpen()) {
					asyncRead();
				}
			} else if (isOpen()) {
				event_handler.onAfterReceviedDatas(size);
				
				try {
					ByteBuffer sended_content = read_buffer;// cipher_engine.decrypt();
					while (sended_content.hasRemaining()) {
						readNextFrame(sended_content);
					}
				} catch (Exception e) {
					failed(e, read_buffer);
				}
				
				read_buffer.clear();
				if (isOpen()) {
					asyncRead();
				}
			}
		}
	}*/
	
	public void close() {
		super.close();
		recevied_frames.clear();
	}
	
	@Override
	protected boolean onGetDatas(boolean partial) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	protected void onIOButClosed(AsynchronousCloseException e) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void onIOExceptionCauseClosing(Throwable e) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void onCloseException(IOException e) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void onCloseButChannelWasClosed(ClosedChannelException e) {
		// TODO Auto-generated method stub
		
	}
	
}
