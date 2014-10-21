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
package hd3gtv.mydmam.db.status;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVStrategy;

public class StatusReport {
	
	public final long creation_date;
	public final List<String> colums_names;
	public final Map<String, List<String>> content;
	
	public StatusReport() {
		creation_date = System.currentTimeMillis();
		colums_names = new ArrayList<String>();
		content = new LinkedHashMap<String, List<String>>();
	}
	
	void addCell(String col_name, String row_name, String separator, Object... separated_values) {
		StringBuffer sb = new StringBuffer();
		for (int pos = 0; pos < separated_values.length; pos++) {
			if (separated_values[pos] == null) {
				continue;
			}
			sb.append(separated_values[pos]);
			if (pos + 1 < separated_values.length) {
				sb.append(separator);
			}
		}
		addCell(col_name, row_name, sb.toString().trim());
	}
	
	void addCell(String col_name, String row_name, Object value) {
		if (value == null) {
			addCell(col_name, row_name, "");
		} else {
			addCell(col_name, row_name, String.valueOf(value));
		}
	}
	
	void addCell(String col_name, String row_name, String value) {
		if (colums_names.contains(col_name) == false) {
			colums_names.add(col_name);
		}
		List<String> values = null;
		if (content.containsKey(row_name)) {
			values = content.get(row_name);
		} else {
			values = new ArrayList<String>();
			content.put(row_name, values);
		}
		if (value == null) {
			values.add("");
		} else {
			values.add(value);
		}
	}
	
	public String toCSVString() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		CSVPrinter printer = new CSVPrinter(baos);
		printer.setStrategy(CSVStrategy.EXCEL_STRATEGY);
		
		printer.printlnComment("Created at " + new Date(this.creation_date));
		
		if (colums_names.isEmpty()) {
			return "";
		}
		String[] line = new String[colums_names.size() + 1];
		line[0] = ""; // First cell in top left.
		for (int pos = 1; pos < line.length; pos++) {
			line[pos] = colums_names.get(pos - 1);
		}
		printer.println(line);
		
		for (Map.Entry<String, List<String>> row : content.entrySet()) {
			line[0] = row.getKey();
			List<String> values = row.getValue();
			
			for (int pos = 1; pos < line.length; pos++) {
				if ((pos - 1) < values.size()) {
					line[pos] = values.get(pos - 1);
				} else {
					line[pos] = "";
				}
			}
			printer.println(line);
		}
		
		return baos.toString();
	}
	
}
