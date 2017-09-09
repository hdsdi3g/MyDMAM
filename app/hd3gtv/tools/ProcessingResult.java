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
 * Copyright (C) hdsdi3g for hd3g.tv 9 sept. 2017
 * 
*/
package hd3gtv.tools;

public final class ProcessingResult<T, R> {
	
	public final T source;
	public final R result;
	public final Throwable error;
	
	ProcessingResult(T source, R result) {
		this.source = source;
		this.result = result;
		this.error = null;
	}
	
	ProcessingResult(T source, Throwable error) {
		this.source = source;
		this.result = null;
		this.error = error;
	}
	
}
