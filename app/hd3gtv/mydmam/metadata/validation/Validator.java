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

import hd3gtv.mydmam.metadata.Analyser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

/**
 * Test if some analysis JSON result values match with predefinited values.
 */
public class Validator {
	
	private LinkedHashMap<Analyser, List<Constraint>> rules;
	
	public Validator() {
		rules = new LinkedHashMap<Analyser, List<Constraint>>();
	}
	
	void addRule(Analyser applyto, Constraint constraint) {
		if (applyto == null) {
			throw new NullPointerException("\"applyto\" can't to be null");
		}
		if (constraint == null) {
			throw new NullPointerException("\"constraint\" can't to be null");
		}
		
		List<Constraint> analyser_rules = null;
		if (rules.containsKey(applyto)) {
			analyser_rules = rules.get(applyto);
		}
		
		if (analyser_rules == null) {
			analyser_rules = new ArrayList<Constraint>();
		}
		
		analyser_rules.add(constraint);
		rules.put(applyto, analyser_rules);
	}
	
	/**
	 * @return null if ok, or causes if fail.
	 */
	public List<RejectCause> validate(LinkedHashMap<Analyser, JSONObject> analysis_results) {
		List<RejectCause> rejects = new ArrayList<RejectCause>();
		
		JSONObject analyst_result;
		List<Constraint> analyser_rules;
		Constraint constraint;
		for (Map.Entry<Analyser, List<Constraint>> entry : rules.entrySet()) {
			if (analysis_results.containsKey(entry.getKey()) == false) {
				continue;
			}
			analyst_result = analysis_results.get(entry.getKey());
			analyser_rules = entry.getValue();
			for (int pos_rules = 0; pos_rules < analyser_rules.size(); pos_rules++) {
				constraint = analyser_rules.get(pos_rules);
				if (constraint.isPassing(analyst_result) == false) {
					rejects.add(new RejectCause(entry.getKey(), analyst_result, constraint));
				}
			}
		}
		
		if (rejects.isEmpty()) {
			return null;
		}
		
		return rejects;
	}
}
