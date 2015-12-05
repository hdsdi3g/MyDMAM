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

public class AJSVerbRecentActivities {// TODO refactoring
	
	public String getVerbName() {
		return "recentactivities";
	}
	
	public Class<AJSRequestRecent> getRequestClass() {
		return AJSRequestRecent.class;
	}
	
	public Class<AJSResponseActivities> getResponseClass() {
		return AJSResponseActivities.class;
	}
	
	public AJSResponseActivities onRequest(AJSRequestRecent request, String caller) throws Exception {
		AJSResponseActivities response = new AJSResponseActivities();
		response.activities = FTPActivity.getRecentActivities(request.user_id, request.last_time);
		return response;
	}
	
	/*public List<? extends AsyncJSSerializer<?>> getJsonSerializers(AsyncJSGsonProvider gson_provider) {// TODO ADD to AJS
		return Arrays.asList(new AJSResponseActivities.Serializer());
	}*/
	
}
