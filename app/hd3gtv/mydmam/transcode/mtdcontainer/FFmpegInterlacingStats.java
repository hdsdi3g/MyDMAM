package hd3gtv.mydmam.transcode.mtdcontainer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.metadata.container.ContainerEntry;
import hd3gtv.mydmam.metadata.container.EntryAnalyser;
import hd3gtv.tools.VideoConst.Interlacing;

public class FFmpegInterlacingStats extends EntryAnalyser {
	
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
		Loggers.Transcode.debug("Parse FFmpeg result for Interlacing analyst, single_detection_value: \"" + single_detection_value + "\", multi_detection_value: \"" + multi_detection_value + "\".");
		
		FFmpegInterlacingStats stats = new FFmpegInterlacingStats();
		String[] items = single_detection_value.split(":");
		stats.tff_detection = Integer.parseInt(items[1].trim().split(" ")[0]);
		stats.bff_detection = Integer.parseInt(items[2].trim().split(" ")[0]);
		stats.pfr_detection = Integer.parseInt(items[3].trim().split(" ")[0]);
		stats.und_detection = Integer.parseInt(items[4].trim().split(" ")[0]);
		
		int analysed_frames_count = stats.tff_detection + stats.bff_detection + stats.pfr_detection + stats.und_detection;
		
		items = multi_detection_value.split(":");
		stats.tff_detection += Integer.parseInt(items[1].trim().split(" ")[0]);
		stats.bff_detection += Integer.parseInt(items[2].trim().split(" ")[0]);
		stats.pfr_detection += Integer.parseInt(items[3].trim().split(" ")[0]);
		stats.und_detection += Integer.parseInt(items[4].trim().split(" ")[0]);
		
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
	
	protected ContainerEntry internalDeserialize(JsonObject source, Gson gson) {// TODO move de/serializer
		return MyDMAM.gson_kit.getGsonSimple().fromJson(source, FFmpegInterlacingStats.class);
	}
	
	public Interlacing getInterlacing() {
		return interlacing;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		sb.append(", tff: ");
		sb.append(tff_detection);
		sb.append(", bff: ");
		sb.append(bff_detection);
		sb.append(", pfr: ");
		sb.append(pfr_detection);
		sb.append(", und: ");
		sb.append(und_detection);
		sb.append(", interlacing: ");
		sb.append(interlacing);
		return sb.toString();
	}
	
}
