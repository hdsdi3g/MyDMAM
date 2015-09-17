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

import hd3gtv.tools.Timecode;

public interface ProgressForJobContextFFmpegBased {
	
	public void setPerformance_fps(float performance_fps);
	
	public void setFrame(int frame);
	
	public void setDupFrame(int dup_frames);
	
	public void setDropFrame(int drop_frames);
	
	public Timecode getSourceDuration();
	
}
