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

import java.io.Serializable;
import java.util.List;

public interface AsyncJSRequestControllerVerb<Rq extends Serializable, Rp extends Serializable> {
	
	/**
	 * @return never null
	 */
	public String getVerbName();
	
	/**
	 * @return OR list, never null
	 */
	public List<String> getMandatoryPrivileges();
	
	/**
	 * @return never null
	 */
	public List<AsyncJSRequestDeserializer<?>> getJsonDeserializers(final AsyncJSRequestGsonProvider gson_provider);
	
	/**
	 * @return never null
	 */
	public List<AsyncJSRequestSerializer<?>> getJsonSerializers(final AsyncJSRequestGsonProvider gson_provider);
	
	/**
	 * @return never null
	 */
	public Class<Rq> getRequestClass();
	
	/**
	 * @return never null
	 */
	public Class<Rp> getResponseClass();
	
	/**
	 * @return never null
	 */
	public Rp onRequest(Rq request) throws Exception;
	
	/**
	 * @return never null
	 */
	public Rp failResponse();
	
}
