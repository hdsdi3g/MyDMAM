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

import hd3gtv.mydmam.db.orm.CrudOrmModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Store all pending notifications actions
 */
public class NotificationUpdate extends CrudOrmModel {
	
	public boolean is_new_error;
	public boolean is_new_done;
	public boolean is_new_readed;
	public boolean is_new_closed;
	public boolean is_new_commented;
	
	public NotificationUpdate() throws Exception {
		is_new_error = false;
		is_new_done = false;
		is_new_readed = false;
		is_new_closed = false;
		is_new_commented = false;
	}
	
	boolean isNeedUpdate() {
		return is_new_error | is_new_done | is_new_readed | is_new_closed | is_new_commented;
	}
	
	List<NotifyReason> getReasons() {
		ArrayList<NotifyReason> reasons = new ArrayList<NotifyReason>();
		if (is_new_error) {
			reasons.add(NotifyReason.ERROR);
		}
		if (is_new_done) {
			reasons.add(NotifyReason.DONE);
		}
		if (is_new_readed) {
			reasons.add(NotifyReason.READED);
		}
		if (is_new_closed) {
			reasons.add(NotifyReason.CLOSED);
		}
		if (is_new_commented) {
			reasons.add(NotifyReason.COMMENTED);
		}
		return reasons;
	}
	
	protected String getCF_Name() {
		return "notification_update";
	}
	
	protected Class<? extends CrudOrmModel> getClassInstance() {
		return NotificationUpdate.class;
	}
	
	protected boolean hasLongGracePeriod() {
		return true;
	}
	
}
