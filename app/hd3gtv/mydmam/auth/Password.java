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

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.net.util.Base64;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.BCrypt;

public class Password {
	
	private IvParameterSpec salt;
	private SecretKey skeySpec;
	private String master_password_key;
	
	static {
		MyDMAM.testIllegalKeySize();
	}
	
	public Password(String master_password_key) throws NoSuchAlgorithmException, NoSuchProviderException, UnsupportedEncodingException {
		this.master_password_key = master_password_key;
		if (master_password_key == null) {
			throw new NullPointerException("\"master_password_key\" can't to be null");
		}
		if (master_password_key.isEmpty()) {
			throw new NullPointerException("\"master_password_key\" can't to be empty");
		}
		
		MessageDigest md = MessageDigest.getInstance("SHA-256", "BC");
		byte[] key = md.digest(master_password_key.getBytes("UTF-8"));
		
		skeySpec = new SecretKeySpec(key, "AES");
		salt = new IvParameterSpec(key, 0, 16);
	}
	
	public String getMaster_password_key() {
		return master_password_key;
	}
	
	/**
	 * @return AES(BCrypt(clear_password, 10), SHA256(master_password_key))
	 */
	public byte[] getHashedPassword(String clear_password) {
		try {
			byte[] hashed = (BCrypt.hashpw(clear_password, BCrypt.gensalt(10))).getBytes("UTF-8");
			
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, salt);
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(cipher.update(hashed));
			baos.write(cipher.doFinal());
			
			return baos.toByteArray();
		} catch (Exception e) {
			Loggers.Auth.error("Can't prepare password", e);
		}
		return null;
	}
	
	public boolean checkPassword(String candidate_password, byte[] raw_password) {
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, salt);
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(cipher.update(raw_password));
			baos.write(cipher.doFinal());
			
			return BCrypt.checkpw(candidate_password, new String(baos.toByteArray()));
		} catch (Exception e) {
			Loggers.Auth.error("Can't extract hashed password", e);
		}
		return false;
	}
	
	/**
	 * @return 12 first chars of Base64(SHA-264(random(1024b)))
	 */
	public static String passwordGenerator() throws NoSuchAlgorithmException, NoSuchProviderException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		Random r = new Random();
		byte[] fill = new byte[1024];
		r.nextBytes(fill);
		byte[] key = md.digest(fill);
		return new String(Base64.encodeBase64String(key)).substring(0, 12);
	}
}
