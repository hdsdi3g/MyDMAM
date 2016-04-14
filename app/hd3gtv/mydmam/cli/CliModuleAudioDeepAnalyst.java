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
package hd3gtv.mydmam.cli;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TimeZone;

import org.apache.commons.io.FileUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.SeriesException;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.transcode.mtdcontainer.FFmpegAudioDeepAnalyst;
import hd3gtv.mydmam.transcode.mtdcontainer.FFmpegAudioDeepAnalystChannelStat;
import hd3gtv.mydmam.transcode.mtdcontainer.FFmpegAudioDeepAnalystSilenceDetect;
import hd3gtv.tools.ApplicationArgs;
import hd3gtv.tools.CopyMove;
import hd3gtv.tools.ExecBinaryPath;
import hd3gtv.tools.Execprocess;
import hd3gtv.tools.ExecprocessEvent;

/**
 * @deprecated
 */
public class CliModuleAudioDeepAnalyst implements CliModule {
	
	public String getCliModuleName() {
		return "audioda";
	}
	
	public String getCliModuleShortDescr() {
		return "Audio deep analyst and stats computing";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		if (args.getParamExist("-i")) {
			File input_file = new File(args.getSimpleParamValue("-i"));
			CopyMove.checkExistsCanRead(input_file);
			ArrayList<String> params = new ArrayList<String>();
			params.add("-nostats");
			params.add("-i");
			params.add(input_file.getAbsolutePath());
			params.add("-filter_complex");
			params.add("ebur128=peak=true,astats,silencedetect=n=-20dB:d=3");
			params.add("-vn");
			params.add("-f");
			params.add("null");
			params.add("/dev/null");
			FFmpegDAEvents ffdae = new FFmpegDAEvents(1000, 600, -80f, -23f, new File("TimeChart.jpeg"), 0.8f);
			Execprocess process = new Execprocess(ExecBinaryPath.get("ffmpeg"), params, ffdae);
			
			Thread t = new Thread() {
				public void run() {
					try {
						process.kill();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			t.setName("Shutdown Hook");
			Runtime.getRuntime().addShutdownHook(t);
			
			process.run();
			
			ffdae.saveLUFSGraphic();
			
			// System.out.println("RESULT :\t" + ffdae.ffmpeg_da_result.toString());
			
		} else {
			showFullCliModuleHelp();
		}
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage: ");
		System.out.println(" " + getCliModuleName() + " -i <audio or video source file>");
	}
	
	public boolean isFunctionnal() {
		try {
			ExecBinaryPath.get("ffprobe");
			ExecBinaryPath.get("ffmpeg");
			return true;
		} catch (Exception e) {
			Loggers.CLI.debug("Can't found ffmpeg and ffprobe: the module " + getCliModuleName() + " will not functionnal.");
			return false;
		}
	}
	
	static {
		System.setProperty("java.awt.headless", "true");
	}
	
	private class FFmpegDAEvents implements ExecprocessEvent {
		
		public void onError(IOException ioe) {
			Loggers.Transcode.error("FFmpeg error", ioe);
		}
		
		public void onError(InterruptedException ie) {
			Loggers.Transcode.error("FFmpeg threads error", ie);
		}
		
		public void onStdout(String message) {
			System.out.println(message);
		}
		
		public void onStart(String commandline, File working_directory) {
			if (working_directory != null) {
				Loggers.Transcode.info("start ffmpeg: " + commandline + "\t in " + working_directory);
			} else {
				Loggers.Transcode.info("start ffmpeg: " + commandline);
			}
		}
		
		public void onKill(long execution_duration) {
			Loggers.Transcode.debug("FFmpeg is killed, after " + (double) execution_duration / 1000d + " sec");
		}
		
		private TimeSeries series_momentary;
		private TimeSeries series_short_term;
		private TimeSeries series_integrated;
		private TimeSeries series_true_peak_per_frame;
		private float lufs_range;
		private int image_width;
		private int image_height;
		private float lufs_ref;
		private File output_file;
		private float jpg_compression_ratio;
		private FFmpegAudioDeepAnalyst ffmpeg_da_result;
		
		public FFmpegDAEvents(int image_width, int image_height, float lufs_depth, float lufs_ref, File output_file, float jpg_compression_ratio) throws Exception {
			this.lufs_range = lufs_depth;
			this.image_width = image_width;
			this.image_height = image_height;
			this.lufs_ref = lufs_ref;
			this.output_file = output_file;
			if (output_file == null) {
				throw new NullPointerException("\"output_file\" can't to be null");
			}
			if (output_file.exists() & output_file.isFile()) {
				FileUtils.forceDelete(output_file);
			}
			this.jpg_compression_ratio = jpg_compression_ratio;
			ffmpeg_da_result = new FFmpegAudioDeepAnalyst();
			
			series_momentary = new TimeSeries("Momentary");
			series_short_term = new TimeSeries("Short term");
			series_integrated = new TimeSeries("Integrated");
			series_true_peak_per_frame = new TimeSeries("True peak");
		}
		
		private void saveLUFSGraphic() throws IOException {
			TimeSeriesCollection tsc = new TimeSeriesCollection();
			tsc.addSeries(series_integrated);
			tsc.addSeries(series_short_term);
			tsc.addSeries(series_momentary);
			tsc.addSeries(series_true_peak_per_frame);
			
			JFreeChart timechart = ChartFactory.createTimeSeriesChart("", "", "", tsc, true, false, false);
			timechart.setAntiAlias(true);
			timechart.setBackgroundPaint(Color.black);
			timechart.getLegend().setBackgroundPaint(Color.black);
			
			XYPlot plot = timechart.getXYPlot();
			
			plot.setBackgroundPaint(Color.black);
			
			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer(0);
			renderer.setBaseLegendTextPaint(Color.GRAY);
			
			/**
			 * series_integrated
			 */
			renderer.setSeriesPaint(0, Color.BLUE);
			BasicStroke thick_stroke = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
			renderer.setSeriesStroke(0, thick_stroke);
			
			/**
			 * series_short_term
			 */
			renderer.setSeriesPaint(1, Color.ORANGE);
			/**
			 * series_momentary
			 */
			renderer.setSeriesPaint(2, Color.RED);
			/**
			 * series_true_peak_per_frame
			 */
			renderer.setSeriesPaint(3, Color.DARK_GRAY);
			
			/**
			 * Time units
			 */
			DateAxis time_axis = (DateAxis) plot.getDomainAxis();
			time_axis.setAxisLineVisible(false);
			time_axis.setLabelPaint(Color.GRAY);
			time_axis.setTickLabelPaint(Color.GRAY);
			time_axis.setLowerMargin(0);
			time_axis.setUpperMargin(0);
			time_axis.setTimeZone(TimeZone.getTimeZone("GMT"));
			time_axis.setMinorTickMarksVisible(true);
			time_axis.setMinorTickCount(10);
			time_axis.setMinorTickMarkInsideLength(5);
			time_axis.setMinorTickMarkOutsideLength(0);
			
			/**
			 * Display the -23 line
			 */
			ValueMarker zero_pos = new ValueMarker(lufs_ref);
			zero_pos.setLabel(String.valueOf(lufs_ref));
			zero_pos.setLabelPaint(Color.CYAN);
			zero_pos.setAlpha(0.5f);
			zero_pos.setLabelBackgroundColor(Color.CYAN);
			zero_pos.setPaint(Color.CYAN);
			zero_pos.setOutlinePaint(Color.CYAN);
			
			float dash[] = { 5.0f };
			BasicStroke dash_stroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
			zero_pos.setStroke(dash_stroke);
			plot.addRangeMarker(zero_pos);
			
			LogarithmicAxis rangeAxis = new LogarithmicAxis("dB LU");
			rangeAxis.centerRange(0d);
			rangeAxis.setAllowNegativesFlag(true);
			rangeAxis.setRange(lufs_range, 0);
			rangeAxis.setLabelPaint(Color.GRAY);
			rangeAxis.setAxisLineVisible(false);
			rangeAxis.setTickLabelPaint(Color.GRAY);
			
			plot.setRangeAxis(rangeAxis);
			plot.setOutlinePaint(Color.GRAY);
			
			timechart.setTextAntiAlias(true);
			
			ChartUtilities.saveChartAsJPEG(output_file, jpg_compression_ratio, timechart, image_width, image_height);
		}
		
		public void onEnd(int exitvalue, long execution_duration) {
			Loggers.Transcode.debug("End ffmpeg execution, after " + (double) execution_duration / 1000d + " sec");
		}
		
		/**
		 * Internal var for ffmpeg return analyst string
		 */
		private String[] splited_line;
		private ArrayList<String> line_entries = new ArrayList<String>(20);
		private Float entry_time = 0f;
		private Float entry_momentary = 0f;
		private Float entry_short_term = 0f;
		private Float entry_integrated = 0f;
		private Float entry_true_peak_per_frame_L = 0f;
		private Float entry_true_peak_per_frame_R = 0f;
		private boolean ebur128_summary_block = false;
		private FFmpegAudioDeepAnalystChannelStat current_astat;
		private FFmpegAudioDeepAnalystSilenceDetect current_silence;
		
		public void onStderr(String message) {
			if (ebur128_summary_block) {
				parseEbur128Summary(message);
				return;
			}
			
			if (message.startsWith("[Parsed_ebur128_0 @ ")) {
				if (message.endsWith(" Summary:")) {
					ebur128_summary_block = true;
				} else {
					parseEbur128Line(message);
				}
			} else if (message.startsWith("[Parsed_astats_1 @ ")) {
				parseAstatStatment(message);
			} else if (message.startsWith("[silencedetect @ ")) {
				parseSilencedetectStatment(message);
			} /*else {
				System.out.println(message);
				}*/
		}
		
		/**
		 * @param message "[silencedetect @ 0x7f9532700680] silence_start: 447.441" or "[silencedetect @ 0x7f9532700680] silence_end: 451.041 | silence_duration: 3.6"
		 */
		private void parseSilencedetectStatment(String message) {
			message = message.trim();
			splited_line = message.split("]");
			message = splited_line[1].trim();
			
			if (message.startsWith("silence_start")) {
				if (current_silence != null) {
					/**
					 * Still in silence : start + start but no end for the first start.
					 */
					return;
				}
				current_silence = new FFmpegAudioDeepAnalystSilenceDetect();
				current_silence.parseFromFFmpegLine(message);
			} else if (message.startsWith("silence_end")) {
				if (current_silence == null) {
					/**
					 * Not in silence : end with not the start.
					 */
					return;
				}
				current_silence.parseFromFFmpegLine(message);
				if (ffmpeg_da_result.silences == null) {
					ffmpeg_da_result.silences = new ArrayList<FFmpegAudioDeepAnalystSilenceDetect>();
				}
				ffmpeg_da_result.silences.add(current_silence);
				current_silence = null;
			}
			
		}
		
		/**
		 * @param message "[Parsed_astats_1 @ 0x7fb930422f80] Channel: 1" or "[Parsed_astats_1 @ 0x7fb930422f80] Max difference: 0.873830"
		 */
		private void parseAstatStatment(String message) {
			message = message.trim();
			splited_line = message.split("]");
			message = splited_line[1].trim();
			
			if (message.startsWith("Channel:")) {
				if (current_astat != null) {
					if (ffmpeg_da_result.channels_stat == null) {
						ffmpeg_da_result.channels_stat = new ArrayList<FFmpegAudioDeepAnalystChannelStat>(2);
					}
					ffmpeg_da_result.channels_stat.add(current_astat);
				}
				
				current_astat = new FFmpegAudioDeepAnalystChannelStat();
				current_astat.parseFromFFmpegLine(message);
			} else if (message.equals("Overall")) {
				if (current_astat != null) {
					if (ffmpeg_da_result.channels_stat == null) {
						ffmpeg_da_result.channels_stat = new ArrayList<FFmpegAudioDeepAnalystChannelStat>(2);
					}
					ffmpeg_da_result.channels_stat.add(current_astat);
				}
				
				current_astat = new FFmpegAudioDeepAnalystChannelStat();
				current_astat.channel = -1;
			} else if (message.startsWith("Number of samples")) {
				ffmpeg_da_result.number_of_samples = Long.parseLong(message.split(":")[1].trim());
				ffmpeg_da_result.overall_stat = current_astat;
			} else {
				if (current_astat == null) {
					Loggers.Metadata.warn("Bad ffmpeg return during AudioDeepAnalysis and astats, a value is presented without declare the current audio channel: " + message);
				}
				current_astat.parseFromFFmpegLine(message);
			}
		}
		
		private boolean ebur128_summary_block_loudness_range = false;
		
		private void parseEbur128Summary(String message) {
			message = message.trim();
			if (message.equals("")) {
				return;
			}
			
			splited_line = message.split(":");
			
			/*
			 * Parse this :
				Integrated loudness:
				I:         -17.5 LUFS
				Threshold: -27.5 LUFS
				
				Loudness range:
				LRA:        20.7 LU
				Threshold: -37.2 LUFS
				LRA low:   -37.2 LUFS
				LRA high:  -16.5 LUFS
				
				True peak:
				Peak:       -0.2 dBFS
			 * 
			 */
			if (splited_line[0].equals("Integrated loudness")) {
				ebur128_summary_block_loudness_range = false;
			} else if (splited_line[0].equals("I")) {
				ffmpeg_da_result.integrated_loudness = protectedParseFloat(splited_line[1].trim().split(" ")[0]);
			} else if (splited_line[0].equals("Threshold")) {
				if (ebur128_summary_block_loudness_range) {
					ffmpeg_da_result.loudness_range_threshold = protectedParseFloat(splited_line[1].trim().split(" ")[0]);
				} else {
					ffmpeg_da_result.integrated_loudness_threshold = protectedParseFloat(splited_line[1].trim().split(" ")[0]);
				}
			} else if (splited_line[0].equals("Loudness range")) {
				ebur128_summary_block_loudness_range = true;
			} else if (splited_line[0].equals("LRA")) {
				ffmpeg_da_result.loudness_range_LRA = protectedParseFloat(splited_line[1].trim().split(" ")[0]);
			} else if (splited_line[0].equals("LRA low")) {
				ffmpeg_da_result.loudness_range_LRA_low = protectedParseFloat(splited_line[1].trim().split(" ")[0]);
			} else if (splited_line[0].equals("LRA high")) {
				ffmpeg_da_result.loudness_range_LRA_high = protectedParseFloat(splited_line[1].trim().split(" ")[0]);
			} else if (splited_line[0].equals("Peak")) {
				ffmpeg_da_result.true_peak = protectedParseFloat(splited_line[1].trim().split(" ")[0]);
				ebur128_summary_block = false;
			}
		}
		
		private void parseEbur128Line(String message) {
			splited_line = message.substring(message.indexOf("]") + 1).trim().split(" ");
			/**
			 * [Parsed_ebur128_0 @ 0x7fb594000a00] t: 130.4 M: -6.9 S: -7.4 I: -9.2 LUFS LRA: 4.1 LU FTPK: -0.1 0.0 dBFS TPK: 0.3 0.4 dBFS
			 * Convert spaces to params.
			 */
			line_entries.clear();
			for (int pos = 0; pos < splited_line.length; pos++) {
				if (splited_line[pos].equals("")) {
					/**
					 * Remove empty spaces
					 */
					continue;
				}
				line_entries.add(splited_line[pos]);
			}
			/**
			 * [t:, 102.9, M:, -10.3, S:, -10.0, I:, -9.6, LUFS, LRA:, 4.6, LU, FTPK:, -3.9, -2.6, dBFS, TPK:, 0.3, 0.4, dBFS]
			 */
			entry_time = 0f;
			entry_momentary = 0f;
			entry_short_term = 0f;
			entry_integrated = 0f;
			entry_true_peak_per_frame_L = 0f;
			entry_true_peak_per_frame_R = 0f;
			
			String entry;
			for (int pos = 0; pos < line_entries.size(); pos++) {
				entry = line_entries.get(pos);
				if (entry.equalsIgnoreCase("t:")) {
					entry_time = protectedParseFloat(line_entries.get(pos + 1));
				} else if (entry.equalsIgnoreCase("M:")) {
					entry_momentary = protectedParseFloat(line_entries.get(pos + 1));
				} else if (entry.equalsIgnoreCase("S:")) {
					entry_short_term = protectedParseFloat(line_entries.get(pos + 1));
				} else if (entry.equalsIgnoreCase("I:")) {
					entry_integrated = protectedParseFloat(line_entries.get(pos + 1));
				} else if (entry.equalsIgnoreCase("FTPK:")) {
					entry_true_peak_per_frame_L = protectedParseFloat(line_entries.get(pos + 1));
					entry_true_peak_per_frame_R = protectedParseFloat(line_entries.get(pos + 2));
				} else {
					continue;
				}
			}
			
			/*System.out.print(time);
			System.out.print("\t\t");
			System.out.print(momentary);
			System.out.print("M\t");
			System.out.print(short_term);
			System.out.print("S\t");
			System.out.print(integrated);
			System.out.print("I\t");
			System.out.print(Math.max(true_peak_per_frame_L, true_peak_per_frame_R));
			System.out.print("FTPK");
			System.out.println();*/
			
			try {
				FixedMillisecond now = new FixedMillisecond(Math.round(Math.ceil(entry_time * 1000f)));
				if (entry_momentary == 0) {
					entry_momentary = protectedParseFloat(null);
				}
				series_momentary.add(now, entry_momentary);
				
				if (entry_short_term == 0) {
					entry_short_term = protectedParseFloat(null);
				}
				series_short_term.add(now, entry_short_term);
				series_integrated.add(now, entry_integrated);
				series_true_peak_per_frame.add(now, Math.max(entry_true_peak_per_frame_L, entry_true_peak_per_frame_R));
			} catch (SeriesException e) {
				e.printStackTrace();
				return;
			}
		}
		
	}
	
	/**
	 * @return -144 if NaN
	 */
	public static float protectedParseFloat(String value) {
		try {
			return Float.parseFloat(value);
		} catch (NullPointerException e) {
			return -144.49f;
		} catch (NumberFormatException e) {
			return -144.49f;
		}
	}
	
}
