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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.assetsxcross;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVStrategy;

import com.google.gson.JsonElement;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonKit;

public class CSVESImporter {
	
	private Charset charset;
	public static final CSVStrategy MS_EXCEL = new CSVStrategy(';', '\"', '#', '\\', true, true, true, true);
	public ArrayList<ImportRoutingEntry> import_routes;
	
	public CSVESImporter(Charset charset, Object configuration_node) throws FileNotFoundException {
		this.charset = charset;
		if (charset == null) {
			throw new NullPointerException("\"charset\" can't to be null");
		}
		
		JsonElement j_conf = MyDMAM.gson_kit.getGsonSimple().toJsonTree(configuration_node);
		import_routes = MyDMAM.gson_kit.getGsonSimple().fromJson(j_conf, GsonKit.type_ArrayList_ImportRoutingEntry);
	}
	
	public void importCSV(File csv_file) throws IOException {
		InputStreamReader isr = new InputStreamReader(new FileInputStream(csv_file), charset);
		CSVParser parser = new CSVParser(isr, MS_EXCEL);
		
		try {
			String[] line;
			while (((line = parser.getLine()) != null)) {
				// TODO
			}
		} catch (IOException e) {
			isr.close();
		}
	}
	
	private enum ImportType {
		TEXT, INTEGER, FLOAT, DATE, BOOLEAN
	}
	
	public class ImportRoutingEntry {
		private boolean index_key = false;
		private String name;
		private ImportType type = ImportType.TEXT;
		private String setup; // "yyyy/MM/dd HH:mm:ss,SSS"
		private transient SimpleDateFormat date_format;
		
		private ImportRoutingEntry() {
		}
		
		private Object getValue(String csv_cell) throws ParseException, NumberFormatException {
			if (type == ImportType.INTEGER) {
				return Long.parseLong(csv_cell);
			} else if (type == ImportType.FLOAT) {
				return Double.parseDouble(csv_cell);
			} else if (type == ImportType.DATE) {
				if (date_format == null) {
					date_format = new SimpleDateFormat(setup);
				}
				return date_format.parse(csv_cell).getTime();
			} else if (type == ImportType.BOOLEAN) {
				return setup.equalsIgnoreCase(csv_cell.trim());
			} else {
				return csv_cell;
			}
		}
		
	}
	
}
