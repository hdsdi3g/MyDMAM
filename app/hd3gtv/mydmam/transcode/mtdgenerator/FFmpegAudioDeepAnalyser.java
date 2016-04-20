package hd3gtv.mydmam.transcode.mtdgenerator;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.metadata.ContainerEntryResult;
import hd3gtv.mydmam.metadata.MetadataExtractor;
import hd3gtv.mydmam.metadata.PreviewType;
import hd3gtv.mydmam.metadata.RenderedFile;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.ContainerEntry;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.transcode.mtdcontainer.FFmpegAudioDeepAnalyst;
import hd3gtv.mydmam.transcode.mtdcontainer.FFmpegAudioDeepAnalystChannelStat;
import hd3gtv.mydmam.transcode.mtdcontainer.FFmpegAudioDeepAnalystSilenceDetect;
import hd3gtv.mydmam.transcode.mtdcontainer.FFprobe;
import hd3gtv.tools.CopyMove;
import hd3gtv.tools.ExecBinaryPath;
import hd3gtv.tools.Execprocess;
import hd3gtv.tools.ExecprocessEvent;

public class FFmpegAudioDeepAnalyser implements MetadataExtractor {
	
	private int silencedetect_level_threshold;
	private int silencedetect_min_duration;
	
	private int image_width;
	private int image_height;
	private float lufs_depth;
	private float lufs_ref;
	private float truepeak_ref;
	private float jpg_compression_ratio;
	
	public FFmpegAudioDeepAnalyser() {
		silencedetect_level_threshold = -Math.abs(Configuration.global.getValue("metadata_analysing", "ffmpeg_audioda_silencedetect_level_threshold", -60));
		
		silencedetect_min_duration = Configuration.global.getValue("metadata_analysing", "ffmpeg_audioda_silencedetect_min_duration", 3);
		image_width = Configuration.global.getValue("metadata_analysing", "ffmpeg_audioda_image_width", 1000);
		image_height = Configuration.global.getValue("metadata_analysing", "ffmpeg_audioda_image_height", 600);
		lufs_depth = -Math.abs((float) Configuration.global.getValue("metadata_analysing", "ffmpeg_audioda_lufs_depth", -80f));
		lufs_ref = -Math.abs((float) Configuration.global.getValue("metadata_analysing", "ffmpeg_audioda_lufs_ref", -23f));
		truepeak_ref = -Math.abs((float) Configuration.global.getValue("metadata_analysing", "ffmpeg_audioda_truepeak_ref", -3f));
		jpg_compression_ratio = (float) Configuration.global.getValue("metadata_analysing", "ffmpeg_audioda_jpg_compression_ratio", 0.8f);
	}
	
	public boolean isEnabled() {
		try {
			ExecBinaryPath.get("ffprobe");
			ExecBinaryPath.get("ffmpeg");
			return true;
		} catch (Exception e) {
		}
		return false;
	}
	
	public boolean canProcessThisMimeType(String mimetype) {
		if (FFprobeAnalyser.canProcessThisAudioOnly(mimetype) | FFprobeAnalyser.canProcessThisVideoOnly(mimetype)) {
			return true;
		}
		return false;
	}
	
	public String getLongName() {
		return "Audio deep analyst and stats computing via ffmpeg";
	}
	
	public List<Class<? extends ContainerEntry>> getAllRootEntryClasses() {
		return Arrays.asList(FFmpegAudioDeepAnalyst.class, AudioDeepAnalystGraphic.class);
	}
	
	public List<String> getMimeFileListCanUsedInMasterAsPreview() {
		return null;
	}
	
	public boolean isCanUsedInMasterAsPreview(Container container) {
		return false;
	}
	
	public ContainerEntryResult processFast(Container container) throws Exception {
		return null;
	}
	
	public PreviewType getPreviewTypeForRenderer(Container container, EntryRenderer entry) {
		return PreviewType.audio_graphic_deepanalyst;
	}
	
	public static class AudioDeepAnalystGraphic extends EntryRenderer {
		public String getES_Type() {
			return "ffaudiodagraphic";
		}
	}
	
