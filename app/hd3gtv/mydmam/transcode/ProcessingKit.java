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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.mydmam.transcode;

import java.io.File;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import hd3gtv.mydmam.manager.InstanceStatusItem;
import hd3gtv.mydmam.metadata.container.Container;

public interface ProcessingKit extends InstanceStatusItem {
	
	/**
	 * @return true if all ExecBinaryPath wanted are founded, if app context is ok, if some TranscodeProfiles are founded...
	 */
	public boolean isFunctionnal();
	
	public boolean validateItem(Container indexing_result);
	
	public String getDescription();
	
	public String getVendor();
	
	public String getVersion();
	
	/**
	 * Always do a validateItem() before think to createInstance() !
	 * @param temp_directory must exists.
	 */
	public ProcessingKitInstance createInstance(File temp_directory);
	
	default public String getReferenceKey() {
		return "pkitHcode:" + String.valueOf(getClass().getName().hashCode());
	}
	
	default public Class<?> getInstanceStatusItemReferenceClass() {
		return getClass();
	}
	
	default public JsonElement getInstanceStatusItem() {
		JsonObject jo = new JsonObject();
		jo.addProperty("class_name", getClass().getName());
		jo.addProperty("description", getDescription());
		jo.addProperty("vendor", getVendor());
		jo.addProperty("version", getVersion());
		return jo;
	}
	
}
