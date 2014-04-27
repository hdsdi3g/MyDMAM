/*
 * This file is part of MyDMAM
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.mail.notification;

import java.util.List;

import models.UserProfile;

public class Notification {
	
	/**
	 * About works
	 */
	private List<UserProfile> observers;
	private UserProfile creator;
	private List<String> linked_tasks_keys;
	private String creating_comment;
	
	/**
	 * About workflow
	 */
	private long created_at;
	private boolean is_read;
	private UserProfile first_reader;
	private long closed_at;
	private boolean is_close;
	private UserProfile closed_by;
	private long commented_at;
	private String users_comment;
	
	private long archived_after;
	
	/**
	 * About users callbacks
	 */
	private List<UserProfile> notify_if_error;
	private List<UserProfile> notify_if_done;
	private List<UserProfile> notify_if_readed;
	private List<UserProfile> notify_if_closed;
	private List<UserProfile> notify_if_commented;
	
}
