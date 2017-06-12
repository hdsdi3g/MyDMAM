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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.tools;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TwoListComparing<Ltype, Rtype, ComparingKeyType> {
	
	private List<Ltype> l_list;
	private List<Rtype> r_list;
	
	private Function<Ltype, ComparingKeyType> l_extractor;
	private Function<Rtype, ComparingKeyType> r_extractor;
	
	/**
	 * Object comparing is based on equals and hashCode (map.containsKey) in ComparingKeyType.
	 */
	public TwoListComparing(List<Ltype> l_list, List<Rtype> r_list, Function<Ltype, ComparingKeyType> l_extractor, Function<Rtype, ComparingKeyType> r_extractor) {
		this.l_list = l_list;
		if (l_list == null) {
			throw new NullPointerException("\"l_list\" can't to be null");
		}
		this.r_list = r_list;
		if (r_list == null) {
			throw new NullPointerException("\"r_list\" can't to be null");
		}
		this.l_extractor = l_extractor;
		if (l_extractor == null) {
			throw new NullPointerException("\"l_extractor\" can't to be null");
		}
		this.r_extractor = r_extractor;
		if (r_extractor == null) {
			throw new NullPointerException("\"r_extractor\" can't to be null");
		}
	}
	
	public ComparingResult process() {
		return new ComparingResult();
	}
	
	public class ComparingResult {
		private List<Rtype> missing_in_L_added_in_R;
		private List<Ltype> missing_in_R_added_in_L;
		
		private ComparingResult() {
			if (l_list.isEmpty()) {
				missing_in_L_added_in_R = r_list.stream().collect(Collectors.toList());
				missing_in_R_added_in_L = Collections.emptyList();
				return;
			} else if (r_list.isEmpty()) {
				missing_in_R_added_in_L = l_list.stream().collect(Collectors.toList());
				missing_in_L_added_in_R = Collections.emptyList();
				return;
			}
			
			Map<Ltype, ComparingKeyType> key_by_ltypes = l_list.stream().collect(Collectors.toMap(l -> {
				return l;
			}, l_extractor));
			Map<ComparingKeyType, Ltype> ltype_by_keys = l_list.stream().collect(Collectors.toMap(l -> {
				return key_by_ltypes.get(l);
			}, l -> {
				return l;
			}));
			Map<Rtype, ComparingKeyType> key_by_rtypes = r_list.stream().collect(Collectors.toMap(r -> {
				return r;
			}, r_extractor));
			Map<ComparingKeyType, Rtype> rtype_by_keys = r_list.stream().collect(Collectors.toMap(r -> {
				return key_by_rtypes.get(r);
			}, r -> {
				return r;
			}));
			
			missing_in_L_added_in_R = r_list.stream().filter(r -> {
				return ltype_by_keys.containsKey(key_by_rtypes.get(r)) == false;
			}).collect(Collectors.toList());
			
			missing_in_R_added_in_L = l_list.stream().filter(l -> {
				return rtype_by_keys.containsKey(key_by_ltypes.get(l)) == false;
			}).collect(Collectors.toList());
		}
		
		public List<Rtype> getMissingInLAddedInR() {
			return missing_in_L_added_in_R;
		}
		
		public List<Ltype> getMissingInRAddedInL() {
			return missing_in_R_added_in_L;
		}
		
		public String toString() {
			return "missing_in_L_added_in_R: " + missing_in_L_added_in_R + ", missing_in_R_added_in_L: " + missing_in_R_added_in_L;
		}
		
		public boolean hasPositiveResults() {
			return missing_in_L_added_in_R.isEmpty() == false | missing_in_R_added_in_L.isEmpty() == false;
		}
	}
	
	public static void main(String[] args) throws MalformedURLException {
		List<URL> l_list = Arrays.asList(new URL("http://test1"), new URL("http://test2"), new URL("http://test3"), new URL("http://test5"));
		List<String> r_list = Arrays.asList("http://test1", "http://test2", "http://test3", "http://test4");
		
		TwoListComparing<URL, String, String> two = new TwoListComparing<>(l_list, r_list, l -> {
			return l.toString();
		}, r -> {
			return r;
		});
		
		System.out.println(two.process().toString());
	}
	
}
