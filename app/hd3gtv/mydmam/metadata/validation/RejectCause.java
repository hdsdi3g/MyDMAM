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
package hd3gtv.mydmam.metadata.validation;

import java.util.LinkedHashMap;
import java.util.List;

import com.jayway.jsonpath.JsonPath;

import hd3gtv.mydmam.metadata.container.EntryAnalyser;

public class RejectCause {
	
	private Class<? extends EntryAnalyser> generatorAnalyser;
	private String source;
	private Constraint constraint;
	
	RejectCause(Class<? extends EntryAnalyser> generatorAnalyser, String source, Constraint constraint) {
		this.generatorAnalyser = generatorAnalyser;
		if (generatorAnalyser == null) {
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
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("entry type, : ");
		sb.append(generatorAnalyser.getName());
		sb.append(", rule: ");
		sb.append(constraint.rule);
		List<Object> values = JsonPath.read(source, constraint.rule);
		if (values == null) {
			sb.append(", value: (null)");
			return sb.toString();
		} else if (values.isEmpty()) {
			sb.append(", value: (not found)");
			return sb.toString();
		} else if (values.size() == 1) {
			sb.append(", value: ");
			sb.append(values.get(0));
		} else {
			sb.append(", values: ");
			sb.append(values);
		}
		sb.append(", comparator: ");
		sb.append(constraint.comparator.name());
		sb.append(", reference: ");
		sb.append(constraint.getReference());
		
		return sb.toString();
	}
	
	public static LinkedHashMap<String, Object> getAllLogDebug(List<RejectCause> causes) {
		if (causes == null) {
			return null;
		}
		if (causes.isEmpty()) {
			return null;
		}
		LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
		for (int pos_res = 0; pos_res < causes.size(); pos_res++) {
			log.put("cause " + (pos_res + 1) + ":", causes.get(pos_res));
		}
		return log;
	}
}
