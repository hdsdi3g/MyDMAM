/*
 * This file is part of Java Tools by hdsdi3g'.
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2009-2012
 * 
*/
package hd3gtv.tools;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ExecprocessOutputstreamHandler implements ExecprocessOutputstream {
	
	private InputStream inputstream;
	
	public ExecprocessOutputstreamHandler(InputStream inputstream) {
		this.inputstream = inputstream;
		if (inputstream == null) {
			throw new NullPointerException("\"inputstream\" can't to be null");
		}
	}
	
	public void onStartProcess(OutputStream outputstream) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(inputstream, 32 * 1024);
		int len;
		byte[] buffer = new byte[32 * 1024];
		
		try {
			while ((len = bis.read(buffer)) > 0) {
				outputstream.write(buffer, 0, len);
			}
			outputstream.close();
		} catch (IOException e) {
			if (e.getMessage().equalsIgnoreCase("Broken pipe")) {
				try {
					bis.close();
				} catch (IOException e1) {
				}
			} else {
				bis.close();
				throw e;
			}
		}
	}
	
}
