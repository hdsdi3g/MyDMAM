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
package hd3gtv.mydmam.analysis.validation;

import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.mydmam.analysis.Analyser;

import java.util.List;

import org.json.simple.JSONObject;

public class RejectCause implements Log2Dumpable {
	
	private Analyser analyser;
	private JSONObject source;
	private Constraint constraint;
	private boolean fatal;
	
	RejectCause(Analyser analyser, JSONObject source, Constraint constraint, boolean fatal) {
		this.analyser = analyser;
		if (analyser == null) {
			throw new NullPointerException("\"analyser\" can't to be null");
		}
		this.source = source;
		if (source == null) {
			throw new NullPointerException("\"source\" can't to be null");
		}
		this.constraint = constraint;
		if (constraint == null) {
			throw new NullPointerException("\"constraint\" can't to be null");
		}
		this.fatal = fatal;
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("fatal", fatal);
		dump.add("analyser", analyser.getName());
		dump.add("rule", constraint.rule);
		List<Object> values = constraint.extractValueFromJson(source);
		if (values == null) {
			dump.add("value", "(null)");
			return dump;
		} else if (values.isEmpty()) {
			dump.add("value", "(not found)");
			return dump;
		} else if (values.size() == 1) {
			dump.add("value", values.get(0));
		} else {
			dump.add("values", values);
		}
		dump.add("comparator", constraint.comparator.name());
		dump.add("reference", constraint.getReference());
		return dump;
	}
	
	public static Log2Dump getAllLog2Dump(List<RejectCause> causes) {
		if (causes == null) {
			return null;
		}
		if (causes.isEmpty()) {
			return null;
		}
		Log2Dump dump = new Log2Dump();
		for (int pos_res = 0; pos_res < causes.size(); pos_res++) {
			dump.add("cause " + (pos_res + 1) + ":", "____");
			dump.addAll(causes.get(pos_res));
		}
		return dump;
	}
}
