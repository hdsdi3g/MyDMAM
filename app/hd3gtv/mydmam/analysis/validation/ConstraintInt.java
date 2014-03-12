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

public class ConstraintInt extends Constraint {
	
	private int reference;
	
	public ConstraintInt(String scope, Comparator comparator, int reference) {
		super(scope, comparator);
		this.reference = reference;
	}
	
	protected boolean isInternalPassing(Object value) {
		int int_value;
		try {
			int_value = (Integer) value;
		} catch (NumberFormatException e1) {
			if (value instanceof String) {
				try {
					int_value = Integer.parseInt((String) value);
				} catch (NumberFormatException e2) {
					return false;
				}
			} else if (value instanceof Number) {
				int_value = ((Number) value).intValue();
			} else {
				return false;
			}
		}
		
		if (comparator == Comparator.EQUALS) {
			return reference == int_value;
		} else if (comparator == Comparator.DIFFERENT) {
			return reference != int_value;
		} else if (comparator == Comparator.GREATER_THAN) {
			return reference < int_value;
		} else if (comparator == Comparator.SMALLER_THAN) {
			return reference > int_value;
		} else {
			return false;
		}
	}
}
