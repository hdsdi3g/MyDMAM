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
package models;

import hd3gtv.configuration.Configuration;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import play.data.validation.Required;
import play.db.jpa.GenericModel;

@Entity
public class BlackListIP extends GenericModel {
	
	@Id
	@Required
	public String address;
	
	@Required
	public Date first_attempt;
	
	@Required
	public Date last_attempt;
	
	@Required
	public String last_attempt_username;
	
	@Required
	public int attempt;
	
	public BlackListIP(String address, String attempt_username) {
		this.address = address;
		this.last_attempt_username = attempt_username;
		first_attempt = new Date();
		attempt = 0;
	}
	
	private static final int max_attempt_for_blocking_addr = Configuration.global.getValue("auth", "max_attempt_for_blocking_addr", 10);
	
	private static final int grace_attempt_count = Configuration.global.getValue("auth", "grace_attempt_count", 2);
	
	private static final int grace_period_factor_time = Configuration.global.getValue("auth", "grace_period_factor_time", 10);
	
	public static boolean validThisIP(String address) {
		BlackListIP bl = BlackListIP.findById(address);
		
		if (bl == null) {
			/**
			 * No actual black list now.
			 */
			return true;
		}
		
		if (bl.attempt > max_attempt_for_blocking_addr) {
			/**
			 * Too many attempt from this IP, "user" will get the fuck off.
			 */
			return false;
		}
		
		if (bl.attempt <= grace_attempt_count) {
			/**
			 * "user" can try login now.
			 */
			return true;
		}
		
		if ((bl.last_attempt.getTime() + (long) ((bl.attempt - grace_attempt_count) * grace_period_factor_time * 1000)) > (System.currentTimeMillis())) {
			/**
			 * "user" must wait some time before retry.
			 */
			return false;
		}
		
		/**
		 * "user" has some attempts, It wait time, but now, it can try to login now.
		 */
		return true;
	}
	
	public static void releaseIP(String address) {
		BlackListIP bl = BlackListIP.findById(address);
		if (bl == null) {
			return;
		}
		bl.delete();
	}
	
	public static void failedAttempt(String address, String login_name) {
		BlackListIP bl = BlackListIP.findById(address);
		if (bl == null) {
			bl = new BlackListIP(address, login_name);
		}
		bl.attempt++;
		bl.last_attempt = new Date();
		bl.save();
	}
}
