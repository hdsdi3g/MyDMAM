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
 * Copyright (C) hdsdi3g for hd3g.tv 4 sept. 2017
 * 
*/
package hd3gtv.mydmam.embddb;

import java.util.UUID;

import hd3gtv.mydmam.Loggers;

public class AlreadyBusyLock extends Exception {
	
	/**
	 * Never null
	 */
	final String target_id;
	/**
	 * Never null
	 */
	final long expiration_date;
	/**
	 * Never null
	 */
	final String owner;
	
	final UUID locker_node;
	
	AlreadyBusyLock(String target_id, long expiration_date, String owner, UUID locker_node) {
		this.target_id = target_id;
		if (target_id == null) {
			throw new NullPointerException("\"target_id\" can't to be null");
		}
		this.expiration_date = expiration_date;
		if (expiration_date == 0) {
			throw new NullPointerException("\"expiration_date\" can't == 0");
		}
		this.owner = owner;
		if (owner == null) {
			throw new NullPointerException("\"owner\" can't to be null");
		}
		this.locker_node = locker_node;
		if (locker_node == null) {
			throw new NullPointerException("\"locker_node\" can't to be null");
		}
	}
	
	public String getMessage() {
		return target_id + " was lock by " + owner + " until the " + Loggers.dateLog(expiration_date);
	}
	
}
