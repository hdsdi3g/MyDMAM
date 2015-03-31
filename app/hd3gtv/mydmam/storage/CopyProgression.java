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
package hd3gtv.mydmam.storage;

import hd3gtv.mydmam.manager.JobProgression;

public class CopyProgression {
	
	public CopyProgression(FileExistsPolicy fileexistspolicy, boolean delete_after_copy, JobProgression progression) {
		this.fileexistspolicy = fileexistspolicy;
		if (fileexistspolicy == null) {
			throw new NullPointerException("\"fileexistspolicy\" can't to be null");
		}
		this.delete_after_copy = delete_after_copy;
		this.progression = progression;
		if (progression == null) {
			throw new NullPointerException("\"progression\" can't to be null");
		}
	}
	
	public enum FileExistsPolicy {
		OVERWRITE, IGNORE, RENAME
	}
	
	private JobProgression progression;
	private boolean delete_after_copy;
	private FileExistsPolicy fileexistspolicy;
	
	private long total_size = 0;
	private long progress_copy = 0;
	private int actual_progress_value = 0;
	private int last_progress_value = 0;
	private int progress_size = 0;
	
}
