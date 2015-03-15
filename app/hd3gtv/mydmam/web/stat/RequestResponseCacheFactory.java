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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.web.stat;

import java.util.HashMap;
import java.util.List;

public interface RequestResponseCacheFactory<T> {
	
	/**
	 * @return never null
	 */
	HashMap<String, T> makeValues(List<String> cache_reference_tags) throws Exception;
	
	/**
	 * @return never null
	 */
	String serializeThis(T item) throws Exception;
	
	/**
	 * @return never null
	 */
	T deserializeThis(String value) throws Exception;
	
	boolean hasExpired(T item);
	
	String getLocaleCategoryName();
}
