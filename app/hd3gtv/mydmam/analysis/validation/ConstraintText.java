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

public class ConstraintText extends Constraint {
	
	private String reference;
	
	public ConstraintText(String scope, Comparator comparator, String reference) {
		super(scope, comparator);
		this.reference = reference;
		if (reference == null) {
			throw new NullPointerException("\"reference\" can't to be null");
		}
	}
	
	protected boolean isInternalPassing(Object value) {
		String str_value = value.toString();
		if (value instanceof String) {
			str_value = (String) value;
		} else if (value instanceof Number) {
			str_value = Long.toString(((Number) value).longValue());
		}
		
		if (comparator == Comparator.EQUALS) {
			return reference.equalsIgnoreCase(str_value);
		} else {
			return (reference.equalsIgnoreCase(str_value)) == false;
		}
	}
}
