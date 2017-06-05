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
package hd3gtv.mydmam.transcode.mtdgenerator;

import java.io.IOException;
import java.util.List;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.metadata.ContainerEntryResult;
import hd3gtv.mydmam.metadata.MetadataExtractor;
import hd3gtv.mydmam.metadata.PreviewType;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.transcode.mtdcontainer.BBCBmx;
import hd3gtv.tools.ExecBinaryPath;
import hd3gtv.tools.ExecprocessBadExecutionException;
import hd3gtv.tools.StoppableProcessing;

public class BBCBmxAnalyser implements MetadataExtractor {
	
	public boolean isEnabled() {
		try {
			ExecBinaryPath.get("mxf2raw");
			return true;
		} catch (Exception e) {
		}
		return false;
	}
	
	public ContainerEntryResult processFast(Container container) throws Exception {
		return processFull(container, null);
	}
	
	public ContainerEntryResult processFull(Container container, StoppableProcessing stoppable) throws Exception {
		BBCBmx result = new BBCBmx();
		
		try {
			result.analystFile(container.getPhysicalSource());
		} catch (IOException e) {
			if (e instanceof ExecprocessBadExecutionException) {
				Loggers.Transcode_Metadata.error("Problem with mxf2raw (BBC BMX) on item " + container);
			}
			throw e;
		}
		
		container.getSummary().putSummaryContent(result, result.processSummary());
		return new ContainerEntryResult(result);
	}
	
	public String getLongName() {
		return "BBC BMX (MXF) Tools";
	}
	
	public static boolean canProcessThisVideoOnly(String mimetype) {
		if (mimetype.equalsIgnoreCase("application/mxf")) return true;
		return false;
	}
	
	public static boolean canProcessThisAudioOnly(String mimetype) {
		if (mimetype.equalsIgnoreCase("application/mxf")) return true;
		return false;
	}
	
	public boolean canProcessThisMimeType(String mimetype) {
		if (canProcessThisVideoOnly(mimetype)) return true;
		if (canProcessThisAudioOnly(mimetype)) return true;
		return false;
	}
	
	public List<String> getMimeFileListCanUsedInMasterAsPreview() {
		return null;
	}
	
	public boolean isCanUsedInMasterAsPreview(Container container) {
		return false;
	}
	
	public PreviewType getPreviewTypeForRenderer(Container container, EntryRenderer entry) {
		return null;
	}
	
	public boolean isTheExtractionWasActuallyDoes(Container container) {
		return container.containAnyMatchContainerEntryType(BBCBmx.ES_TYPE);
	}
	
}
