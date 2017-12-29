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

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;

import org.apache.log4j.Logger;
import org.bouncycastle.operator.OperatorCreationException;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.tools.Hexview;

@GsonIgnore
public final class Protocol {
	private static final Logger log = Logger.getLogger(Protocol.class);
	
	public static final byte VERSION = 2;
	
	private final String hashed_password_key;
	private final String cipher_algorithm;
	private final String key_algorithm;
	private final String password_hash_algorithm;
	private final SecretKey cipher_key;
	private final IvParameterSpec iv;
	
	/*
	 * TLS tools 
	 */
	private KeystoreTool kt_tool;
	private SSLContext ssl_context;
	
	/**
	 * @param master_password_key no security checks will be done here.
	 */
	public Protocol() throws GeneralSecurityException, IOException, SecurityException, OperatorCreationException {
		this(getMasterPasswordKey());
		
		String keystore_file_name = Configuration.global.getValue("embddb", "keystore", MyDMAM.APP_ROOT_PLAY_CONF_DIRECTORY.getPath() + File.separator + "keystore.jks");
		String keystore_password = Configuration.global.getValue("embddb", "keystore_password", getMasterPasswordKey());
		String x590_principal_hostname = Configuration.global.getValue("embddb", "x590_principal_hostname", Configuration.global.getValue("service", "workername", InetAddress.getLocalHost().getHostName()));
		
		kt_tool = new KeystoreTool(new File(keystore_file_name), keystore_password, x590_principal_hostname);
		ssl_context = kt_tool.createTLSContext();
	}
	
	private static String getMasterPasswordKey() throws GeneralSecurityException {
		String master_password_key = Configuration.global.getValue("embddb", "master_password_key", "");
		if (master_password_key.equalsIgnoreCase("SetMePlease")) {
			throw new GeneralSecurityException("You can't use \"SetMePlease\" as password for EmbDDB");
		}
		if (master_password_key.length() < 5) {
			log.warn("You should not use a so small password for EmbDDB (" + master_password_key.length() + " chars)");
		}
		return master_password_key;
	}
	
	/**
	 * USED ONLY FOR INTERNAL TESTS !
	 * It don't load kt_tool and ssl_context.
	 */
	Protocol(String master_password) throws GeneralSecurityException, IOException, SecurityException {
		cipher_algorithm = Configuration.global.getValue("embddb", "cipher_algorithm", "AES/CBC/PKCS5Padding");
		key_algorithm = Configuration.global.getValue("embddb", "key_algorithm", "AES");
		password_hash_algorithm = Configuration.global.getValue("embddb", "password_hash_algorithm", "SHA-256");
		int key_size = Configuration.global.getValue("embddb", "key_size", 16);
		
		MessageDigest md = MessageDigest.getInstance(password_hash_algorithm);
		byte[] key = md.digest(master_password.getBytes("UTF-8"));
		iv = new IvParameterSpec(key, 0, key_size);
		hashed_password_key = MyDMAM.byteToString(key);
		cipher_key = new SecretKeySpec(key, 0, key_size, key_algorithm);
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
	
	public String getHashedPasswordKey() {
		return hashed_password_key;
	}
	
	public byte[] encrypt(byte[] cleared_datas) throws GeneralSecurityException {
		return encryptDecrypt(cleared_datas, 0, cleared_datas.length, Cipher.ENCRYPT_MODE);
	}
	
	public byte[] decrypt(byte[] crypted_datas) throws GeneralSecurityException {
		return encryptDecrypt(crypted_datas, 0, crypted_datas.length, Cipher.DECRYPT_MODE);
	}
	
	private byte[] encryptDecrypt(byte[] datas, int pos, int len, int mode) throws GeneralSecurityException {
		if (log.isTraceEnabled()) {
			if (mode == Cipher.ENCRYPT_MODE) {
				log.trace("Raw data input (no crypted)" + Hexview.LINESEPARATOR + Hexview.tracelog(datas, pos, len));
			} else {
				log.trace("Raw data input (crypted)" + Hexview.LINESEPARATOR + Hexview.tracelog(datas, pos, len));
			}
		}
		
		Cipher c = Cipher.getInstance(cipher_algorithm);
		c.init(mode, cipher_key, iv);
		byte[] result = c.doFinal(datas, pos, len);
		
		/*
		BlockCipherPadding padding = new PKCS7Padding();
		BufferedBlockCipher cipher2 = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), padding);
		cipher2.reset();
		cipher2.init(mode == Cipher.ENCRYPT_MODE, params);
		
		byte[] buf = new byte[cipher2.getOutputSize(len)];
		int len2 = cipher2.processBytes(datas, pos, len, buf, 0);
		len2 += cipher2.doFinal(buf, len2);
		
		byte[] result = new byte[len2];
		System.arraycopy(buf, 0, result, 0, len2);*/
		
		if (log.isTraceEnabled()) {
			if (mode == Cipher.ENCRYPT_MODE) {
				log.trace("Raw data input (crypted)" + Hexview.LINESEPARATOR + Hexview.tracelog(result, 0, result.length));
			} else {
				log.trace("Raw data input (decrypted)" + Hexview.LINESEPARATOR + Hexview.tracelog(result, 0, result.length));
			}
		}
		return result;
	}
	
	public KeystoreTool getKeystoreTool() {
		return kt_tool;
	}
	
	public SSLContext getSSLContext() {
		return ssl_context;
	}
}
