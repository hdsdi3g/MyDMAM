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
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;

import org.apache.log4j.Logger;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.BlockCipherPadding;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.tools.Hexview;

@GsonIgnore
public final class Protocol {
	private static final Logger log = Logger.getLogger(Protocol.class);
	
	public static final int VERSION = 2;
	public static final int BUFFER_SIZE = 0xFFFF;// TODO2 remove I/O limit
	public static final long MAX_DELTA_AGE_BLOCK = TimeUnit.SECONDS.toMillis(10);
	
	/**
	 * EMBDDBMYD
	 */
	public static final byte[] APP_EMBDDB_SOCKET_HEADER_TAG = "EMBDDBMYD".getBytes(MyDMAM.UTF8);
	
	/**
	 * MYDNETDSCVR
	 */
	public static final byte[] APP_NETDISCOVER_SOCKET_HEADER_TAG = "MYDNETDSCVR".getBytes(MyDMAM.UTF8);
	
	private final KeyParameter keyParam;
	private final CipherParameters params;
	private final String hashed_password_key;
	
	public Protocol(String master_password_key) throws NoSuchAlgorithmException, NoSuchProviderException, UnsupportedEncodingException {
		if (master_password_key == null) {
			throw new NullPointerException("\"master_password_key\" can't to be null");
		}
		if (master_password_key.isEmpty()) {
			throw new NullPointerException("\"master_password_key\" can't to be empty");
		}
		
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] key = md.digest(master_password_key.getBytes("UTF-8"));
		keyParam = new KeyParameter(key);
		params = new ParametersWithIV(keyParam, key, 0, 16);
		
		hashed_password_key = MyDMAM.byteToString(key);
	}
	
	public int getDefaultTCPPort() {
		return 9160;
	}
	
	public int getDefaultUDPMulticastPort() {
		return 9160;
	}
	
	/**
	 * 239.0.0.1
	 * https://www.iana.org/assignments/multicast-addresses/multicast-addresses.xhtml
	 * IPv4 Multicast Address, Organization-Local Scope
	 */
	public Inet4Address getDefaulMulticastIPv4Addr() {
		try {
			return (Inet4Address) InetAddress.getByName("239.0.0.1");
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * FF02::110
	 * http://www.iana.org/assignments/ipv6-multicast-addresses/ipv6-multicast-addresses.xhtml
	 * IPv6 Multicast Address, Link-Local Scope Multicast Addresses, variable scope allocation
	 */
	public Inet6Address getDefaulMulticastIPv6Addr() {
		try {
			return (Inet6Address) InetAddress.getByName("FF02::110");
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @return SHA-256(master_password_key)
	 */
	public String getHashedPasswordKey() {
		return hashed_password_key;
	}
	
	public byte[] encrypt(byte[] cleared_datas, int pos, int len) throws GeneralSecurityException {
		try {
			return encryptDecrypt(cleared_datas, pos, len, Cipher.ENCRYPT_MODE);
		} catch (DataLengthException | IllegalStateException | InvalidCipherTextException e) {
			throw new GeneralSecurityException(e);
		}
	}
	
	public byte[] decrypt(byte[] crypted_datas, int pos, int len) throws GeneralSecurityException {
		try {
			return encryptDecrypt(crypted_datas, pos, len, Cipher.DECRYPT_MODE);
		} catch (DataLengthException | IllegalStateException | InvalidCipherTextException e) {
			throw new GeneralSecurityException(e);
		}
	}
	
	private byte[] encryptDecrypt(byte[] datas, int pos, int len, int mode) throws GeneralSecurityException, DataLengthException, IllegalStateException, InvalidCipherTextException {
		if (log.isTraceEnabled()) {
			if (mode == Cipher.ENCRYPT_MODE) {
				log.trace("Raw data input (no crypted)" + Hexview.LINESEPARATOR + Hexview.tracelog(datas, pos, len));
			} else {
				log.trace("Raw data input (crypted)" + Hexview.LINESEPARATOR + Hexview.tracelog(datas, pos, len));
			}
		}
		
		BlockCipherPadding padding = new PKCS7Padding();
		BufferedBlockCipher cipher2 = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), padding);
		cipher2.reset();
		cipher2.init(mode == Cipher.ENCRYPT_MODE, params);
		
		byte[] buf = new byte[cipher2.getOutputSize(len)];
		int len2 = cipher2.processBytes(datas, pos, len, buf, 0);
		len2 += cipher2.doFinal(buf, len2);
		
		byte[] result = new byte[len2];
		System.arraycopy(buf, 0, result, 0, len2);
		
		if (log.isTraceEnabled()) {
			if (mode == Cipher.ENCRYPT_MODE) {
				log.trace("Raw data input (crypted)" + Hexview.LINESEPARATOR + Hexview.tracelog(result, 0, result.length));
			} else {
				log.trace("Raw data input (decrypted)" + Hexview.LINESEPARATOR + Hexview.tracelog(result, 0, result.length));
			}
		}
		return result;
	}
	
}
