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
import java.util.LinkedHashMap;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVStrategy;

import com.google.gson.JsonElement;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElasticsearchBulkOperation;
import hd3gtv.mydmam.gson.GsonKit;

public class CSVESImporter {
	
	private Charset charset;
	public static final CSVStrategy MS_EXCEL = new CSVStrategy(';', '\"', '#', '\\', true, true, true, true);
	private ArrayList<ImportRoutingEntry> import_routes;
	private String es_index;
	private String es_type;
	
	public CSVESImporter(Charset charset, Object configuration_node, String es_index, String es_type) throws FileNotFoundException {
		this.charset = charset;
		if (charset == null) {
			throw new NullPointerException("\"charset\" can't to be null");
		}
		this.es_index = es_index;
		if (es_index == null) {
			throw new NullPointerException("\"es_index\" can't to be null");
		}
		this.es_type = es_type;
		if (es_type == null) {
			throw new NullPointerException("\"es_type\" can't to be null");
		}
		
		JsonElement j_conf = MyDMAM.gson_kit.getGsonSimple().toJsonTree(configuration_node);
		import_routes = MyDMAM.gson_kit.getGsonSimple().fromJson(j_conf, GsonKit.type_ArrayList_ImportRoutingEntry);
	}
	
	public void importCSV(File csv_file) throws IOException {
		ElasticsearchBulkOperation bulk = Elasticsearch.prepareBulk();
		
		InputStreamReader isr = new InputStreamReader(new FileInputStream(csv_file), charset);
		try {
			CSVParser parser = new CSVParser(isr, MS_EXCEL);
			
			int index_key_pos = import_routes.indexOf(import_routes.stream().filter(entry -> {
				return entry.index_key;
			}).findFirst().orElseThrow(() -> {
				return new IndexOutOfBoundsException("No index_key referenced in configuration");
			}));
			
			String[] line;
			int actual_line = -1;
			ImportRoutingEntry routing_entry;
			try {
				while (((line = parser.getLine()) != null)) {
					actual_line++;
					
					LinkedHashMap<String, Object> line_data = new LinkedHashMap<>(line.length);
					String key = null;
					for (int pos = 0; pos < line.length; pos++) {
						String cell = line[pos];
						routing_entry = import_routes.get(pos);
						if (pos == index_key_pos) {
							key = (String) routing_entry.getValue(cell);
						} else {
							line_data.put(routing_entry.name, routing_entry.getValue(cell));
						}
					}
					line_data.put("mydmam_record_date", System.currentTimeMillis());
					
					bulk.add(bulk.getClient().prepareIndex(es_index, es_type, key).setSource(MyDMAM.gson_kit.getGsonPretty().toJson(line_data)));
				}
			} catch (ParseException | NumberFormatException | IndexOutOfBoundsException e) {
				String log_line = "";
				if (actual_line > -1) {
					log_line = " (line " + actual_line + ")";
				}
				throw new IOException("Problem during parsing" + log_line, e);
			}
		} catch (IOException e) {
			isr.close();
			throw e;
		}
		
		bulk.terminateBulk();
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
				if (csv_cell.equals("")) {
					return 0l;
				}
				return Long.parseLong(csv_cell);
			} else if (type == ImportType.FLOAT) {
				if (csv_cell.equals("")) {
					return 0d;
				}
				return Double.parseDouble(csv_cell);
			} else if (type == ImportType.DATE) {
				if (date_format == null) {
					date_format = new SimpleDateFormat(setup);
				}
				if (csv_cell.equals("")) {
					return -1l;
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
