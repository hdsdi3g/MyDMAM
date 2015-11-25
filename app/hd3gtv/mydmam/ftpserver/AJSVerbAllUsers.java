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
package hd3gtv.mydmam.ftpserver;

import java.util.Arrays;
import java.util.List;

import hd3gtv.mydmam.web.AsyncJSControllerVerb;
import hd3gtv.mydmam.web.AsyncJSGsonProvider;
import hd3gtv.mydmam.web.AsyncJSObjectVoid;
import hd3gtv.mydmam.web.AsyncJSSerializer;

public class AJSVerbAllUsers extends AsyncJSControllerVerb<AsyncJSObjectVoid, AJSResponseUserList> {
	
	public String getVerbName() {
		return "allusers";
	}
	
	public Class<AsyncJSObjectVoid> getRequestClass() {
		return AsyncJSObjectVoid.class;
	}
	
	public Class<AJSResponseUserList> getResponseClass() {
		return AJSResponseUserList.class;
	}
	
	public AJSResponseUserList onRequest(AsyncJSObjectVoid request) throws Exception {
		AJSResponseUserList ul = new AJSResponseUserList();
		ul.users = FTPUser.getAllAJSUsers();
		return ul;
	}
	
	public List<? extends AsyncJSSerializer<?>> getJsonSerializers(AsyncJSGsonProvider gson_provider) {
		return Arrays.asList(new AJSResponseUserList.Serializer());
	}
}
