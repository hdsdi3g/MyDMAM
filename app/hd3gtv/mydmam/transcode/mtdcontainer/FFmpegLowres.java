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

import hd3gtv.mydmam.metadata.container.EntryRenderer;

public class FFmpegLowres {
	
	public static class Lowres_lq extends EntryRenderer {
		public String getES_Type() {
			return "pvw_ffmpeg_lowres_lq";
		}
		
	}
	
	public static class Lowres_sd extends EntryRenderer {
		public String getES_Type() {
			return "pvw_ffmpeg_lowres_sd";
		}
		
	}
	
	public static class Lowres_hd extends EntryRenderer {
		public String getES_Type() {
			return "pvw_ffmpeg_lowres_hd";
		}
		
	}
	
	public static class Lowres_audio extends EntryRenderer {
		public String getES_Type() {
			return "pvw_ffmpeg_lowres_audio";
		}
		
	}
	
}
