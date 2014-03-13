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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.mydmam.analysis.validation;

import java.util.List;

import org.json.simple.JSONObject;

import com.jayway.jsonpath.JsonPath;

abstract class Constraint {
	
	protected String rule;
	protected Comparator comparator;
	
	/**
	 * @param rule like $.streams[?(@.codec_type == 'audio')].sample_rate (via https://code.google.com/p/json-path)
	 */
	protected Constraint(String rule, Comparator comparator) {
		this.rule = rule;
		this.comparator = comparator;
		if (rule == null) {
			throw new NullPointerException("\"scope\" can't to be null");
		}
		this.comparator = comparator;
		if (comparator == null) {
			throw new NullPointerException("\"comparator\" can't to be null");
		}
	}
	
	final List<Object> extractValueFromJson(JSONObject value) {
		return JsonPath.read(value.toJSONString(), rule);
	}
	
	final boolean isPassing(JSONObject value) {
		List<Object> result = extractValueFromJson(value);
		if (result == null) {
			return false;
		}
		if (result.isEmpty()) {
			return false;
		}
		for (int pos = 0; pos < result.size(); pos++) {
			if (isInternalPassing(result.get(pos)) == false) {
				return false;
			}
		}
		return true;
	}
	
	protected abstract boolean isInternalPassing(Object value);
	
	abstract String getReference();
	
	public String toString() {
		return rule + " :: " + comparator.name();
	}
	
}
