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

import org.json.simple.JSONObject;

abstract class Constraint {
	
	protected String rule;
	protected Comparator comparator;
	
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
	
	final boolean isPassing(JSONObject value) {
		/*Object item = jsonCrawler(value, scope);
		if (item == null) {
			return false;
		}
		return isInternalPassing(item);*/
		return false;
		// TODO
	}
	
	protected abstract boolean isInternalPassing(Object value);
	
}
