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

public class RejectValidationCause {
	
	private String cause;
	private ConstraintText constraintText;
	
	RejectValidationCause(String cause, ConstraintText constraintText) {
		this.cause = cause;
		if (cause == null) {
			throw new NullPointerException("\"cause\" can't to be null");
		}
		this.constraintText = constraintText;
		if (constraintText == null) {
			throw new NullPointerException("\"constraint\" can't to be null");
		}
	}
	
	public String getCause() {
		return cause;
	}
	
	public ConstraintText getConstraint() {
		return constraintText;
	}
	
}
