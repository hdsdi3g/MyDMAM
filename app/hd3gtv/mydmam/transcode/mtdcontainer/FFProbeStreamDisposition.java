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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.mydmam.transcode.mtdcontainer;

import com.google.gson.annotations.SerializedName;

public final class FFProbeStreamDisposition {
	
	public int clean_effects;
	public int karaoke;
	
	@SerializedName("default")
	public int default_;
	
	public int hearing_impaired;
	public int original;
	public int attached_pic;
	public int lyrics;
	public int comment;
	public int dub;
	public int visual_impaired;
	public int forced;
	public int timed_thumbnails;
	
}
