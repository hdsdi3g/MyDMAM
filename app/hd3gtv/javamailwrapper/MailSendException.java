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
 * Copyright (C) hdsdi3g for hd3g.tv 2008-2013
 * 
*/

package hd3gtv.javamailwrapper;

import java.io.IOException;

public class MailSendException extends IOException {
	
	private static final long serialVersionUID = -5726536708334429232L;
	
	public MailSendException() {
		super();
	}
	
	public MailSendException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public MailSendException(String message) {
		super(message);
	}
	
	public MailSendException(Throwable cause) {
		super(cause);
	}
	
}
