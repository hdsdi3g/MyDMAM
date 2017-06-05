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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.tools;

/**
 * Inspired by http://stackoverflow.com/questions/23308193/how-to-break-or-return-from-java8-lambda-foreach
 */
public class BreakReturnException extends RuntimeException {
	
	private Object result;
	
	public BreakReturnException(Object result) {
		this.result = result;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> type) {
		if (result == null) {
			return null;
		}
		return (T) result;
	}
}
