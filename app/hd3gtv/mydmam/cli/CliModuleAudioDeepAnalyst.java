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

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;

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
		
		private RrdDb rrdDb;
		private long start;
		private long end;
		
		public FFmpegDAEvents() throws Exception {
			(new File("test.rrd")).delete();
			
			RrdDef rrdDef = new RrdDef("test.rrd", 300);
			rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 1, 30);
			rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 2, 60);
			rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 10, 60);
			rrdDef.addArchive(ConsolFun.AVERAGE, 0.5, 100, 60);
			rrdDef.addArchive(ConsolFun.MAX, 0.5, 1, 60);
			rrdDef.addDatasource("momentary", DsType.GAUGE, 10, 0, 200);
			// rrdDef.addDatasource("outbytes", DsType.GAUGE, 600, 0, Double.NaN);
			start = System.currentTimeMillis();
			rrdDb = new RrdDb(rrdDef);
		}
		
		public void onEnd(int exitvalue, long execution_duration) {
			Loggers.Transcode.debug("End ffmpeg execution, after " + (double) execution_duration / 1000d + " sec");
			try {
				rrdDb.close();
				(new File("test.png")).delete();
				
				// then create a graph definition
				RrdGraphDef gDef = new RrdGraphDef();
				gDef.setWidth(800);
				gDef.setHeight(400);
				gDef.setFilename("test.png");
				gDef.setStartTime(start);
				gDef.setEndTime(end);
				gDef.setTitle("My Title");
				gDef.setVerticalLabel("db ABS");
				
				gDef.datasource("momentary", "test.rrd", "momentary", ConsolFun.AVERAGE);
				gDef.hrule(50, Color.GREEN, "hrule");
				gDef.setImageFormat("png");
				
				new RrdGraph(gDef);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void onStderr(String message) {
			System.out.println(message);
			if (message.startsWith("[Parsed_ebur128_0 @ ") == false) {
				return;
			}
			if (message.endsWith(" Summary:")) {
				// TODO parse end summary results
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
			float momentary = 0;
			float short_term = 0;
			float integrated = 0;
			float loudness_range = 0;
			float true_peak_per_frame_L = 0;
			float true_peak_per_frame_R = 0;
			float true_peak_L = 0;
			float true_peak_R = 0;
			
			String entry;
			for (int pos = 0; pos < entries.size(); pos++) {
				entry = entries.get(pos);
				if (entry.equalsIgnoreCase("t:")) {
					time = Float.parseFloat(entries.get(pos + 1));
				} else if (entry.equalsIgnoreCase("M:")) {
					momentary = Float.parseFloat(entries.get(pos + 1));
				} else if (entry.equalsIgnoreCase("S:")) {
					short_term = Float.parseFloat(entries.get(pos + 1));
				} else if (entry.equalsIgnoreCase("I:")) {
					integrated = Float.parseFloat(entries.get(pos + 1));
				} else if (entry.equalsIgnoreCase("LRA:")) {
					loudness_range = Float.parseFloat(entries.get(pos + 1));
				} else if (entry.equalsIgnoreCase("FTPK:")) {
					true_peak_per_frame_L = Float.parseFloat(entries.get(pos + 1));
					true_peak_per_frame_R = Float.parseFloat(entries.get(pos + 2));
				} else if (entry.equalsIgnoreCase("TPK:")) {
					true_peak_L = Float.parseFloat(entries.get(pos + 1));
					true_peak_R = Float.parseFloat(entries.get(pos + 2));
				} else {
					continue;
				}
			}
			
			// System.out.println(entries);
			/*System.out.print(time);
			System.out.print(" ");
			System.out.print(momentary);
			System.out.print(" ");
			System.out.print(short_term);
			System.out.print(" ");
			System.out.print(integrated);
			System.out.print(" ");
			System.out.print(loudness_range);
			System.out.print(" ");
			System.out.print(true_peak_per_frame_L);
			System.out.print(" ");
			System.out.print(true_peak_per_frame_R);
			System.out.print(" ");
			System.out.print(true_peak_L);
			System.out.print(" ");
			System.out.print(true_peak_R);
			System.out.println();*/
			
			Sample sample;
			end = start + Math.round(time * 1000);
			try {
				sample = rrdDb.createSample();
				sample.setTime(end);
				sample.setValue("momentary", Math.abs(momentary));// XXX
				sample.update();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
