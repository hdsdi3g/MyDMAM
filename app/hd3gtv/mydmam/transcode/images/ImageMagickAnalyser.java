package hd3gtv.mydmam.transcode.images;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.metadata.GeneratorAnalyser;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.EntryAnalyser;
import hd3gtv.mydmam.metadata.container.Operations;
import hd3gtv.tools.ExecprocessBadExecutionException;
import hd3gtv.tools.ExecprocessGettext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ImageMagickAnalyser implements GeneratorAnalyser {
	
	private String convert_bin;
	
	// TODO limits
	// -limit memory 100MB -limit map 100MB -limit area 100MB -limit disk 30MB -limit file 50 -limit time 50
	
	// TODO @see FFprobeAnalyser
	
	public ImageMagickAnalyser() {
		convert_bin = Configuration.global.getValue("transcoding", "convert_bin", "convert");
	}
	
	@Override
	public EntryAnalyser process(Container container) throws Exception {
		ArrayList<String> param = new ArrayList<String>();
		ExecprocessGettext process = null;
		try {
			param.add(container.getOrigin().getPhysicalSource().getPath() + "[0]");
			param.add("json:-");
			
			process = new ExecprocessGettext(convert_bin, param);
			process.setEndlinewidthnewline(true);
			process.start();
			
			JsonParser p = new JsonParser();
			JsonObject result = p.parse(process.getResultstdout().toString()).getAsJsonObject();
			result = result.get("image").getAsJsonObject();
			
			if (result.has("profiles")) {
				if (result.get("profiles").getAsJsonObject().has("iptc")) {
					/**
					 * Import and inject IPTC
					 */
					param.add(container.getOrigin().getPhysicalSource().getPath() + "[0]");
					param.add("iptctext:-");
					process = new ExecprocessGettext(convert_bin, param);
					process.setEndlinewidthnewline(true);
					process.setExitcodemusttobe0(false);
					process.start();
					
					if (process.getRunprocess().getExitvalue() == 0) {
						// process.getResultstdout()
						// TODO parse IPTC raw data like : 2#120#Caption="Description avec des àccénts.&#13;et un sau\t de &quot;ligne&quot;"
						// and insert in result.properties.iptc:2-120 -> Description...
						// beware, some entries are not single !
					}
				}
				result.remove("profiles");
			}
			
			result.remove("artifacts");
			
			return Operations.getGson().fromJson(result, ImageAttributes.class);
		} catch (IOException e) {
			if (e instanceof ExecprocessBadExecutionException) {
				Log2Dump dump = new Log2Dump();
				dump.add("param", param);
				if (process != null) {
					dump.add("stdout", process.getResultstdout().toString().trim());
					dump.add("stderr", process.getResultstderr().toString().trim());
					dump.add("exitcode", process.getRunprocess().getExitvalue());
				}
				Log2.log.error("Problem with convert", null, dump);
			}
			throw e;
		}
	}
	
	static final ArrayList<String> mimetype_list;
	
	static {
		mimetype_list = new ArrayList<String>();
		// TODO mimetype_list
	}
	
	@Override
	public boolean canProcessThis(String mimetype) {
		return mimetype_list.contains(mimetype);
	}
	
	public boolean isEnabled() {
		return (new File(convert_bin)).exists();
	}
	
	public String getLongName() {
		return "ImageMagick Analyser";
	}
	
	public List<String> getMimeFileListCanUsedInMasterAsPreview() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean isCanUsedInMasterAsPreview(Container container) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public Class<? extends EntryAnalyser> getRootEntryClass() {
		return ImageAttributes.class;
	}
}
