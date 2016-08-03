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
import java.util.List;

import com.google.gson.JsonObject;

import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.tools.StoppableProcessing;

public interface ProcessingKitInstance {
	
	public void setJobProgression(JobProgression progression);
	
	public void setStoppable(StoppableProcessing stoppable);
	
	public void setOptions(JsonObject options);
	
	public void setDestDirectory(File dest_base_directory);
	
	public List<File> process(File physical_source, Container source_indexing_result) throws Exception;
	
	public void cleanTempFiles();
	
}
