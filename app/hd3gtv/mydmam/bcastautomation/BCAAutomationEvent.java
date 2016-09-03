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
package hd3gtv.mydmam.bcastautomation;

import com.google.gson.JsonObject;

import hd3gtv.tools.Timecode;

public interface BCAAutomationEvent {
	
	long getStartDate();
	
	String getName();
	
	String getAutomationId();
	
	String getFileId();
	
	boolean isRecording();
	
	String getVideoSource();
	
	Timecode getDuration();
	
	boolean isAutomationPaused();
	
	Timecode getSOM();
	
	String getComment();
	
	String getChannel();
	
	JsonObject getOtherProperties();
	
	String getMaterialType();
	
}
