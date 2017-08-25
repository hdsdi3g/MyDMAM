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
 * Copyright (C) hdsdi3g for hd3g.tv 22 nov. 2016
 * 
*/
package hd3gtv.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Do an hexdump like:
 * 0000 06 00 44 6f.6d 61 69 6e|44 4a 42 4f.07 00 41 4f|62 6a 44 6f.63 04 13 00|32 30 31 36.2f 30 38 2f ..DomainDJBO..AO bjDoc...2016/08/
 * 0032 70 54 41 65.76 54 41 1e|00 4d 65 64.69 61 20 43|6f 6d 70 6f.73 65 72 20|38 2e 35 2e.33 2e 34 31 pTAevTA..Media C omposer 8.5.3.41
 */
public class Hexview {
	
	public static final int COLS = 32;
	public static final int ROWS = 32;
	public static final String LINESEPARATOR = System.getProperty("line.separator");
	
	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.err.println("Usage file1.ext file2.ext ...");
			System.exit(1);
		}
		
		Arrays.asList(args).forEach(f -> {
			try {
				File file = new File(f);
				
				InputStream in = new BufferedInputStream(new FileInputStream(file), 0xFFFF);
				
				byte[] buffer = new byte[COLS * ROWS];
				int len;
				Hexview hv = null;
				
				while ((len = in.read(buffer)) != -1) {
					if (hv == null) {
						hv = new Hexview(buffer, 0, len);
						hv.setSize(file.length());
					} else {
						hv.update(buffer, 0, len);
					}
					System.out.println(hv.getView());
				}
				
				IOUtils.closeQuietly(in);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(2);
			}
		});
	}
	
	private byte[] datas;
	private int pos;
	private int len;
	private long offset;
	private long size;
	
	public Hexview(byte[] datas, int pos, int len) {
		this.datas = datas;
		if (datas == null) {
			throw new NullPointerException("\"datas\" can't to be null");
		}
		this.pos = pos;
		this.len = len;
		offset = 0;
		size = len;
	}
	
	/*public static void display(byte[] datas, int pos, int len) {
		Hexview hv = new Hexview(datas, pos, len);
		System.out.println(hv.getView());
	}*/
	
	/*public static void display(byte[] datas) {
		Hexview hv = new Hexview(datas, 0, datas.length);
		System.out.println(hv.getView());
	}*/
	
	public static String tracelog(byte[] datas) {
		return new Hexview(datas, 0, datas.length).getView();
	}
	
	public static String tracelog(byte[] datas, int pos, int len) {
		return new Hexview(datas, pos, len).getView();
	}
	
	/**
	 * It will update offset.
	 */
	public void update(byte[] datas, int pos, int len) {
		this.datas = datas;
		if (datas == null) {
			throw new NullPointerException("\"datas\" can't to be null");
		}
		this.pos = pos;
		this.len = len;
		offset += len;
	}
	
	public void setSize(long size) {
		this.size = size;
	}
	
	public String getView() {
		StringBuilder sb = new StringBuilder();
		
		int pos_number_size = String.valueOf(size).length();
		char[] string_view = new char[COLS];
		
		long current_pos;
		int zeros_to_add;
		String hex_number;
		int value;
		for (int pos = this.pos; pos < len; pos++) {
			if (pos % COLS == 0) {
				current_pos = offset + (pos - this.pos);
				
				zeros_to_add = pos_number_size - String.valueOf(current_pos).length();
				sb.append(StringUtils.repeat("0", zeros_to_add));
				sb.append(current_pos);
				sb.append(" ");
			}
			
			if (pos % COLS == 4 | pos % COLS == 12 | pos % COLS == 20 | pos % COLS == 28) {
				sb.append(".");
			} else if (pos % COLS == 8 | pos % COLS == 16 | pos % COLS == 24) {
				sb.append("|");
			} else {
				sb.append(" ");
			}
			
			value = Byte.toUnsignedInt(datas[pos]);
			hex_number = String.format("%x", value);
			if (hex_number.length() == 1) {
				sb.append("0");
			}
			sb.append(hex_number);
			
			if (value > 31 & value < 127) {
				string_view[pos % COLS] = (char) datas[pos];
			} else {
				string_view[pos % COLS] = '.';
			}
			
			if (pos % COLS == COLS - 1) {
				sb.append(" ");
				for (int pos_sv = 0; pos_sv < string_view.length; pos_sv++) {
					if (pos_sv % (COLS / 2) == 0) {
						sb.append(" ");
					}
					sb.append(string_view[pos_sv]);
				}
				
				sb.append(LINESEPARATOR);
			}
		}
		
		if ((len % COLS) > 0) {
			int empty_bytes = COLS - (len % COLS);
			for (int pos = 0; pos < empty_bytes; pos++) {
				sb.append("   ");
			}
			sb.append(" ");
			for (int pos = 0; pos < (len % COLS); pos++) {
				if (pos % (COLS / 2) == 0) {
					sb.append(" ");
				}
				sb.append(string_view[pos]);
			}
		}
		
		return sb.toString();
	}
	
}
