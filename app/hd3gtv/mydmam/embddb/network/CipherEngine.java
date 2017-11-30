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
import java.security.GeneralSecurityException;

@Deprecated
public interface CipherEngine {
	
	public byte[] encrypt(byte[] cleared_datas) throws GeneralSecurityException;
	
	public byte[] decrypt(byte[] crypted_datas) throws GeneralSecurityException;
	
	public default ByteBuffer encrypt(ByteBuffer source_content) throws IOException, GeneralSecurityException {
		byte[] source_data_to_send = new byte[source_content.remaining()];
		source_content.get(source_data_to_send);
		return ByteBuffer.wrap(encrypt(source_data_to_send));
	}
	
	public default ByteBuffer decrypt(ByteBuffer source_content) throws IOException, GeneralSecurityException {
		byte[] received_source_data = new byte[source_content.remaining()];
		source_content.get(received_source_data);
		return ByteBuffer.wrap(decrypt(received_source_data));
	}
	
}
