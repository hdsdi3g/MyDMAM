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
 * Copyright (C) hdsdi3g for hd3g.tv 21 nov. 2016
 * 
*/
package hd3gtv.mydmam.embddb.network;

import java.io.UnsupportedEncodingException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;

import hd3gtv.tools.Hexview;

public final class Protocol {
	private static final Logger log = Logger.getLogger(Protocol.class);
	
	public static final int VERSION = 1;
	public static final Charset UTF8 = Charset.forName("UTF-8");
	public static final int BUFFER_SIZE = 0xFFFF;
	
	/**
	 * EMBDBMYD
	 */
	public static final byte[] APP_SOCKET_HEADER_TAG = "EMBDBMYD".getBytes(UTF8);
	
	private IvParameterSpec salt;
	private SecretKey skeySpec;
	
	private SocketHandlerReader handler_reader;
	private SocketHandlerWriter handler_writer;
	private SocketHandlerWriterCloser handler_writer_closer;
	
	public Protocol(String master_password_key) throws NoSuchAlgorithmException, NoSuchProviderException, UnsupportedEncodingException {
		if (master_password_key == null) {
			throw new NullPointerException("\"master_password_key\" can't to be null");
		}
		if (master_password_key.isEmpty()) {
			throw new NullPointerException("\"master_password_key\" can't to be empty");
		}
		handler_reader = new SocketHandlerReader();
		handler_writer = new SocketHandlerWriter();
		handler_writer_closer = new SocketHandlerWriterCloser();
		
		MessageDigest md = MessageDigest.getInstance("SHA-256", "BC");
		byte[] key = md.digest(master_password_key.getBytes("UTF-8"));
		
		skeySpec = new SecretKeySpec(key, "AES");
		salt = new IvParameterSpec(key, 0, 16);
	}
	
	public int getDefaultTCPPort() {
		return 9160;
	}
	
	public int getDefaultUDPBroadcastPort() {
		return 9160;
	}
	
	public SocketHandlerReader getHandlerReader() {
		return handler_reader;
	}
	
	public SocketHandlerWriter getHandlerWriter(boolean close_channel_after_send) {
		if (close_channel_after_send) {
			return handler_writer_closer;
		} else {
			return handler_writer;
		}
	}
	
	public byte[] encrypt(byte[] cleared_datas, int pos, int len) throws GeneralSecurityException {
		return encryptDecrypt(cleared_datas, pos, len, Cipher.ENCRYPT_MODE);
	}
	
	public byte[] decrypt(byte[] crypted_datas, int pos, int len) throws GeneralSecurityException {
		return encryptDecrypt(crypted_datas, pos, len, Cipher.DECRYPT_MODE);
	}
	
	private byte[] encryptDecrypt(byte[] datas, int pos, int len, int mode) throws GeneralSecurityException {
		if (log.isTraceEnabled()) {
			if (mode == Cipher.ENCRYPT_MODE) {
				log.trace("Raw data input (no crypted)" + Hexview.LINESEPARATOR + Hexview.tracelog(datas, pos, len));
			} else {
				log.trace("Raw data input (crypted)" + Hexview.LINESEPARATOR + Hexview.tracelog(datas, pos, len));
			}
		}
		
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
		cipher.init(mode, skeySpec, salt);
		byte[] result = cipher.doFinal(datas, pos, len);
		
		if (log.isTraceEnabled()) {
			if (mode == Cipher.ENCRYPT_MODE) {
				log.trace("Raw data input (crypted)" + Hexview.LINESEPARATOR + Hexview.tracelog(result, 0, result.length));
			} else {
				log.trace("Raw data input (decrypted)" + Hexview.LINESEPARATOR + Hexview.tracelog(result, 0, result.length));
			}
		}
		return result;
	}
	
	class SocketHandlerReader implements CompletionHandler<Integer, Node> {
		
		public void completed(Integer size, Node node) {
			if (size == -1) {
				return;
			} else if (size < 1) {
				log.debug("Get empty datas from " + node.toString() + ", size = " + size);
				return;
			}
			
			if (log.isTraceEnabled()) {
				log.trace("Recevied from " + node + " " + size + " bytes");
			}
			
			if (node.isOpenSocket()) {
				try {
					node.doProcessReceviedDatas();
				} catch (Exception e) {
					failed(e, node);
				}
			}
			
			if (node.isOpenSocket()) {
				node.asyncRead();
			}
		}
		
		public void failed(Throwable e, Node node) {
			if (e instanceof AsynchronousCloseException) {
				log.debug("Channel " + node + " was closed, so can't close it.");
			} else {
				log.error("Channel " + node + " failed, close socket because " + e.getMessage());
				node.close(getClass());
			}
		}
		
	}
	
	class SocketHandlerWriter implements CompletionHandler<Integer, Node> {
		
		protected void showLogs(Integer size, Node node) {
			if (log.isTraceEnabled()) {
				log.trace("Sended to " + node + " " + size + " bytes");
			}
		}
		
		public void completed(Integer size, Node node) {
			if (size == -1) {
				return;
			}
			showLogs(size, node);
		}
		
		public void failed(Throwable e, Node node) {
			if (e instanceof AsynchronousCloseException) {
				log.debug("Channel " + node + " was closed, so can't close it.");
			} else {
				log.error("Channel " + node + " failed, close socket because " + e.getMessage());
				node.close(getClass());
			}
		}
		
	}
	
	class SocketHandlerWriterCloser extends SocketHandlerWriter {
		
		public void completed(Integer size, Node node) {
			showLogs(size, node);
			log.info("Manual close socket after send datas to other node " + node.toString());
			node.close(getClass());
		}
		
	}
}
