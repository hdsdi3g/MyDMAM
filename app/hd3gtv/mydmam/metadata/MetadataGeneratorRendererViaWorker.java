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

import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.EntryRenderer;

import java.io.File;
import java.util.List;

public interface MetadataGeneratorRendererViaWorker extends MetadataGeneratorRenderer {
	
	/**
	 * Don't create jobs, just add callbacks to create new jobs to current_create_jobs_list.
	 * It will be executed after all metadatas analysing/rendering.
	 */
	void prepareJobs(final Container container, List<FutureCreateJobs> current_create_job_list) throws Exception;
	
	/**
	 * You NEED to consolidate rendered elements.
	 * Call RenderedFile.export_to_entry() for populate in EntryRenderer
	 */
	EntryRenderer standaloneProcess(File origin, JobProgression job_progress, Container container, JobContextMetadataRenderer renderer_context) throws Exception;
	
	void stopStandaloneProcess() throws Exception;
	
	Class<? extends JobContextMetadataRenderer> getContextClass();
	
}
