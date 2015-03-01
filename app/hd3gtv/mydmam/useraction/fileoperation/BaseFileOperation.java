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
package hd3gtv.mydmam.useraction.fileoperation;

import hd3gtv.mydmam.useraction.UAFunctionalityContext;
import hd3gtv.mydmam.useraction.UAFunctionalitySection;
import hd3gtv.mydmam.useraction.UAJobProcess;
import hd3gtv.tools.StoppableProcessing;

abstract class BaseFileOperation extends UAFunctionalityContext implements UAJobProcess, StoppableProcessing {
	
	public final UAFunctionalitySection getSection() {
		return UAFunctionalitySection.filesystem;
	}
	
	public final String getVendor() {
		return "Internal MyDMAM";
	}
	
	public final String getLongName() {
		return "Filesystem operation User Action: " + getSubLongName();
	}
	
	protected abstract String getSubLongName();
	
	public final String getMessageBaseName() {
		return "uafileoperation." + getSubMessageBaseName();
	}
	
	protected abstract String getSubMessageBaseName();
	
	protected boolean stop;
	
	public synchronized void forceStopProcess() throws Exception {
		stop = true;
	}
	
	public synchronized boolean isWantToStopCurrentProcessing() {
		return stop;
	}
	
}