	public ContainerEntryResult processFull(Container container) throws Exception {
		FFprobe ffprobe = container.getByClass(FFprobe.class);
		
		if (ffprobe == null) {
			return null;
		}
		if (ffprobe.hasAudio() == false) {
			return null;
		}
		
		File input_file = container.getPhysicalSource();
		CopyMove.checkExistsCanRead(input_file);
		ArrayList<String> params = new ArrayList<String>();
		params.add("-nostats");
		params.add("-i");
		params.add(input_file.getAbsolutePath());
		params.add("-filter_complex");
		
		params.add("ebur128=peak=true,astats,silencedetect=n=" + silencedetect_level_threshold + "dB:d=" + silencedetect_min_duration);
		params.add("-vn");
		params.add("-f");
		params.add("null");
		params.add("/dev/null");
		FFmpegDAEvents ffdae = new FFmpegDAEvents(image_width, image_height, lufs_depth, lufs_ref, truepeak_ref);
		Execprocess process = new Execprocess(ExecBinaryPath.get("ffmpeg"), params, ffdae);
		
		process.run();// TODO stoppable (via start)
		
		RenderedFile rf_lufs_truepeak_graphic = new RenderedFile("lufs_truepeak_graphic", "jpg");
		
		ffdae.closeLastSilence();
		ffdae.saveLUFSGraphic(rf_lufs_truepeak_graphic.getTempFile(), jpg_compression_ratio);
		
		AudioDeepAnalystGraphic entry_graphic = new AudioDeepAnalystGraphic();
		entry_graphic.getOptions().addProperty("width", image_width);
		entry_graphic.getOptions().addProperty("height", image_height);
		entry_graphic.getOptions().addProperty("lufs_depth", lufs_depth);
		entry_graphic.getOptions().addProperty("lufs_ref", lufs_ref);
		entry_graphic.getOptions().addProperty("truepeak_ref", truepeak_ref);
		
		rf_lufs_truepeak_graphic.consolidateAndExportToEntry(entry_graphic, container, this);
		
		ffdae.ffmpeg_da_result.silencedetect_level_threshold = silencedetect_level_threshold;
		ffdae.ffmpeg_da_result.silencedetect_min_duration = silencedetect_min_duration;
		
		StringBuilder summay = new StringBuilder();
		
		summay.append(ffdae.ffmpeg_da_result.integrated_loudness);
		summay.append(" LUFS, True peak: ");
		summay.append(ffdae.ffmpeg_da_result.true_peak);
		summay.append(" dB");
		
		if (ffdae.ffmpeg_da_result.silences != null) {
			summay.append(", ");
			summay.append(ffdae.ffmpeg_da_result.silences.size());
			if (ffdae.ffmpeg_da_result.silences.size() > 1) {
				summay.append(" silences detected");
			} else {
				summay.append(" silence detected");
			}
		}
		
		container.getSummary().putSummaryContent(ffdae.ffmpeg_da_result, summay.toString());
		
		return new ContainerEntryResult(entry_graphic, ffdae.ffmpeg_da_result);
	}
	
	private class FFmpegDAEvents implements ExecprocessEvent {
		
		public void onError(IOException ioe) {
			Loggers.Transcode.error("FFmpeg error", ioe);
		}
		
		public void onError(InterruptedException ie) {
			Loggers.Transcode.error("FFmpeg threads error", ie);
		}
		
		public void onStdout(String message) {
			// System.out.println(message);
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
		private float truepeak_ref;
		private FFmpegAudioDeepAnalyst ffmpeg_da_result;
		
		public FFmpegDAEvents(int image_width, int image_height, float lufs_depth, float lufs_ref, float truepeak_ref) throws Exception {
			this.lufs_range = lufs_depth;
			this.image_width = image_width;
			this.image_height = image_height;
			this.lufs_ref = lufs_ref;
			this.truepeak_ref = truepeak_ref;
			
			ffmpeg_da_result = new FFmpegAudioDeepAnalyst();
			
			series_momentary = new TimeSeries("Momentary");
			series_short_term = new TimeSeries("Short term");
			series_integrated = new TimeSeries("Integrated");
			series_true_peak_per_frame = new TimeSeries("True peak");
		}
		
		private void saveLUFSGraphic(File output_file, float jpg_compression_ratio) throws IOException {
			if (output_file == null) {
				throw new NullPointerException("\"output_file\" can't to be null");
			}
			if (output_file.exists() & output_file.isFile()) {
				FileUtils.forceDelete(output_file);
			}
			
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
			zero_pos.setLabel("   " + String.valueOf(lufs_ref));
			zero_pos.setLabelPaint(Color.CYAN);
			zero_pos.setAlpha(0.5f);
			zero_pos.setLabelBackgroundColor(Color.CYAN);
			zero_pos.setPaint(Color.CYAN);
			zero_pos.setOutlinePaint(Color.CYAN);
			
			/**
			 * Display the -3 line
			 */
			ValueMarker zero_pos2 = new ValueMarker(truepeak_ref);
			zero_pos2.setLabel("   " + String.valueOf(truepeak_ref));
			zero_pos2.setLabelPaint(Color.CYAN);
			zero_pos2.setAlpha(0.5f);
			zero_pos2.setLabelBackgroundColor(Color.CYAN);
			zero_pos2.setPaint(Color.CYAN);
			zero_pos2.setOutlinePaint(Color.CYAN);
			
			float dash[] = { 5.0f };
			BasicStroke dash_stroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
			zero_pos.setStroke(dash_stroke);
			zero_pos2.setStroke(dash_stroke);
			plot.addRangeMarker(zero_pos);
			plot.addRangeMarker(zero_pos2);
			
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
		
		private void closeLastSilence() {
			if (current_silence != null) {
				if (ffmpeg_da_result.silences == null) {
					ffmpeg_da_result.silences = new ArrayList<FFmpegAudioDeepAnalystSilenceDetect>();
				}
				ffmpeg_da_result.silences.add(current_silence);
				current_silence = null;
			}
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
