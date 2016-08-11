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
import java.io.IOException;
import java.util.List;

import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.tools.CopyMove;
import hd3gtv.tools.StoppableProcessing;

public abstract class ProcessingKitInstance {
	
	protected File temp_directory;
	protected JobProgression progression;
	protected StoppableProcessing stoppable;
	protected JobContextTranscoder transcode_context;
	protected File dest_base_directory;
	
	public ProcessingKitInstance(File temp_directory) throws NullPointerException, IOException {
		this.temp_directory = temp_directory;
		if (temp_directory == null) {
			throw new NullPointerException("\"temp_directory\" can't to be null");
		}
		CopyMove.checkExistsCanRead(temp_directory);
		CopyMove.checkIsDirectory(temp_directory);
		CopyMove.checkIsWritable(temp_directory);
	}
	
	public final void setJobProgression(JobProgression progression) {
		this.progression = progression;
	}
	
	public final void setStoppable(StoppableProcessing stoppable) {
		this.stoppable = stoppable;
	}
	
	public final void setTranscodeContext(JobContextTranscoder transcode_context) {
		this.transcode_context = transcode_context;
	}
	
	public final void setDestDirectory(File dest_base_directory) {
		this.dest_base_directory = dest_base_directory;
	}
	
	/**
	 * @return may be null.
	 */
	public abstract List<File> process(File physical_source, Container source_indexing_result) throws Exception;
	
	/**
	 * Always called after process(), even it failed.
	 * Don't touch to process() result list files.
	 */
	public abstract void cleanTempFiles();
	
}
