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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.metadata.container;

import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;

public class RenderedContent implements Log2Dumpable {
	
	public String hash;
	public String name;
	public String producer;
	public String mime;
	public long date;
	public long size;
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("name", name);
		dump.add("hash", hash);
		dump.add("producer", producer);
		dump.add("mime", mime);
		dump.add("date", date);
		dump.add("size", size);
		return dump;
	}
	
}
