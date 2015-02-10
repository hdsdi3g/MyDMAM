package hd3gtv.mydmam.transcode.mtdgenerator;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.metadata.GeneratorAnalyser;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.EntryAnalyser;
import hd3gtv.mydmam.transcode.mtdcontainer.FFmpegInterlacingStats;
import hd3gtv.tools.ExecprocessBadExecutionException;
import hd3gtv.tools.ExecprocessGettext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FFmpegInterlacingDetection implements GeneratorAnalyser {
	
	private String ffmpeg_bin;
	
	public FFmpegInterlacingDetection() {
		ffmpeg_bin = Configuration.global.getValue("transcoding", "ffmpeg_bin", "ffmpeg");
	}
	
	public boolean isEnabled() {
		return (new File(ffmpeg_bin)).exists();
	}
	
	@Override
	public boolean canProcessThis(String mimetype) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public String getLongName() {
		return "Video interlacing detection type";
	}
	
	@Override
	public EntryAnalyser process(Container container) throws Exception {
		ArrayList<String> param = new ArrayList<String>();
		param.add("-i");
		param.add("/Users/fabien/Downloads/debug/MXF_OP1A_D10_50_4-3_608.mxf");
		param.add("-filter:v");
		param.add("idet");
		param.add("-frames:v");
		param.add("1000");
		param.add("-an");
		param.add("-f");
		param.add("md5");
		param.add("-y");
		param.add("-");
		
		ExecprocessGettext process = new ExecprocessGettext(ffmpeg_bin, param);
		process.setEndlinewidthnewline(true);
		try {
			process.start();
		} catch (IOException e) {
			if (e instanceof ExecprocessBadExecutionException) {
				Log2Dump dump = new Log2Dump();
				if (process.getRunprocess().getExitvalue() == 1) {
					dump.add("stderr", process.getResultstderr().toString().trim());
					Log2.log.error("Invalid data found when processing input", null, dump);
				} else {
					dump.add("stdout", process.getResultstdout().toString().trim());
					dump.add("stderr", process.getResultstderr().toString().trim());
					dump.add("exitcode", process.getRunprocess().getExitvalue());
					Log2.log.error("Problem with ffmpeg", null, dump);
				}
			}
			throw e;
		}
		
		String[] lines = process.getResultstderr().toString().split(ExecprocessGettext.LINESEPARATOR);
		String current_line;
		String item;
		String item_single = null;
		String item_multi = null;
		for (int pos_ln = 0; pos_ln < lines.length; pos_ln++) {
			current_line = lines[pos_ln];
			if (current_line.startsWith("[Parsed_idet_0 @") == false) {
				continue;
			}
			// [Parsed_idet_0 @ 0x7f844b700000] Single frame detection: TFF:16 BFF:0 Progressive:0 Undetermined:8
			// [Parsed_idet_0 @ 0x7f844b700000] Multi frame detection: TFF:24 BFF:0 Progressive:0 Undetermined:0
			item = current_line.substring(current_line.indexOf("]") + 1).trim();
			if (item.startsWith("Single frame detection:")) {
				item_single = item.substring("Single frame detection:".length()).trim();
				continue;
			}
			if (item.startsWith("Multi frame detection:")) {
				item_multi = item.substring("Multi frame detection:".length()).trim();
				continue;
			}
		}
		
		return FFmpegInterlacingStats.parseFFmpegResult(item_single, item_multi);
	}
	
	public List<String> getMimeFileListCanUsedInMasterAsPreview() {
		return null;
	}
	
	public boolean isCanUsedInMasterAsPreview(Container container) {
		return false;
	}
	
	public Class<? extends EntryAnalyser> getRootEntryClass() {
		return FFmpegInterlacingStats.class;
	}
	
}
