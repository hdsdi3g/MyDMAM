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
import java.util.ArrayList;
import java.util.List;

import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.transcode.TranscodeProfile.OutputFormat;
import hd3gtv.mydmam.transcode.mtdcontainer.FFmpegInterlacingStats;
import hd3gtv.mydmam.transcode.mtdcontainer.FFprobe;
import hd3gtv.mydmam.transcode.mtdcontainer.Stream;
import hd3gtv.tools.VideoConst.AudioRoutingStream;
import hd3gtv.tools.VideoConst.Interlacing;

public class CommandLineModifierFFmpeg implements CommandLineModifier {
	
	public void modify(ArrayList<String> current_params, OutputFormat current_profile_output_format, Container source_container) {
		int insert_pos = current_params.lastIndexOf("-i") + 1;
		
		if (insert_pos == 1) {
			return;
		}
		
		FFprobe ffprobe = source_container.getByClass(FFprobe.class);
		if (ffprobe == null) {
			return;
		}
		
		/**
		 * Audio management
		 */
		ArrayList<AudioRoutingStream> audio_destination_map = current_profile_output_format.getAudioMap();
		if (audio_destination_map != null && ffprobe.hasAudio()) {
			List<Stream> source_audio_streams = ffprobe.getStreamsByCodecType("audio");
			
			/**
			 * Add to map video stream
			 */
			if (ffprobe.hasVideo() && current_params.contains("-vn") == false) {
				current_params.add(insert_pos++, "-map");
				current_params.add(insert_pos++, "[0:" + ffprobe.getStreamsByCodecType("video").get(0).getIndex() + "]");
			}
			
			/**
			 * Split all multichannels (stereo, quad, 5.1...) to separate streams.
			 * Like -filter_complex [0:1]channelsplit=channel_layout=2c[abssrc1][abssrc2] ...
			 */
			int source_stream_index;
			int source_stream_channels_count;
			int source_stream_channels_absolute_pos = 1;
			StringBuilder sb;
			ArrayList<String> avaliable_streams = new ArrayList<String>();
			
			for (Stream stream : source_audio_streams) {
				source_stream_index = stream.getIndex();
				source_stream_channels_count = stream.getParam("channels").getAsInt();
				
				sb = new StringBuilder();
				for (int pos = 0; pos < source_stream_channels_count; pos++) {
					sb.append("[abssrc");
					sb.append(pos + source_stream_channels_absolute_pos);
					sb.append("]");
				}
				current_params.add(insert_pos++, "-filter_complex");
				current_params.add(insert_pos++, "[0:" + source_stream_index + "]channelsplit=channel_layout=" + source_stream_channels_count + "c" + sb.toString());
				
				source_stream_channels_absolute_pos += source_stream_channels_count;
				avaliable_streams.add(sb.toString());
			}
			
			/**
			 * Merge splited channels.
			 * Like -filter_complex '[abssrc1][abssrc2]amerge=inputs=2[adest12]' -filter_complex '[abssrc3][abssrc4]amerge=inputs=2[adest34]'
			 */
			String map;
			AudioRoutingStream audio_destination_group;
			for (int pos_adm = 0; pos_adm < audio_destination_map.size(); pos_adm++) {
				audio_destination_group = audio_destination_map.get(pos_adm);
				if (audio_destination_group.source_channel.size() > 1) {
					/**
					 * Add multiple channels to one stream.
					 */
					current_params.add(insert_pos++, "-filter_complex");
					
					sb = new StringBuilder();
					for (Integer channel : audio_destination_group.source_channel) {
						sb.append("[abssrc");
						sb.append(channel);
						sb.append("]");
						avaliable_streams.remove("[abssrc" + channel + "]");
					}
					map = "[adest" + pos_adm + "]";
					current_params.add(insert_pos++, sb.toString() + "amerge=inputs=" + audio_destination_group.source_channel.size() + map);
					
					current_params.add(insert_pos++, "-map");
					current_params.add(insert_pos++, map);
				} else {
					/**
					 * Add simple (mono) channel stream.
					 */
					map = "[abssrc" + audio_destination_group.source_channel.get(0) + "]";
					current_params.add(insert_pos++, "-map");
					current_params.add(insert_pos++, map);
					avaliable_streams.remove(map);
				}
				
			}
			
			/**
			 * Add to end the rejected source channels.
			 * Like -map "0:0" -map "[abssrc2]" -f mov test.mp4 -map "[abssrc1]" -f null -
			 */
			for (String stream : avaliable_streams) {
				current_params.add("-map");
				current_params.add(stream);
				current_params.add("-f");
				current_params.add("null");
				current_params.add("-");
			}
		}
		
		/**
		 * Interlacing management
		 */
		FFmpegInterlacingStats source_interlacing_stats = source_container.getByClass(FFmpegInterlacingStats.class);
		Interlacing input_interlacing = null;
		if (source_interlacing_stats != null) {
			input_interlacing = source_interlacing_stats.getInterlacing();
		} else {
			input_interlacing = Interlacing.Unknow;
		}
		
		Interlacing output_interlacing = current_profile_output_format.getInterlacing();
		if (output_interlacing != Interlacing.Unknow && input_interlacing != Interlacing.Unknow) {
			// TODO
		}
		
		/**
		 * Overlay insertion
		 */
		File overlay_file = current_profile_output_format.getOverlay();
		if (overlay_file != null) {
			// TODO
		}
	}
	
}
