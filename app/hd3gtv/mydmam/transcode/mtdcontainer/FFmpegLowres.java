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

import hd3gtv.mydmam.metadata.container.Entry;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.metadata.container.SelfSerializing;

import java.util.List;

public class FFmpegLowres {
	
	public static class Lowres_lq extends EntryRenderer {
		public String getES_Type() {
			return "pvw_ffmpeg_lowres_lq";
		}
		
		protected Entry create() {
			return new Lowres_lq();
		}
		
		protected List<Class<? extends SelfSerializing>> getSerializationDependencies() {
			return null;
		}
	}
	
	public static class Lowres_sd extends EntryRenderer {
		public String getES_Type() {
			return "pvw_ffmpeg_lowres_sd";
		}
		
		protected Entry create() {
			return new Lowres_sd();
		}
		
		protected List<Class<? extends SelfSerializing>> getSerializationDependencies() {
			return null;
		}
	}
	
	public static class Lowres_hd extends EntryRenderer {
		public String getES_Type() {
			return "pvw_ffmpeg_lowres_hd";
		}
		
		protected Entry create() {
			return new Lowres_hd();
		}
		
		protected List<Class<? extends SelfSerializing>> getSerializationDependencies() {
			return null;
		}
	}
	
	public static class Lowres_audio extends EntryRenderer {
		public String getES_Type() {
			return "pvw_ffmpeg_lowres_audio";
		}
		
		protected Entry create() {
			return new Lowres_audio();
		}
		
		protected List<Class<? extends SelfSerializing>> getSerializationDependencies() {
			return null;
		}
	}
	
}
