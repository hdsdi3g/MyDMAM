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
package hd3gtv.mydmam.metadata.validation;

import java.util.List;

import com.jayway.jsonpath.JsonPath;

import hd3gtv.mydmam.Loggers;

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
	
	final boolean isPassing(String value) {
		List<Object> result = JsonPath.read(value, rule);
		if (result == null) {
			return false;
		}
		if (result.isEmpty()) {
			return false;
		}
		
		for (int pos = 0; pos < result.size(); pos++) {
			if (isInternalPassing(result.get(pos)) == false) {
				if (Loggers.Metadata.isTraceEnabled()) {
					Loggers.Metadata.trace("Validation test failed for [" + this.toString() + "/" + getClass().getSimpleName() + "] this=" + getReference() + " is not like item=" + result.get(pos));
				}
				return false;
			}
		}
		if (Loggers.Metadata.isTraceEnabled()) {
			Loggers.Metadata.trace("Validation ok for " + this.toString());
		}
		return true;
	}
	
	protected abstract boolean isInternalPassing(Object value);
	
	abstract String getReference();
	
	public String toString() {
		return rule + " :: " + comparator.name();
	}
	
}
