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

import org.bouncycastle.operator.OperatorCreationException;

import junit.framework.TestCase;

public class DataBlockTest extends TestCase {
	
	private static final String example = "Lor€m ipsum dolor sit amet, consectetur adipiscing elit. Sed nec imperdiet est. Quisque ac imperdiet metus. Morbi euismod sapien vitae euismod lacinia. Vestibulum porttitor nec sem at interdum. Praesent eu maximus turpis, ac convallis sapien. Vivamus at libero odio. Quisque eleifend tristique bibendum. Nulla facilisi. Mauris mattis velit a dolor lacinia mattis at non lorem. Duis porttitor sodales tellus, nec iaculis nulla. Proin vulputate dolor in nisi ultricies, a hendrerit ipsum porttitor. Praesent vitae fringilla diam. Fusce turpis nisl, laoreet commodo sapien vel, dignissim bibendum tortor. Nunc a dictum dui. In tincidunt vitae eros in luctus. Etiam vel neque maximus, placerat metus eu, scelerisque erat. Etiam malesuada fringilla nibh vel vestibulum. Integer commodo dolor nec tempus iaculis. Nunc feugiat, metus non iaculis tempus, sapien magna pellentesque elit, sed imperdiet est ex id dolor. Aenean commodo sit amet massa in semper. Integer bibendum vitae quam eget tincidunt. In hac habitasse platea dictumst. Sed et pellentesque ex, vitae suscipit lorem. Nam ac pellentesque est. Duis auctor purus ac fermentum dictum. Duis dictum ligula vel sem maximus blandit. Vivamus a ipsum elementum, consequat libero suscipit, iaculis ante. Phasellus eleifend ligula at ante volutpat posuere. Sed fringilla auctor mauris. Donec blandit eget felis vel condimentum. Duis nec nunc turpis. Aliquam erat volutpat. Proin ac tempus dolor. Aenean non sem sed mauris sollicitudin consequat. Suspendisse sapien velit, viverra et ex sit amet, condimentum finibus mauris. Aliquam eget odio non augue porta porttitor non et elit. Morbi imperdiet tellus sed diam vehicula venenatis. Phasellus fermentum dui id bibendum euismod. Ut convallis sit amet nulla blandit volutpat. Nullam malesuada, sapien at maximus lacinia, dui eros pellentesque mauris, a convallis augue lorem in quam. Cras sagittis diam ut ante mollis tincidunt. Aliquam eget fermentum risus. Maecenas finibus pretium sem, et placerat quam sodales at. Aenean vitae massa et eros bibendum ullamcorper at ac enim. Ut vitae condimentum urna. Donec a tellus a ligula accumsan aliquam quis nec risus. Nam a quam tincidunt, efficitur purus at, fringilla leo. Nunc sed ligula mattis, porta nunc ac, bibendum ante. Quisque finibus tortor eget ligula sodales, at ultricies orci accumsan. In hac habitasse platea dictumst. Etiam convallis velit condimentum pellentesque condimentum. Sed a nisi libero. Cras vestibulum magna neque, nec posuere augue condimentum ac. Cras varius dapibus maximus. Pellentesque ultrices ante magna, ac tempor lorem semper non. Phasellus ut tortor nisi. Curabitur a diam aliquet, pellentesque nisi id, gravida massa. Vivamus id arcu congue, dignissim sapien vel, bibendum tellus. Nullam id leo quam. Donec fermentum mauris id mauris imperdiet, non euismod nisi congue.";
	
	public void test() throws IOException, GeneralSecurityException, SecurityException, OperatorCreationException {
		HandleName handle = new HandleName("RHandleName");
		
		Protocol p = new Protocol();
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
		
		DataBlock db = new DataBlock(r_h, example);
		assertEquals(handle, db.getRequestName());
		
		ByteBuffer payload = db.getFramePayloadContent();
		
		DataBlock db2 = new DataBlock(payload);
		assertEquals(db.getCreateDate(), db2.getCreateDate());
		assertEquals(db.getRequestName(), db2.getRequestName());
		
		assertEquals(example, db.getStringDatas());
		assertEquals(example, db.getStringDatas());
	}
	
}
