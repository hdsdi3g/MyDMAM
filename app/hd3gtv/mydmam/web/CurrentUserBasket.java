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
package hd3gtv.mydmam.web;

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.operation.Basket;

import java.util.ArrayList;

import models.UserProfile;

import com.google.gson.Gson;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import controllers.Secure;

public class CurrentUserBasket {
	
	/**
	 * @return JSON Array
	 */
	public static String getBasket() {
		String user_key = UserProfile.prepareKey(Secure.connected());
		Basket basket = new Basket(user_key);
		
		Gson g = new Gson();
		try {
			return g.toJson(basket.getContent());
		} catch (ConnectionException e) {
			Log2.log.error("Can't access to Cassandra DB", e);
			return "[]";
		}
	}
	
	public static void setBasket(String[] content) {
		if (content == null) {
			return;
		}
		if (content.length == 0) {
			return;
		}
		
		String user_key = UserProfile.prepareKey(Secure.connected());
		Basket basket = new Basket(user_key);
		
		ArrayList<String> al_content = new ArrayList<String>(content.length);
		for (int pos = 0; pos < content.length; pos++) {
			al_content.add(content[pos]);
		}
		try {
			basket.setContent(al_content);
		} catch (ConnectionException e) {
			Log2.log.error("Can't access to Cassandra DB", e);
		}
	}
	
}
