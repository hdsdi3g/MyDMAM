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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.web;

import java.util.Collections;
import java.util.List;

import hd3gtv.mydmam.Loggers;

public abstract class AsyncJSControllerVerb<Rq extends AsyncJSRequestObject, Rp extends AsyncJSResponseObject> {
	
	/**
	 * @return never null
	 */
	public abstract String getVerbName();
	
	/**
	 * @return OR list, never null
	 */
	public List<String> getMandatoryPrivileges() {
		return Collections.emptyList();
	}
	
	/**
	 * @return never null
	 */
	public List<? extends AsyncJSDeserializer<?>> getJsonDeserializers(final AsyncJSGsonProvider gson_provider) {
		return Collections.emptyList();
	}
	
	/**
	 * @return never null
	 */
	public List<? extends AsyncJSSerializer<?>> getJsonSerializers(final AsyncJSGsonProvider gson_provider) {
		return Collections.emptyList();
	}
	
	/**
	 * @return never null
	 */
	public abstract Class<Rq> getRequestClass();
	
	/**
	 * @return never null
	 */
	public abstract Class<Rp> getResponseClass();
	
	/**
	 * @return never null
	 */
	public abstract Rp onRequest(Rq request) throws Exception;
	
	/**
	 * @return never null
	 */
	public Rp failResponse() {
		try {
			return getResponseClass().newInstance();
		} catch (Exception e) {
			Loggers.Play.error("Can't to create an empty response, class: " + getResponseClass().getName(), e);
			return null;
		}
	}
	
}
