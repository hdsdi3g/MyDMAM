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
import hd3gtv.tools.VideoConst.Resolution;

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
		
		if (ffprobe.hasVideo() == false) {
			return;
		}
		
		/**
		 * Interlacing management, like
		 * -vf scale=720:576:interl=1,setfield=tff,fieldorder=bff,setfield=bff,setdar=dar=16/9
		 * or
		 * -vf scale=720:576,yadif=0:1:0,setdar=dar=16/9
		 * AND
		 * Overlay insertion, like
		 * -i <> -filter_complex '[0:0]yadif=0:1:0[2:0];[1:0]scale=1920x1080,[2:0]overlay'
		 * or
		 * -i <> -filter_complex '[0:0]scale=720:576:interl=1,setfield=tff,fieldorder=bff,setfield=bff,setdar=dar=16/9[2:0];[1:0]scale=1920x1080,[2:0]overlay'
		 */
		FFmpegInterlacingStats source_interlacing_stats = source_container.getByClass(FFmpegInterlacingStats.class);
		Interlacing input_interlacing = null;
		if (source_interlacing_stats != null) {
			input_interlacing = source_interlacing_stats.getInterlacing();
		} else {
			input_interlacing = Interlacing.Unknow;
		}
		
		String source_resolution = ffprobe.getVideoResolution().x + "x" + ffprobe.getVideoResolution().y;
		
		File overlay_file = current_profile_output_format.getOverlay();
		
		Interlacing output_interlacing = current_profile_output_format.getInterlacing();
		if (input_interlacing != Interlacing.Unknow && output_interlacing != Interlacing.Unknow && current_profile_output_format.getResolution().x > 0) {
			StringBuilder vf_filters_chain = new StringBuilder();
			
			if (overlay_file != null) {
				vf_filters_chain.append("[0:" + ffprobe.getStreamsByCodecType("video").get(0).getIndex() + "]");
			}
			
			/**
			 * Scaling with interlacing conservation
			 */
			String output_resolution = null;
			if (current_profile_output_format.getResolution() == null) {
				if (current_params.contains("-s")) {
					output_resolution = current_params.get(current_params.indexOf("-s") + 1);
				}
			} else {
				source_resolution = current_profile_output_format.getResolution().x + "x" + current_profile_output_format.getResolution().y;
			}
			
			if (output_resolution != null) {
				if (output_resolution.equals(source_resolution) == false) {
					
					boolean source_vbi = ffprobe.hasVerticalBlankIntervalInImage();
					boolean dest_vbi = Resolution.parseResolution(output_resolution).isVerticalBlankIntervalResolution();
					if (source_vbi && dest_vbi == false) {
						/**
						 * Cut the 32 lines from the top.
						 */
						vf_filters_chain.append("crop=w=in_w:h=in_h-32:x=0:y=32,");
					}
					
					/**
					 * Source will be scaled
					 */
					vf_filters_chain.append("scale=");
					vf_filters_chain.append(output_resolution);
					
					if (input_interlacing != Interlacing.Progressive && output_interlacing != Interlacing.Progressive) {
						/**
						 * Interlaced -> Interlaced
						 */
						vf_filters_chain.append(":interl=1");
					}
					vf_filters_chain.append(",");
				}
			}
			
			/**
			 * Invert frame interlacing order or deinterlace
			 */
			if (input_interlacing == Interlacing.TopFieldFirst && output_interlacing == Interlacing.BottomFieldFirst) {
				/**
				 * TopFieldFirst -> BottomFieldFirst
				 */
				vf_filters_chain.append("setfield=tff,fieldorder=bff,setfield=bff,");
			} else if (input_interlacing == Interlacing.BottomFieldFirst && output_interlacing == Interlacing.TopFieldFirst) {
				/**
				 * BottomFieldFirst -> TopFieldFirst
				 */
				vf_filters_chain.append("setfield=bff,fieldorder=tff,setfield=tff,");
			} else if (input_interlacing == Interlacing.TopFieldFirst && output_interlacing == Interlacing.Progressive) {
				/**
				 * TopFieldFirst -> Progressive (de-interlaced)
				 */
				vf_filters_chain.append("yadif=0:0:0,");// XXX if out fps is > in fps, deinterlace to field
			} else if (input_interlacing == Interlacing.BottomFieldFirst && output_interlacing == Interlacing.Progressive) {
				/**
				 * BottomFieldFirst -> Progressive (de-interlaced)
				 */
				vf_filters_chain.append("yadif=0:1:0,");// XXX if out fps is > in fps, deinterlace to field
			}
			
			/**
			 * Set correct aspect ratio
			 */
			if (current_params.contains("-aspect")) {
				vf_filters_chain.append("setdar=dar=");
				vf_filters_chain.append(current_params.get(current_params.indexOf("-aspect") + 1).replaceAll(":", "/"));
				vf_filters_chain.append(",");
			} else {
				try {
					String source_dar = ffprobe.getStreamsByCodecType("video").get(0).getParam("display_aspect_ratio").getAsString();
					vf_filters_chain.append("setdar=dar=");
					vf_filters_chain.append(source_dar.replaceAll(":", "/"));
					vf_filters_chain.append(",");
				} catch (NullPointerException e) {
				}
			}
			
			if (overlay_file != null) {
				if (vf_filters_chain.toString().endsWith(",")) {
					vf_filters_chain.setLength(vf_filters_chain.length() - 1);
				}
				vf_filters_chain.append("[2:0];[1:0]scale=" + source_resolution + ",[2:0]overlay");
			}
			
			if (vf_filters_chain.length() > 0) {
				insert_pos = current_params.lastIndexOf("-i") + 1;
				
				if (overlay_file != null) {
					current_params.add(insert_pos++, "-i");
					current_params.add(insert_pos++, overlay_file.getAbsolutePath());
					current_params.add(insert_pos++, "-filter_complex");
				} else {
					current_params.add(insert_pos++, "-vf");
				}
				
				/**
				 * remove the last ","
				 */
				if (vf_filters_chain.toString().endsWith(",")) {
					current_params.add(insert_pos++, vf_filters_chain.toString().substring(1));
				} else {
					current_params.add(insert_pos++, vf_filters_chain.toString());
				}
			}
		} else if (overlay_file != null) {
			/**
			 * Insert logo without interlacing management, like
			 * -filter_complex [1:0]scale=1280x720,[0:0]overlay
			 */
			insert_pos = current_params.lastIndexOf("-i") + 1;
			current_params.add(insert_pos++, "-i");
			current_params.add(insert_pos++, overlay_file.getAbsolutePath());
			current_params.add(insert_pos++, "-filter_complex");
			StringBuilder vf_filters_chain = new StringBuilder();
			vf_filters_chain.append("[1:0]scale=");
			vf_filters_chain.append(source_resolution);
			vf_filters_chain.append(",[2:0]overlay");
			current_params.add(insert_pos++, vf_filters_chain.toString());
		}
		
	}
	
}
