package hd3gtv.mydmam.transcode.images;

import hd3gtv.mydmam.metadata.container.Entry;
import hd3gtv.mydmam.metadata.container.EntryAnalyser;
import hd3gtv.mydmam.metadata.container.Operations;
import hd3gtv.mydmam.metadata.container.SelfSerializing;
import hd3gtv.tools.ExecprocessGettext;
import hd3gtv.tools.GsonIgnore;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public class ImageAttributes extends EntryAnalyser {
	
	// @see FFprobe
	
	private static Type properties_typeOfT = new TypeToken<LinkedHashMap<String, String>>() {
	}.getType();
	
	protected void extendedInternalSerializer(JsonObject current_element, EntryAnalyser _item, Gson gson) {
		ImageAttributes item = (ImageAttributes) _item;
		current_element.add("properties", gson.toJsonTree(item.properties, properties_typeOfT));
	}
	
	protected Entry internalDeserialize(JsonObject source, Gson gson) {
		for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
			if (entry.getValue().isJsonPrimitive() == false) {
				continue;
			}
			if (entry.getValue().getAsString().equalsIgnoreCase("Undefined")) {
				source.add(entry.getKey(), JsonNull.INSTANCE);
			}
		}
		
		ImageAttributes item = Operations.getGsonSimple().fromJson(source, ImageAttributes.class);
		item.properties = gson.fromJson(source.get("properties"), properties_typeOfT);
		if (source.has("class")) {
			item.imgclass = source.get("class").getAsString();
		}
		return item;
	}
	
	public String getES_Type() {
		return "imagemagick";
	}
	
	protected List<Class<? extends SelfSerializing>> getSerializationDependencies() {
		return null;
	}
	
	String format;
	String formatDescription;
	String imgclass;
	ImageAttributeDimension resolution;
	ImageAttributeDimension printSize;
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
	ImageAttributeChromaticity chromaticity;
	String alpha;
	String renderingIntent;
	float gamma;
	String totalInkDensity;
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
	boolean tainted;
	
	@GsonIgnore
	LinkedHashMap<String, String> properties;
	
	/**
	 * Parse IPTC raw data like : 2#120#Caption="Raw value\nWith spaces and&nbsp;entities"
	 */
	static void injectIPTCInProperties(String raw_iptc_values, JsonObject jo_properties) {
		String[] lines = raw_iptc_values.split(ExecprocessGettext.LINESEPARATOR);
		
		String line;
		int equal_pos;
		String value;
		String[] headers;
		StringBuffer key_name;
		String[] next_headers;
		
		/**
		 * some entries are not single !
		 */
		int current_header_pos = -1;
		
		for (int pos = 0; pos < lines.length; pos++) {
			line = lines[pos];
			equal_pos = line.indexOf("=");
			value = StringEscapeUtils.unescapeHtml(line.substring(equal_pos + 2, line.length() - 1));
			headers = line.substring(0, equal_pos).split("#");
			
			key_name = new StringBuffer();
			key_name.append("iptc:");
			key_name.append(headers[0]);
			key_name.append("-");
			key_name.append(headers[1]);
			
			if (current_header_pos > -1) {
				key_name.append("_");
				key_name.append(current_header_pos);
			}
			
			if (pos + 1 == lines.length) {
				jo_properties.addProperty(key_name.toString(), value);
				continue;
			}
			
			next_headers = lines[pos + 1].split("#");
			if (headers[0].equals(next_headers[0]) && headers[1].equals(next_headers[1])) {
				if (current_header_pos == -1) {
					key_name.append("_0");
					current_header_pos = 1;
				} else {
					current_header_pos++;
				}
			} else {
				current_header_pos = -1;
			}
			jo_properties.addProperty(key_name.toString(), value);
		}
	}
	
}
