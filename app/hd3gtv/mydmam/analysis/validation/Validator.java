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

import hd3gtv.mydmam.analysis.Analyser;
import hd3gtv.mydmam.analysis.MetadataIndexerResult;

import java.util.List;

public class Validator {
	
	// private List<ValidationRule> rules;
	
	public Validator addRule(Analyser applyto, Constraint constraint, boolean fail_is_fatal) {
		// TODO Auto-generated constructor stub
		return this;
	}
	
	/**
	 * @return null if ok, or causes.
	 */
	public List<RejectValidationCause> validate(MetadataIndexerResult metadatas) {
		// TODO ...
		// TODO array search
		
		return null;
	}
}
