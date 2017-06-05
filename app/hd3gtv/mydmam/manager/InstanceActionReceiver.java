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
package hd3gtv.mydmam.manager;

import com.google.gson.JsonObject;

public interface InstanceActionReceiver extends InstanceStatusAction {
	
	/**
	 * Beware: use class.getSimpleName() to comparate. If 2 differents class has the same name, an error will be thrown.
	 */
	public Class<? extends InstanceActionReceiver> getClassToCallback();
	
	public void doAnAction(JsonObject order) throws Exception;
	
}
