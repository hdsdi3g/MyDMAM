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
import hd3gtv.mydmam.manager.JobProgressor;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.tools.CopyMove;
import hd3gtv.tools.StoppableProcessing;

public abstract class ProcessingKitInstance implements StoppableProcessing {
	
	protected File temp_directory;
	
	protected final Progress progress;
	private JobProgression progression;
	private StoppableProcessing stoppable;
	
	/**
	 * maybe null
	 */
	protected JobContextTranscoder transcode_context;
	
	/**
	 * maybe null
	 */
	protected File dest_base_directory;
	
	public ProcessingKitInstance(File temp_directory) throws NullPointerException, IOException {
		this.temp_directory = temp_directory;
		if (temp_directory == null) {
			throw new NullPointerException("\"temp_directory\" can't to be null");
		}
		CopyMove.checkExistsCanRead(temp_directory);
		CopyMove.checkIsDirectory(temp_directory);
		CopyMove.checkIsWritable(temp_directory);
		
		progress = new Progress();
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
	public abstract void cleanTempFiles() throws Exception;
	
	/**
	 * Overload this for catch error before thrown it to Worker, and can clean temp files before ends job
	 */
	public void onProcessException(File physical_source, Container source_indexing_result, Exception e) throws Exception {
	}
	
	/**
	 * @return never null, but maybe without action...
	 */
	protected Progress getProgress() {
		return progress;
	}
	
	public boolean isWantToStopCurrentProcessing() {
		if (stoppable != null) {
			return stoppable.isWantToStopCurrentProcessing();
		}
		
		return false;
	}
	
	protected class Progress implements JobProgressor {
		private Progress() {
		}
		
		public void update(String last_message) {
			if (progression != null) {
				progression.update(last_message);
			}
		}
		
		public void updateStep(int step, int step_count) {
			if (progression != null) {
				progression.updateStep(step, step_count);
			}
		}
		
		public void incrStep() {
			if (progression != null) {
				progression.incrStep();
			}
		}
		
		public void incrStepCount() {
			if (progression != null) {
				progression.incrStepCount();
			}
		}
		
		public void updateProgress(int progress, int progress_size) {
			if (progression != null) {
				progression.updateProgress(progress, progress_size);
			}
		}
		
		/**
		 * @return maybe empty, never null.
		 */
		public String getJobKey() {
			if (progression != null) {
				return progression.getJobKey();
			}
			return "";
		}
		
	}
	
}
