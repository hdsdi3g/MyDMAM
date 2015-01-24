package hd3gtv.mydmam.transcode.images;

import hd3gtv.mydmam.metadata.container.Entry;
import hd3gtv.mydmam.metadata.container.EntryAnalyser;
import hd3gtv.mydmam.metadata.container.SelfSerializing;

import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class ImageAttributes extends EntryAnalyser {
	
	// TODO @see FFprobe
	
	@Override
	protected void extendedInternalSerializer(JsonObject current_element, EntryAnalyser _item, Gson gson) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void extendedInternalDeserialize(EntryAnalyser _item, JsonObject source, Gson gson) {
		// TODO Auto-generated method stub
		
	}
	
	public String getES_Type() {
		return "identify";
	}
	
	protected Entry create() {
		return new ImageAttributes();
	}
	
	@Override
	protected List<Class<? extends SelfSerializing>> getSerializationDependencies() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@SerializedName("class")
	String image_class;
	
	String name;
	String format;
	String formatDescription;
	ImageAttributeIntPoint resolution;
	ImageAttributeFloatPoint printSize;
	ImageAttributeGeometry geometry;
	String units;
	String type;
	String endianess;
	String colorspace;
	int depth;
	int baseDepth;
	ImageAttributeChannelDepth channelDepth;
	ImageAttributeImageStatistics imageStatistics;
	ImageAttributeChannelStatistics channelStatistics;
	String alpha;
	String renderingIntent;
	float gamma;
	String backgroundColor;
	String borderColor;
	String matteColor;
	String transparentColor;
	String interlace;
	String intensity;
	String compose;
	ImageAttributeGeometry pageGeometry;
	String dispose;
	int iterations;
	String compression;
	String orientation;
	String version;
	String elapsedTime;
	String userTime;
	String pixelsPerSecond;
	String numberPixels;
	String filesize;
	String tainted;
	HashMap<String, String> properties;// TODO ser/de-ser
	
}
