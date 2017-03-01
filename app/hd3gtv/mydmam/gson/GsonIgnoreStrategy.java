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
package hd3gtv.mydmam.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

public class GsonIgnoreStrategy implements ExclusionStrategy {
	
	static final boolean DEBUG = false;
	
	public boolean shouldSkipField(FieldAttributes f) {
		if (DEBUG) {
			System.out.println(f.getName());
			System.out.println(f.getAnnotation(GsonIgnore.class));
		}
		return f.getAnnotation(GsonIgnore.class) != null;
	}
	
	public boolean shouldSkipClass(Class<?> clazz) {
		if (DEBUG) {
			System.out.println(clazz.getName());
		}
		return clazz.getAnnotation(GsonIgnore.class) != null;
	}
	
}
