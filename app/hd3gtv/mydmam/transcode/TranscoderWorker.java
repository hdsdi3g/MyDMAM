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
package hd3gtv.mydmam.transcode;

import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.manager.WorkerCapablities;
import hd3gtv.mydmam.manager.WorkerNG;
import hd3gtv.mydmam.storage.Storage;

import java.util.ArrayList;
import java.util.List;

public class TranscoderWorker extends WorkerNG {
	
	private List<WorkerCapablities> capabilities;
	
	public TranscoderWorker(final List<TranscodeProfile> profiles) {
		if (profiles == null) {
			throw new NullPointerException("\"profiles\" can't to be null");
		}
		if (profiles.isEmpty()) {
			throw new NullPointerException("\"profiles\" can't to be empty");
		}
		
		capabilities = new ArrayList<WorkerCapablities>(profiles.size());
		capabilities.add(new WorkerCapablities() {
			
			@Override
			public List<String> getStoragesAvaliable() {
				return Storage.getAllStoragesNames();
			}
			
			public Class<? extends JobContext> getJobContextClass() {
				return JobContextTranscoder.class;
			}
			
			public List<String> getHookedNames() {
				ArrayList<String> names = new ArrayList<String>();
				for (int pos = 0; pos < profiles.size(); pos++) {
					names.add(profiles.get(pos).getName());
				}
				return names;
			}
		});
	}
	
	public WorkerCategory getWorkerCategory() {
		return WorkerCategory.INTERNAL;
	}
	
	public String getWorkerLongName() {
		return "Media transcoding processor";
	}
	
	public String getWorkerVendorName() {
		return "MyDMAM Internal";
	}
	
	private boolean stop_process;
	
	protected synchronized void forceStopProcess() throws Exception {
		stop_process = true;
	}
	
	protected boolean isActivated() {
		return TranscodeProfile.isConfigured();
	}
	
	public List<WorkerCapablities> getWorkerCapablities() {
		return capabilities;
	}
	
	@Override
	protected void workerProcessJob(JobProgression progression, JobContext context) throws Exception {
		JobContextTranscoder transcode_context = (JobContextTranscoder) context;
		List<String> profiles_to_transcode = transcode_context.hookednames;
		
		// TODO get (download) original file if needed in temp directory.
		
		for (int pos = 0; pos < profiles_to_transcode.size(); pos++) {
			if (stop_process) {
				return;
			}
			TranscodeProfile transcode_profile = TranscodeProfile.getTranscodeProfile(profiles_to_transcode.get(pos));
			// TODO ...
		}
		
		if (stop_process) {
			return;
		}
	}
}
