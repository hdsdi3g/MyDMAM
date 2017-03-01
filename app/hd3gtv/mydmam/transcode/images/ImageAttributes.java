package hd3gtv.mydmam.transcode.images;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.mydmam.gson.GsonKit;
import hd3gtv.mydmam.metadata.container.ContainerEntry;
import hd3gtv.mydmam.metadata.container.EntryAnalyser;
import hd3gtv.mydmam.transcode.images.ImageAttributesEnum.Colorspace;
import hd3gtv.mydmam.transcode.images.ImageAttributesEnum.Compose;
import hd3gtv.mydmam.transcode.images.ImageAttributesEnum.Compress;
import hd3gtv.mydmam.transcode.images.ImageAttributesEnum.Dispose;
import hd3gtv.mydmam.transcode.images.ImageAttributesEnum.Endian;
import hd3gtv.mydmam.transcode.images.ImageAttributesEnum.ImageClass;
import hd3gtv.mydmam.transcode.images.ImageAttributesEnum.ImageType;
import hd3gtv.mydmam.transcode.images.ImageAttributesEnum.Intensity;
import hd3gtv.mydmam.transcode.images.ImageAttributesEnum.Intent;
import hd3gtv.mydmam.transcode.images.ImageAttributesEnum.Interlace;
import hd3gtv.mydmam.transcode.images.ImageAttributesEnum.Orientation;
import hd3gtv.mydmam.transcode.images.ImageAttributesEnum.ResolutionUnits;
import hd3gtv.tools.ExecprocessGettext;

public class ImageAttributes extends EntryAnalyser {
	
	protected void extendedInternalSerializer(JsonObject current_element, EntryAnalyser _item, Gson gson) {// TODO move de/serializer
		ImageAttributes item = (ImageAttributes) _item;
		current_element.add("properties", gson.toJsonTree(item.properties, GsonKit.type_LinkedHashMap_String_String));
	}
	
	protected ContainerEntry internalDeserialize(JsonObject source, Gson gson) {// TODO move de/serializer
		String entry_value;
		for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
			if (entry.getValue().isJsonPrimitive() == false) {
				continue;
			}
			entry_value = entry.getValue().getAsString();
			if (entry_value.equalsIgnoreCase("undefined")) {
				source.add(entry.getKey(), JsonNull.INSTANCE);
			}
		}
		
		ImageAttributes item = MyDMAM.gson_kit.getGsonSimple().fromJson(source, ImageAttributes.class);
		item.properties = gson.fromJson(source.get("properties"), GsonKit.type_LinkedHashMap_String_String);
		if (source.has("class")) {
			item.imgclass = ImageClass.valueOf(source.get("class").getAsString());
		}
		return item;
	}
	
	public String getES_Type() {
		return "imagemagick";
	}
	
	String format;
	String formatDescription;
	ImageClass imgclass;
	ImageAttributeDimension resolution;
	ImageAttributeDimension printSize;
	ImageAttributeGeometry geometry;
	ResolutionUnits units;
	ImageType type;
	Endian endianess;
	Colorspace colorspace;
	int depth;
	int baseDepth;
	ImageAttributeChannelDepth channelDepth;
	ImageAttributeImageStatistics imageStatistics;
	ImageAttributeChannelStatistics channelStatistics;
	ImageAttributeChromaticity chromaticity;
	Intent renderingIntent;
	float gamma;
	String totalInkDensity;
	
	/**
	 * Color, like srgba(253,255,255,0) or rgba(255,255,255,0)
	 */
	String alpha;
	String backgroundColor;
	String borderColor;
	String matteColor;
	String transparentColor;
	
	Interlace interlace;
	Intensity intensity;
	Compose compose;
	ImageAttributeGeometry pageGeometry;
	Dispose dispose;
	int iterations;
	Compress compression;
	int quality;
	Orientation orientation;
	/**
	 * Like ImageMagick 6.9.0-0 Q16 x86_64 2015-01-21 http://www.imagemagick.org
	 */
	String version;
	/**
	 * Like 0:01.000
	 */
	String elapsedTime;
	/**
	 * like 0.000u
	 */
	String userTime;
	/**
	 * Like 512.13GB
	 */
	String pixelsPerSecond;
	/**
	 * 2.049M
	 */
	String numberPixels;
	boolean tainted;
	
	@GsonIgnore
	LinkedHashMap<String, String> properties;
	
	String createSummary() {
		StringBuffer sb = new StringBuffer();
		if (geometry != null) {
			sb.append(geometry);
			sb.append(", ");
		}
		
		if (resolution != null) {
			if (Math.round(resolution.x) != Math.round(resolution.y)) {
				sb.append(Math.round(resolution.x));
				sb.append("x");
				sb.append(Math.round(resolution.y));
			} else {
				sb.append(Math.round(resolution.x));
			}
			sb.append(" ");
			if (units != null) {
				if (units == ResolutionUnits.PixelsPerInch) {
					sb.append("dpi");
				} else {
					sb.append("px/cm");
				}
			} else {
				sb.append("dpi");
			}
			sb.append(", ");
		}
		
		if (colorspace != null) {
			sb.append(colorspace);
			sb.append(", ");
			sb.append(depth);
			sb.append("b");
		}
		
		return sb.toString();
	}
	
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
