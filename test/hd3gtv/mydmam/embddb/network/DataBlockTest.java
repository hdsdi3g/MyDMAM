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
 * Copyright (C) hdsdi3g for hd3g.tv 26 nov. 2017
 * 
*/
package hd3gtv.mydmam.embddb.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.UUID;

import junit.framework.TestCase;

public class DataBlockTest extends TestCase {
	
	public void test() throws IOException, GeneralSecurityException {
		HandleName handle = new HandleName("RHandleName");
		
		Protocol p = new Protocol("A");
		PoolManager p_m = new PoolManager(p, UUID.randomUUID());
		
		RequestHandler<Void> r_h = new RequestHandler<Void>(p_m) {
			public HandleName getHandleName() {
				return handle;
			}
			
			public void onRequest(DataBlock block, Node source_node) {
			}
			
			public DataBlock createRequest(Void options) {
				return null;
			}
		};
		
		String data = "This is a data";
		DataBlock db = new DataBlock(r_h, data);
		assertEquals(handle, db.getRequestName());
		
		ByteBuffer payload = db.getFramePayloadContent();
		
		DataBlock db2 = new DataBlock(payload);
		assertEquals(db.getCreateDate(), db2.getCreateDate());
		assertEquals(db.getRequestName(), db2.getRequestName());
		
		assertEquals(data, db.getStringDatas());
		assertEquals(data, db.getStringDatas());
	}
	
}
