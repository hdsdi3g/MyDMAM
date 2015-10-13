/*
 * This file is part of Javamail Wrapper
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
 * Copyright (C) hdsdi3g for hd3g.tv 2008-2014
 * 
*/

package hd3gtv.mydmam.mail;

import javax.mail.Message;
import javax.mail.MessagingException;

import hd3gtv.mydmam.Loggers;

public enum MailPriority {
	LOWEST, NORMAL, HIGHEST;
	
	Message updateMessage(Message message) {
		try {
			if (this == HIGHEST) {
				message.setHeader("X-PRIORITY", "1 (Highest)");
				message.setHeader("Priority", "urgent");
			} else if (this == LOWEST) {
				message.setHeader("X-PRIORITY", "5 (Lowest)");
				message.setHeader("Priority", "non-urgent");
			}
			return message;
		} catch (MessagingException e) {
			Loggers.Mail.error("Can't update mail priority", e);
		}
		return null;
		
	}
}
