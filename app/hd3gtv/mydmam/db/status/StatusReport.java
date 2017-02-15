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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StatusReport {
	
	public final String report_name;
	public final long creation_date;
	public final List<String> colums_names;
	public final ArrayList<StatusReportTable> datas;
	
	public StatusReport(String report_name) {
		this.report_name = report_name;
		if (report_name == null) {
			throw new NullPointerException("\"report_name\" can't to be null");
		}
		creation_date = System.currentTimeMillis();
		colums_names = new ArrayList<String>();
		datas = new ArrayList<>();
	}
	
	StatusReport addCell(String col_name, String row_name, String separator, Object... separated_values) {
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
		return this;
	}
	
	StatusReport addCell(String col_name, String row_name, Object value) {
		if (value == null) {
			addCell(col_name, row_name, "");
		} else {
			addCell(col_name, row_name, String.valueOf(value));
		}
		return this;
	}
	
	StatusReport addCell(String col_name, String row_name, String value) {
		if (colums_names.contains(col_name) == false) {
			colums_names.add(col_name);
		}
		
		Optional<StatusReportTable> o_table = datas.stream().filter(srt -> {
			return srt.name.equals(row_name);
		}).findFirst();
		
		StatusReportTable current = null;
		if (o_table.isPresent() == false) {
			ArrayList<String> values = new ArrayList<String>();
			values.add(value);
			current = new StatusReportTable(row_name, values);
			datas.add(current);
		} else {
			o_table.get().content.add(value);
		}
		
		return this;
	}
	
}
