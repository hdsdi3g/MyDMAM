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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.metadata;

import java.util.List;

import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.tools.StoppableProcessing;

public interface MetadataExtractor {
	
	boolean canProcessThisMimeType(String mimetype);
	
	boolean isEnabled();
	
	String getLongName();
	
	/**
	 * @return can be null.
	 */
	List<String> getMimeFileListCanUsedInMasterAsPreview();
	
	/**
	 * Should return false for Renderer only Extractors.
	 */
	boolean isCanUsedInMasterAsPreview(Container container);
	
	/**
	 * Used only for speedup short Metadata Analyst/Rendering operations.
	 * -
	 * For rendering result, you NEED to consolidate rendered elements.
	 * Call RenderedFile.export_to_entry() for populate in EntryRenderer.
	 * @return null if no result or not avaliable or coherent.
	 */
	ContainerEntryResult processFast(Container container) throws Exception;
	
	/**
	 * Used if no need to speedup Metadata Analyst/Rendering operations.
	 * Don't forget to add processFast() operations !
	 * -
	 * For rendering result, you NEED to consolidate rendered elements.
	 * Call RenderedFile.export_to_entry() for populate in EntryRenderer.
	 * @return null if no result.
	 */
	ContainerEntryResult processFull(Container container, StoppableProcessing stoppable) throws Exception;
	
	/**
	 * @return JS side parser name for display this render, or null.
	 *         You can return null for Analyst only.
	 */
	PreviewType getPreviewTypeForRenderer(Container container, EntryRenderer entry);
	
}
