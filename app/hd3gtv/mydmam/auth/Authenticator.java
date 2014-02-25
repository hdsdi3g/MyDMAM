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

package hd3gtv.mydmam.auth;

import hd3gtv.log2.Log2Dumpable;

import java.io.IOException;

public interface Authenticator extends Log2Dumpable {
	
	/**
	 * @return null if user & password are invalid, unknow, lock...
	 */
	public AuthenticationUser getUser(String username, String password) throws NullPointerException, IOException, InvalidAuthenticatorUserException;
	
}
