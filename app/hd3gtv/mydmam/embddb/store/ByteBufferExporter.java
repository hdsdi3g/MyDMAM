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
 * Copyright (C) hdsdi3g for hd3g.tv 9 oct. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface ByteBufferExporter {
	
	/**
	 * Get data size from getByteBufferWriteSize.
	 * Don't clear or flip buffer. Just put in it.
	 */
	public void toByteBuffer(ByteBuffer write_buffer) throws IOException;
	
	/**
	 * For toByteBuffer()
	 */
	public int getByteBufferWriteSize();
	
}
