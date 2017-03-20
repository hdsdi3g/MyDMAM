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
package controllers.ajs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import controllers.Check;
import hd3gtv.archivecircleapi.ACFile;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.pathindexing.AJSFileLocationStatus;
import hd3gtv.mydmam.pathindexing.AJSFileLocationStatusRequest;
import hd3gtv.mydmam.web.AJSController;
import hd3gtv.mydmam.web.stat.AsyncMetadataAnalystRequest;
import hd3gtv.mydmam.web.stat.AsyncStatRequest;
import hd3gtv.mydmam.web.stat.AsyncStatResult;
import hd3gtv.mydmam.web.stat.PathElementStat;

public class Stat extends AJSController {
	
	@Check("navigate")
	public static AsyncStatResult cache(AsyncStatRequest request) {
		return new PathElementStat(request).getResult();
	}
	
	@Check("navigate")
	public static JsonObject metadataAnalystResults(AsyncMetadataAnalystRequest request) {
		if (request == null) {
			return null;
		} else if (request.pathelementkey == null) {
			return null;
		} else if (request.mtype == null) {
			return null;
		}
		JsonObject result = ContainerOperations.getRawByMtdKeyForOnlyOneTypeAndCheckedToBeSendedToWebclients(request.pathelementkey, request.mtype);
		if (result == null) {
			return null;
		}
		if (result.has("origin")) {
			result.remove("origin");
		}
		return result;
	}
	
	@Check("navigate")
	public static AJSFileLocationStatus getExternalLocation(AJSFileLocationStatusRequest request) {
		AJSFileLocationStatus response = new AJSFileLocationStatus();
		ACFile ac_file = MyDMAM.getPlayBootstrapper().getBridgePathindexArchivelocation().getExternalLocation(request.storagename, request.path);
		if (ac_file != null) {
			if (Loggers.Play.isTraceEnabled()) {
				Loggers.Play.trace("Get file location for " + request.storagename + ":" + request.path + " -> " + ac_file.bestLocation);
			}
			
			response.getFromACAPI(ac_file);
		} else {
			Loggers.Play.debug("Null return for file location for " + request.storagename + ":" + request.path);
		}
		return response;
	}
	
	@Check("navigate")
	public static JsonArray getExternalLocationStorageList() {
		return MyDMAM.gson_kit.getGsonSimple().toJsonTree(MyDMAM.getPlayBootstrapper().getBridgePathindexArchivelocation().getExternalLocationStorageList()).getAsJsonArray();
	}
	
}
