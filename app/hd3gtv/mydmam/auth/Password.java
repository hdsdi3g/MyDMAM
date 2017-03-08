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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.auth;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Random;

import org.apache.commons.net.util.Base64;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.BlockCipherPadding;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import hd3gtv.mydmam.Loggers;
import hd3gtv.tools.BCrypt;

public class Password {
	
	private KeyParameter keyParam;
	private CipherParameters params;
	private String master_password_key;
	
	public Password(String master_password_key) throws NoSuchAlgorithmException, NoSuchProviderException, UnsupportedEncodingException {
		this.master_password_key = master_password_key;
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
	}
	
	public String getMaster_password_key() {
		return master_password_key;
	}
	
	/**
	 * @throws SecurityException if clear_password is < 8 chars or not contain Upper/Lower/Digits
	 */
	private static String testIfPasswordIsStrong(String clear_password) throws SecurityException {
		char[] chars = clear_password.toCharArray();
		
		boolean is_ok_1 = false;
		boolean is_ok_2 = false;
		boolean is_ok_3 = false;
		
		int real_char_num = 0;
		for (int pos = 0; pos < chars.length; pos++) {
			if (chars[pos] > 47 && chars[pos] < 58) {
				/**
				 * digits
				 */
				is_ok_1 = true;
				real_char_num++;
			}
		}
		for (int pos = 0; pos < chars.length; pos++) {
			if (chars[pos] > 64 && chars[pos] < 91) {
				/**
				 * upper case
				 */
				is_ok_2 = true;
				real_char_num++;
			}
		}
		for (int pos = 0; pos < chars.length; pos++) {
			if (chars[pos] > 64 && chars[pos] < 91) {
				/**
				 * lower case
				 */
				is_ok_3 = true;
				real_char_num++;
			}
		}
		
		if (real_char_num < 8) {
			throw new SecurityException("Invalid tested password: not enough characters");
		}
		
		if ((is_ok_1 & is_ok_2) | (is_ok_2 & is_ok_3) | (is_ok_1 & is_ok_3)) {
			return clear_password.trim();
		}
		
		throw new SecurityException("Invalid tested password: missing digits, upper case or lower case");
	}
	
	/**
	 * @return AES(BCrypt(clear_password, 10), SHA256(master_password_key))
	 */
	public byte[] getHashedPassword(String clear_password) throws SecurityException {
		try {
			byte[] hashed = (BCrypt.hashpw(testIfPasswordIsStrong(clear_password), BCrypt.gensalt(10))).getBytes("UTF-8");
			
			BlockCipherPadding padding = new PKCS7Padding();
			BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), padding);
			cipher.reset();
			cipher.init(true, params);
			
			byte[] buf = new byte[cipher.getOutputSize(hashed.length)];
			int len = cipher.processBytes(hashed, 0, hashed.length, buf, 0);
			len += cipher.doFinal(buf, len);
			
			byte[] out = new byte[len];
			System.arraycopy(buf, 0, out, 0, len);
			
			return out;
		} catch (Exception e) {
			Loggers.Auth.error("Can't prepare password", e);
		}
		return null;
	}
	
	public boolean checkPassword(String candidate_password, byte[] raw_password) {
		try {
			BlockCipherPadding padding = new PKCS7Padding();
			BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), padding);
			cipher.reset();
			cipher.init(false, params);
			
			byte[] buf = new byte[cipher.getOutputSize(raw_password.length)];
			int len = cipher.processBytes(raw_password, 0, raw_password.length, buf, 0);
			len += cipher.doFinal(buf, len);
			
			return BCrypt.checkpw(candidate_password, new String(buf, 0, len));
		} catch (Exception e) {
			Loggers.Auth.error("Can't extract hashed password", e);
		}
		return false;
	}
	
	/**
	 * @return 12 first chars of Base64(SHA-264(random(1024b)))
	 */
	public static String passwordGenerator() throws NoSuchAlgorithmException, NoSuchProviderException {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			Random r = new Random();
			byte[] fill = new byte[1024];
			r.nextBytes(fill);
			byte[] key = md.digest(fill);
			return testIfPasswordIsStrong(new String(Base64.encodeBase64String(key)).substring(0, 12));
		} catch (SecurityException e) {
			return passwordGenerator();
		}
	}
}
