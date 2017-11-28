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
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;

import hd3gtv.mydmam.MyDMAM;
import junit.framework.TestCase;

public class ProtocolCypherTest extends TestCase {
	
	private static final byte[] source = "Lor€m ipsum dolor sit amet, consectetur adipiscing elit. Sed nec imperdiet est. Quisque ac imperdiet metus. Morbi euismod sapien vitae euismod lacinia. Vestibulum porttitor nec sem at interdum. Praesent eu maximus turpis, ac convallis sapien. Vivamus at libero odio. Quisque eleifend tristique bibendum. Nulla facilisi. Mauris mattis velit a dolor lacinia mattis at non lorem. Duis porttitor sodales tellus, nec iaculis nulla. Proin vulputate dolor in nisi ultricies, a hendrerit ipsum porttitor. Praesent vitae fringilla diam. Fusce turpis nisl, laoreet commodo sapien vel, dignissim bibendum tortor. Nunc a dictum dui. In tincidunt vitae eros in luctus. Etiam vel neque maximus, placerat metus eu, scelerisque erat. Etiam malesuada fringilla nibh vel vestibulum. Integer commodo dolor nec tempus iaculis. Nunc feugiat, metus non iaculis tempus, sapien magna pellentesque elit, sed imperdiet est ex id dolor. Aenean commodo sit amet massa in semper. Integer bibendum vitae quam eget tincidunt. In hac habitasse platea dictumst. Sed et pellentesque ex, vitae suscipit lorem. Nam ac pellentesque est. Duis auctor purus ac fermentum dictum. Duis dictum ligula vel sem maximus blandit. Vivamus a ipsum elementum, consequat libero suscipit, iaculis ante. Phasellus eleifend ligula at ante volutpat posuere. Sed fringilla auctor mauris. Donec blandit eget felis vel condimentum. Duis nec nunc turpis. Aliquam erat volutpat. Proin ac tempus dolor. Aenean non sem sed mauris sollicitudin consequat. Suspendisse sapien velit, viverra et ex sit amet, condimentum finibus mauris. Aliquam eget odio non augue porta porttitor non et elit. Morbi imperdiet tellus sed diam vehicula venenatis. Phasellus fermentum dui id bibendum euismod. Ut convallis sit amet nulla blandit volutpat. Nullam malesuada, sapien at maximus lacinia, dui eros pellentesque mauris, a convallis augue lorem in quam. Cras sagittis diam ut ante mollis tincidunt. Aliquam eget fermentum risus. Maecenas finibus pretium sem, et placerat quam sodales at. Aenean vitae massa et eros bibendum ullamcorper at ac enim. Ut vitae condimentum urna. Donec a tellus a ligula accumsan aliquam quis nec risus. Nam a quam tincidunt, efficitur purus at, fringilla leo. Nunc sed ligula mattis, porta nunc ac, bibendum ante. Quisque finibus tortor eget ligula sodales, at ultricies orci accumsan. In hac habitasse platea dictumst. Etiam convallis velit condimentum pellentesque condimentum. Sed a nisi libero. Cras vestibulum magna neque, nec posuere augue condimentum ac. Cras varius dapibus maximus. Pellentesque ultrices ante magna, ac tempor lorem semper non. Phasellus ut tortor nisi. Curabitur a diam aliquet, pellentesque nisi id, gravida massa. Vivamus id arcu congue, dignissim sapien vel, bibendum tellus. Nullam id leo quam. Donec fermentum mauris id mauris imperdiet, non euismod nisi congue."
			.getBytes(MyDMAM.UTF8);
	
	public void testNormal() throws IOException, GeneralSecurityException {
		Protocol p = new Protocol("ThisIsAPassword");
		
		byte[] result = p.encrypt(source);
		
		assertFalse(Arrays.equals(result, source));
		
		byte[] result2 = p.decrypt(result);
		
		assertTrue(Arrays.equals(source, result2));
	}
	
	public void testNormal2() throws IOException, GeneralSecurityException {
		Protocol p = new Protocol("ThisIsAPassword");
		byte[] result = p.encrypt(source);
		p = null;
		
		assertFalse(Arrays.equals(result, source));
		
		Protocol p2 = new Protocol("ThisIsAPassword");
		byte[] result2 = p2.decrypt(result);
		
		assertTrue(Arrays.equals(source, result2));
	}
	
	public void testNotSamePassword() throws IOException, GeneralSecurityException {
		Protocol p = new Protocol("ThisIsAPassword1");
		byte[] result = p.encrypt(source);
		p = null;
		
		Protocol p2 = new Protocol("ThisIsAPassword2");
		byte[] result2 = p2.encrypt(result);
		
		assertFalse(Arrays.equals(result, result2));
	}
	
	public void testBadPassword() throws IOException, NoSuchAlgorithmException, NoSuchProviderException {
		Protocol p = new Protocol("GoodPassword");
		byte[] result;
		try {
			result = p.encrypt(source);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
		p = null;
		
		Protocol p2 = new Protocol("BadPassword");
		byte[] result2 = null;
		GeneralSecurityException gse = null;
		try {
			result2 = p2.decrypt(result);
		} catch (GeneralSecurityException e) {
			gse = e;
		}
		
		assertNull(result2);
		assertNotNull(gse);
		assertEquals("org.bouncycastle.crypto.InvalidCipherTextException: pad block corrupted", gse.getMessage());
	}
	
	public void testAltered() throws IOException, NoSuchAlgorithmException, NoSuchProviderException {
		Protocol p = new Protocol("Password");
		byte[] result;
		try {
			result = p.encrypt(source);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
		
		result[result.length - 1]++;
		
		byte[] result2 = null;
		GeneralSecurityException gse = null;
		try {
			result2 = p.decrypt(result);
		} catch (GeneralSecurityException e) {
			gse = e;
		}
		
		assertNull(result2);
		assertNotNull(gse);
		assertEquals("org.bouncycastle.crypto.InvalidCipherTextException: pad block corrupted", gse.getMessage());
	}
	
}
