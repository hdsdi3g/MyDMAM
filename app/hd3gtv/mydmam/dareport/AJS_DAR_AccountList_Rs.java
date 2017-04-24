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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.dareport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

import hd3gtv.mydmam.auth.AuthTurret;
import hd3gtv.mydmam.auth.UserNG;

public class AJS_DAR_AccountList_Rs {
	
	ArrayList<DARAccount> list;
	HashMap<String, String> usernames;
	
	public void populate(AuthTurret turret) throws Exception {
		list = DARAccount.list();
		
		usernames = new HashMap<>(list.stream().map(account -> {
			return account.userkey;
		}).map(user_key -> {
			return turret.getByUserKey(user_key);
		}).filter(user -> {
			return user != null;
		}).collect(Collectors.toMap(user -> {
			return ((UserNG) user).getKey();
		}, user -> {
			return ((UserNG) user).getFullname();
		})));
		
	}
	
}
