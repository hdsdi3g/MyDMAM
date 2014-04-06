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
package hd3gtv.mydmam.metadata.rendering;

import hd3gtv.mydmam.metadata.indexing.MetadataIndexerResult;
import hd3gtv.mydmam.taskqueue.Job;

import java.io.File;
import java.util.List;

import org.json.simple.JSONObject;

public interface RendererViaWorker extends Renderer {
	
	/**
	 * Don't create tasks, just add callbacks to create new tasks to current_create_task_list.
	 * It will be executed after all metadatas analysing/rendering.
	 */
	void prepareTasks(final MetadataIndexerResult analysis_result, List<FuturePrepareTask> current_create_task_list) throws Exception;
	
	/**
	 * You don't need to consolidate rendered elements
	 */
	List<RenderedElement> standaloneProcess(File origin, Job job, JSONObject renderer_context) throws Exception;
	
	void stopStandaloneProcess() throws Exception;
	
}
