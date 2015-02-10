package hd3gtv.mydmam.transcode.mtdcontainer;

import hd3gtv.mydmam.metadata.container.Entry;
import hd3gtv.mydmam.metadata.container.EntryAnalyser;
import hd3gtv.mydmam.metadata.container.SelfSerializing;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class FFmpegInterlacingStats extends EntryAnalyser {
	
	/**
	 * @param single_detection_value: "TFF:16 BFF:0 Progressive:0 Undetermined:8"
	 * @param multi_detection_value: "TFF:24 BFF:0 Progressive:0 Undetermined:0"
	 */
	public static FFmpegInterlacingStats parseFFmpegResult(String single_detection_value, String multi_detection_value) {
		if ((single_detection_value == null) & (multi_detection_value == null)) {
			return null;
		}
		
		FFmpegInterlacingStats stats = new FFmpegInterlacingStats();
		if (single_detection_value != null) {
			
		}
		if (multi_detection_value != null) {
			
		}
		// TODO stats
		return stats;
	}
	
	@Override
	protected void extendedInternalSerializer(JsonObject current_element, EntryAnalyser _item, Gson gson) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public String getES_Type() {
		return "ffinterlacing";
	}
	
	@Override
	protected List<Class<? extends SelfSerializing>> getSerializationDependencies() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	protected Entry internalDeserialize(JsonObject source, Gson gson) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
