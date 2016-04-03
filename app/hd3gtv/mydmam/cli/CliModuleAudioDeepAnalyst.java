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
import hd3gtv.tools.ApplicationArgs;
import hd3gtv.tools.CopyMove;
import hd3gtv.tools.ExecBinaryPath;
import hd3gtv.tools.Execprocess;
import hd3gtv.tools.ExecprocessEvent;

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
			params.add("ebur128=peak=true");
			params.add("-vn");
			params.add("-f");
			params.add("null");
			params.add("/dev/null");
			FFmpegDAEvents ffdae = new FFmpegDAEvents();
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
		
		TimeSeries series_momentary;
		TimeSeries series_short_term;
		TimeSeries series_integrated;
		TimeSeries series_true_peak_per_frame;
		
		public FFmpegDAEvents() throws Exception {
			series_momentary = new TimeSeries("Momentary");
			series_short_term = new TimeSeries("Short term");
			series_integrated = new TimeSeries("Integrated");
			series_true_peak_per_frame = new TimeSeries("True peak");
		}
		
		public void onEnd(int exitvalue, long execution_duration) {
			Loggers.Transcode.debug("End ffmpeg execution, after " + (double) execution_duration / 1000d + " sec");
			
			TimeSeriesCollection tsc = new TimeSeriesCollection();
			tsc.addSeries(series_integrated);
			tsc.addSeries(series_short_term);
			tsc.addSeries(series_momentary);
			tsc.addSeries(series_true_peak_per_frame);
			
			JFreeChart timechart = ChartFactory.createTimeSeriesChart("", "", "dBFS", tsc, true, false, false);
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
			// time_axis.setAxisLinePaint(Color.GRAY);
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
			ValueMarker zero_pos = new ValueMarker(-23);
			zero_pos.setLabel("-23");
			zero_pos.setLabelPaint(Color.CYAN);
			// zero_pos.setStroke(Stroke);
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
			rangeAxis.setRange(-80, 0);
			rangeAxis.setLabelPaint(Color.GRAY);
			rangeAxis.setAxisLineVisible(false);
			rangeAxis.setTickLabelPaint(Color.GRAY);
			// rangeAxis.setVerticalTickLabels(true);
			
			plot.setRangeAxis(rangeAxis);
			plot.setOutlinePaint(Color.GRAY);
			
			timechart.setTextAntiAlias(true);
			
			int width = 1000;
			int height = 600;
			try {
				File out_file = new File("TimeChart-5.jpeg");
				out_file.delete();
				ChartUtilities.saveChartAsJPEG(out_file, timechart, width, height);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void onStderr(String message) {
			// System.out.println(message);
			if (message.startsWith("[Parsed_ebur128_0 @ ") == false) {
				return;
			}
			if (message.endsWith(" Summary:")) {
				// TODO parse end summary results
				/*
				 * WARN : can't get this beacause if (message.startsWith("[Parsed_ebur128_0 @ ") == false) { return
				 * 
				[Parsed_ebur128_0 @ 0x7f9944800000] Summary:
				
				Integrated loudness:
				I:          -9.1 LUFS
				Threshold: -19.3 LUFS
				
				Loudness range:
				LRA:         3.9 LU
				Threshold: -29.3 LUFS
				LRA low:   -11.4 LUFS
				LRA high:   -7.4 LUFS
				
				True peak:
				Peak:        0.5 dBFS
				 * 
				 * 
				 * */
				return;
			}
			String[] line = message.substring(message.indexOf("]") + 1).trim().split(" ");
			/**
			 * [Parsed_ebur128_0 @ 0x7fb594000a00] t: 130.4 M: -6.9 S: -7.4 I: -9.2 LUFS LRA: 4.1 LU FTPK: -0.1 0.0 dBFS TPK: 0.3 0.4 dBFS
			 * Convert spaces to params.
			 */
			ArrayList<String> entries = new ArrayList<String>(20);
			for (int pos = 0; pos < line.length; pos++) {
				if (line[pos].equals("")) {
					/**
					 * Remove empty spaces
					 */
					continue;
				}
				entries.add(line[pos]);
			}
			/**
			 * [t:, 102.9, M:, -10.3, S:, -10.0, I:, -9.6, LUFS, LRA:, 4.6, LU, FTPK:, -3.9, -2.6, dBFS, TPK:, 0.3, 0.4, dBFS]
			 */
			float time = 0;
			Float momentary = 0f;
			Float short_term = 0f;
			Float integrated = 0f;
			Float true_peak_per_frame_L = 0f;
			Float true_peak_per_frame_R = 0f;
			
			String entry;
			for (int pos = 0; pos < entries.size(); pos++) {
				entry = entries.get(pos);
				if (entry.equalsIgnoreCase("t:")) {
					time = protectedParseFloat(entries.get(pos + 1));
				} else if (entry.equalsIgnoreCase("M:")) {
					momentary = protectedParseFloat(entries.get(pos + 1));
				} else if (entry.equalsIgnoreCase("S:")) {
					short_term = protectedParseFloat(entries.get(pos + 1));
				} else if (entry.equalsIgnoreCase("I:")) {
					integrated = protectedParseFloat(entries.get(pos + 1));
				} else if (entry.equalsIgnoreCase("FTPK:")) {
					true_peak_per_frame_L = protectedParseFloat(entries.get(pos + 1));
					true_peak_per_frame_R = protectedParseFloat(entries.get(pos + 2));
				} else {
					continue;
				}
			}
			
			System.out.print(time);
			System.out.print("\t\t");
			System.out.print(momentary);
			System.out.print("M\t");
			System.out.print(short_term);
			System.out.print("S\t");
			System.out.print(integrated);
			System.out.print("I\t");
			System.out.print(Math.max(true_peak_per_frame_L, true_peak_per_frame_R));
			System.out.print("FTPK");
			System.out.println();
			
			try {
				FixedMillisecond now = new FixedMillisecond(Math.round(Math.ceil(time * 1000f)));
				if (momentary == 0) {
					momentary = -100f;
				}
				series_momentary.add(now, momentary);
				
				if (short_term == 0) {
					short_term = -100f;
				}
				series_short_term.add(now, short_term);
				series_integrated.add(now, integrated);
				series_true_peak_per_frame.add(now, Math.max(true_peak_per_frame_L, true_peak_per_frame_R));
			} catch (SeriesException e) {
				e.printStackTrace();
				return;
			}
		}
		
	}
	
	/**
	 * @return MIN_VALUE if NaN
	 */
	public static float protectedParseFloat(String value) {
		try {
			return Float.parseFloat(value);
		} catch (NumberFormatException e) {
			return Float.MIN_VALUE;
		}
	}
	
}
