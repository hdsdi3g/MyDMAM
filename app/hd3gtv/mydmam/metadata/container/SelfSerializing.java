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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.mydmam.metadata.container;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Don't forget to declare this in Operations.
 */
public interface SelfSerializing {
	
	/**
	 * @param item from create()
	 */
	SelfSerializing deserialize(JsonObject source, Gson gson);
	
	/**
	 * @param item must be like the same type like create()
	 */
	JsonObject serialize(SelfSerializing _item, Gson gson);
	
}
