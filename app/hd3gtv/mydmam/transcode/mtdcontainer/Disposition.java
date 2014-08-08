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

public class Disposition {
	
	int clean_effects;
	int karaoke;
	
	@SerializedName("default")
	int default_;
	
	int hearing_impaired;
	int original;
	int attached_pic;
	int lyrics;
	int comment;
	int dub;
	int visual_impaired;
	int forced;
}
