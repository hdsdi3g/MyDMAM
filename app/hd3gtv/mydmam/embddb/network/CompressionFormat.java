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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public enum CompressionFormat {
	
	NONE {
		public byte[] shrink(byte[] source) {
			return source;
		}
		
		public byte[] expand(byte[] source) {
			return source;
		}
		
		public byte getReference() {
			return 0;
		}
		
		public String toString() {
			return "uncompressed";
		}
	},
	GZIP {
		public byte[] shrink(byte[] source) throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(source.length);
			GZIPOutputStream gzout = new GZIPOutputStream(baos);
			gzout.write(source, 0, source.length);
			gzout.finish();
			gzout.flush();
			gzout.close();
			return baos.toByteArray();
		}
		
		public byte[] expand(byte[] source) throws IOException {
			ByteArrayInputStream bais = new ByteArrayInputStream(source);
			GZIPInputStream gzin = new GZIPInputStream(bais, source.length);
			ByteArrayOutputStream baos = new ByteArrayOutputStream(source.length);
			
			byte[] buffer = new byte[source.length];
			int readed_size;
			
			while ((readed_size = gzin.read(buffer)) > 0) {
				baos.write(buffer, 0, readed_size);
			}
			
			return baos.toByteArray();
		}
		
		public byte getReference() {
			return 1;
		}
		
		public String toString() {
			return "gziped";
		}
	};
	
	public abstract byte[] shrink(byte[] source) throws IOException;
	
	public abstract byte[] expand(byte[] source) throws IOException;
	
	public abstract byte getReference();
	
	public static CompressionFormat fromReference(byte ref) {
		return Arrays.asList(values()).stream().filter(c_f -> c_f.getReference() == ref).findFirst().orElse(null);
	}
	
}
