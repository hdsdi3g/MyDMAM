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
package hd3gtv.mydmam.manager;

public class InstanceAction {
	/**
	 * TODO remote exec actions to workers (start, stop, change cyclic times...)
	 */
	
	private InstanceStatus instance;
	
	private String target;
	
	private String order;
	
	private String caller;
	
	// @see WorkerGroupEngine.setStatusChangesToWorkers
	// TODO change Log2Filter on the fly
}
