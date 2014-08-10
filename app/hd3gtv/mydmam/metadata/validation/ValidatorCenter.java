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

import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.metadata.GeneratorAnalyser;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.EntryAnalyser;

import java.util.ArrayList;
import java.util.List;

/**
 * Add OR relation between validators
 */
public class ValidatorCenter {
	
	private List<List<Validator>> validators_lists;
	private List<Validator> current_validator_list;
	
	public ValidatorCenter() {
		validators_lists = new ArrayList<List<Validator>>();
		current_validator_list = new ArrayList<Validator>();
		validators_lists.add(current_validator_list);
	}
	
	ValidatorCenter addValidator(Validator validator) {
		if (validator == null) {
			throw new NullPointerException("\"validator\" can't to be null");
		}
		current_validator_list.add(validator);
		return this;
	}
	
	/**
	 * @param rule like $.streams[?(@.codec_type == 'audio')].sample_rate (via https://code.google.com/p/json-path)
	 * @param reference with OR relations
	 */
	public ValidatorCenter addRule(Class<? extends EntryAnalyser> applyto, String rule, Comparator comparator, Float... references) {
		Validator validator = new Validator();
		for (int pos = 0; pos < references.length; pos++) {
			validator.addRule(applyto, new ConstraintFloat(rule, comparator, references[pos]));
		}
		addValidator(validator);
		return this;
	}
	
	/**
	 * @param rule like $.streams[?(@.codec_type == 'audio')].sample_rate (via https://code.google.com/p/json-path)
	 * @param reference with OR relations
	 */
	public ValidatorCenter addRule(Class<? extends EntryAnalyser> applyto, String rule, Comparator comparator, String... references) {
		Validator validator = new Validator();
		for (int pos = 0; pos < references.length; pos++) {
			validator.addRule(applyto, new ConstraintString(rule, comparator, references[pos]));
		}
		addValidator(validator);
		return this;
	}
	
	/**
	 * @param rule like $.streams[?(@.codec_type == 'audio')].sample_rate (via https://code.google.com/p/json-path)
	 * @param reference with OR relations
	 */
	public ValidatorCenter addRule(Class<? extends EntryAnalyser> applyto, String rule, Comparator comparator, Integer... references) {
		Validator validator = new Validator();
		for (int pos = 0; pos < references.length; pos++) {
			validator.addRule(applyto, new ConstraintInteger(rule, comparator, references[pos]));
		}
		addValidator(validator);
		return this;
	}
	
	/**
	 * Start a new OR relation block.
	 */
	public ValidatorCenter and() {
		if (current_validator_list.isEmpty()) {
			return this;
		}
		current_validator_list = new ArrayList<Validator>();
		validators_lists.add(current_validator_list);
		return this;
	}
	
	public boolean validate(GeneratorAnalyser entry, Container container) {// TODO refactor
		if (container == null) {
			throw new NullPointerException("\"container\" can't to be null");
		}
		List<Validator> current_list;
		Validator current;
		List<RejectCause> rejects = null;
		Log2Dump dump = new Log2Dump();
		boolean passed;
		
		for (int pos_lists = 0; pos_lists < validators_lists.size(); pos_lists++) {
			/**
			 * AND relations
			 */
			current_list = validators_lists.get(pos_lists);
			passed = false;
			if (current_list.isEmpty()) {
				continue;
			}
			for (int pos_list = 0; pos_list < current_list.size(); pos_list++) {
				/**
				 * OR relations
				 */
				current = current_list.get(pos_list);
				rejects = current.validate(container);
				if (rejects == null) {
					passed = true;
					break;
				} else {
					dump.add("trial " + (pos_list + 1), "___");
					dump.addAll(RejectCause.getAllLog2Dump(rejects));
				}
			}
			if (passed == false) {
				// Log2.log.debug("Fail to validate analysis", dump);
			} else {
				dump = new Log2Dump();
			}
		}
		return true;
	}
}
