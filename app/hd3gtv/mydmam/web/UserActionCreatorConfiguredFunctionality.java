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

import hd3gtv.mydmam.useraction.UAConfigurator;
import hd3gtv.mydmam.useraction.UAFunctionality;
import hd3gtv.mydmam.useraction.UAManager;

import com.google.gson.JsonElement;

public class UserActionCreatorConfiguredFunctionality {
	
	String functionality_classname;
	JsonElement raw_associated_user_configuration;
	
	transient UAConfigurator associated_user_configuration;
	transient UAFunctionality functionality;
	
	void prepare() throws NullPointerException {
		functionality = UAManager.getByName(functionality_classname);
		if (functionality == null) {
			throw new NullPointerException("Can't found functionality " + functionality_classname + ".");
		}
		associated_user_configuration = functionality.createEmptyConfiguration();
		if (associated_user_configuration != null) {
			associated_user_configuration.setObject(UserActionCreator.gson.fromJson(raw_associated_user_configuration, associated_user_configuration.getObjectClass()));
		}
	}
	
}
