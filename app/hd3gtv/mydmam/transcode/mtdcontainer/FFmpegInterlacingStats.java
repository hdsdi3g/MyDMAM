package hd3gtv.mydmam.transcode.mtdcontainer;

import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.mydmam.metadata.container.Entry;
import hd3gtv.mydmam.metadata.container.EntryAnalyser;
import hd3gtv.mydmam.metadata.container.Operations;
import hd3gtv.mydmam.metadata.container.SelfSerializing;
import hd3gtv.tools.VideoConst.Interlacing;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class FFmpegInterlacingStats extends EntryAnalyser implements Log2Dumpable {
	
	private int tff_detection;
	private int bff_detection;
	private int pfr_detection;
	private int und_detection;
	Interlacing interlacing;
	
	/**
	 * @param single_detection_value: "TFF:16 BFF:0 Progressive:0 Undetermined:8"
	 * @param multi_detection_value: "TFF:24 BFF:0 Progressive:0 Undetermined:0"
	 */
	public static FFmpegInterlacingStats parseFFmpegResult(String single_detection_value, String multi_detection_value) {
		if ((single_detection_value == null) | (multi_detection_value == null)) {
			return null;
		}
		FFmpegInterlacingStats stats = new FFmpegInterlacingStats();
		String[] items = single_detection_value.split(" ");
		stats.tff_detection = Integer.parseInt(items[0].split(":")[1]);
		stats.bff_detection = Integer.parseInt(items[1].split(":")[1]);
		stats.pfr_detection = Integer.parseInt(items[2].split(":")[1]);
		stats.und_detection = Integer.parseInt(items[3].split(":")[1]);
		
		int analysed_frames_count = stats.tff_detection + stats.bff_detection + stats.pfr_detection + stats.und_detection;
		
		items = multi_detection_value.split(" ");
		stats.tff_detection += Integer.parseInt(items[0].split(":")[1]);
		stats.bff_detection += Integer.parseInt(items[1].split(":")[1]);
		stats.pfr_detection += Integer.parseInt(items[2].split(":")[1]);
		stats.und_detection += Integer.parseInt(items[3].split(":")[1]);
		
		stats.tff_detection = stats.tff_detection / 2;
		stats.bff_detection = stats.bff_detection / 2;
		stats.pfr_detection = stats.pfr_detection / 2;
		stats.und_detection = stats.und_detection / 2;
		
		float tff_detection_percent = ((float) stats.tff_detection / (float) analysed_frames_count) * 100;
		float bff_detection_percent = ((float) stats.bff_detection / (float) analysed_frames_count) * 100;
		float pfr_detection_percent = ((float) stats.pfr_detection / (float) analysed_frames_count) * 100;
		float und_detection_percent = ((float) stats.und_detection / (float) analysed_frames_count) * 100;
		
		if ((tff_detection_percent < 6) & (bff_detection_percent < 6)) {
			if (und_detection_percent > pfr_detection_percent) {
				stats.interlacing = Interlacing.Unknow;
			} else {
				stats.interlacing = Interlacing.Progressive;
			}
		} else {
			if (tff_detection_percent > bff_detection_percent) {
				stats.interlacing = Interlacing.TopFieldFirst;
			} else {
				stats.interlacing = Interlacing.BottomFieldFirst;
			}
		}
		return stats;
	}
	
	protected void extendedInternalSerializer(JsonObject current_element, EntryAnalyser _item, Gson gson) {
	}
	
	public String getES_Type() {
		return "ffinterlacing";
	}
	
	protected List<Class<? extends SelfSerializing>> getSerializationDependencies() {
		return null;
	}
	
	protected Entry internalDeserialize(JsonObject source, Gson gson) {
		return Operations.getGsonSimple().fromJson(source, FFmpegInterlacingStats.class);
	}
	
	public Interlacing getInterlacing() {
		return interlacing;
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump result = super.getLog2Dump();
		result.add("tff_detection", tff_detection);
		result.add("bff_detection", bff_detection);
		result.add("pfr_detection", pfr_detection);
		result.add("und", und_detection);
		result.add("interlacing", interlacing);
		return result;
	}
}
