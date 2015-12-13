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
package hd3gtv.mydmam.accesscontrol;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.accesscontrol.AccessControlAddresses.AccessControlAddressesStatus;

public class AccessControl {
	
	public static final int max_attempt_for_blocking_addr;
	public static final int grace_attempt_count;
	public static final int grace_period_factor_time;
	
	static {
		max_attempt_for_blocking_addr = Configuration.global.getValue("access_control", "max_attempt_for_blocking_addr", 20);
		grace_attempt_count = Configuration.global.getValue("access_control", "grace_attempt_count", 5);
		grace_period_factor_time = Configuration.global.getValue("access_control", "grace_period_factor_time", 60);
	}
	
	private AccessControl() {
	}
	
	public static boolean validThisIP(String address) {
		AccessControlAddressesStatus status = AccessControlAddresses.getAddrStatus(address);
		if (status == AccessControlAddressesStatus.NEVERBLOCK) {
			Loggers.AccessControl.trace("User try to login from a neverblock addr: " + address);
			return true;
		}
		if (status == AccessControlAddressesStatus.BLACK) {
			Loggers.AccessControl.info("User try to login from blacklisted addr: " + address);
			return false;
		}
		try {
			AccessControlEntry entry = AccessControlEntry.getFromAdress(address);
			
			if (entry == null) {
				/**
				 * No actual black list now.
				 */
				Loggers.AccessControl.trace("User try to login, and it's no blocked: " + address);
				return true;
			}
			
			if (entry.getAttempt() > max_attempt_for_blocking_addr) {
				Loggers.AccessControl.info("Too many attempt from this IP, \"user\" will get the fuck off, addr: " + address);
				return false;
			}
			
			if (entry.getAttempt() <= grace_attempt_count) {
				Loggers.AccessControl.debug("\"user\" can try login now, addr: " + address);
				return true;
			}
			
			if ((entry.getLastAttemptDate() + (long) ((entry.getAttempt() - grace_attempt_count) * grace_period_factor_time * 1000)) > (System.currentTimeMillis())) {
				Loggers.AccessControl.info("\"user\" must wait some time before retry, addr: " + address);
				return false;
			}
			
			Loggers.AccessControl.info("\"user\" has some attempts, It wait time, but now, it can try to login now, addr: " + address);
			
		} catch (ConnectionException e) {
			Loggers.AccessControl.warn("Can't connect to Cassandra, ignore AC check", e);
		}
		return true;
	}
	
	public static void releaseIP(String address) {
		try {
			AccessControlEntry entry = AccessControlEntry.getFromAdress(address);
			if (entry == null) {
				return;
			}
			Loggers.AccessControl.trace("Release IP, addr: " + address);
			entry.delete();
		} catch (ConnectionException e) {
			Loggers.AccessControl.warn("Can't connect to Cassandra", e);
		}
	}
	
	public static void failedAttempt(String address, String login_name) {
		try {
			AccessControlEntry entry = AccessControlEntry.getFromAdress(address);
			if (entry == null) {
				entry = AccessControlEntry.create(address, login_name);
			} else {
				entry.update(login_name);
			}
			Loggers.AccessControl.info("Failed attempt " + entry);
		} catch (ConnectionException e) {
			Loggers.AccessControl.warn("Can't connect to Cassandra", e);
		}
	}
	
}
