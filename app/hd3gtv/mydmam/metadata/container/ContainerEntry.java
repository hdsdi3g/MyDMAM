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
package hd3gtv.mydmam.metadata.container;

public abstract class ContainerEntry {
	
	ContainerEntry() {
	}
	
	ContainerOrigin origin;
	
	transient Container container;
	
	public final boolean hasOrigin() {
		return (origin != null);
	}
	
	public final ContainerOrigin getOrigin() {
		if (hasOrigin() == false) {
			throw new NullPointerException("Origin is not set because this Entry is not get from database");
		}
		return origin;
	}
	
	public abstract String getES_Type();
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("type: ");
		sb.append(this.getClass().getName());
		sb.append(", ES type: ");
		sb.append(getES_Type());
		sb.append(", origin: ");
		sb.append(origin);
		return sb.toString();
	}
	
	public final void setOrigin(ContainerOrigin origin) throws NullPointerException {
		if (origin == null) {
			throw new NullPointerException("\"origin\" can't to be null");
		}
		this.origin = origin;
	}
	
}
