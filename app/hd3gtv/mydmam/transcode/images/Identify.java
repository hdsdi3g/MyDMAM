package hd3gtv.mydmam.transcode.images;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.tools.ExecprocessBadExecutionException;
import hd3gtv.tools.ExecprocessGettext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.google.gson.JsonObject;

public class Identify {
	
	private String identify_bin;
	
	// -limit memory 100MB -limit map 100MB -limit area 100MB -limit disk 30MB -limit file 50 -limit time 50
	/*		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
				try {
					String line = "";
					while (channel.isClosed() == false) {
						while (((line = reader.readLine()) != null)) {
	 * */
	
	public Identify() {
		identify_bin = Configuration.global.getValue("transcoding", "identify_bin", "identify");
	}
	
	JsonObject analyst(File inputfile) throws IOException {
		ArrayList<String> param = new ArrayList<String>();
		param.add("-verbose");
		param.add(inputfile.getCanonicalPath() + "[0]");
		
		ExecprocessGettext process = new ExecprocessGettext(identify_bin, param);
		process.setEndlinewidthnewline(true);
		try {
			process.start();
		} catch (IOException e) {
			if (e instanceof ExecprocessBadExecutionException) {
				Log2Dump dump = new Log2Dump();
				dump.add("param", param);
				dump.add("stdout", process.getResultstdout().toString().trim());
				dump.add("stderr", process.getResultstderr().toString().trim());
				dump.add("exitcode", process.getRunprocess().getExitvalue());
				Log2.log.error("Problem with identify", null, dump);
			}
			throw e;
		}
		
		String[] lines = process.getResultstdout().toString().split(ExecprocessGettext.LINESEPARATOR);
		
		JsonObject result = new JsonObject();
		
		String line;
		String key_name;
		String value;
		final String SPACE_SEPARATOR = "  ";
		int pos_colon;
		int incr_level;
		String incr_header;
		JsonObject current_branch = result;
		int previous_level = 0;
		
		for (int pos = 0; pos < lines.length; pos++) {
			line = lines[pos];
			if (pos == 0 && line.startsWith("Image: ")) {
				continue;
			}
			
			if (line.startsWith(SPACE_SEPARATOR)) {
				pos_colon = line.indexOf(":");
				if (pos_colon == -1) {
					continue;
				}
				incr_level = 0;
				incr_header = SPACE_SEPARATOR;
				
				for (int pos_incr = 0; pos_incr < 10; pos_incr++) {
					/**
					 * Count the number of double-spaces line header. One pair = 1 incr_level
					 */
					incr_level = pos_incr;
					incr_header = incr_header + SPACE_SEPARATOR;
					if (line.startsWith(incr_header) == false) {
						break;
					}
				}
				
				key_name = line.substring(0, pos_colon).trim();
				value = line.substring(pos_colon + 1).trim();
				System.out.println(incr_level + "\t" + key_name + " -> " + value);
				
				if (incr_level == previous_level) {
					current_branch.addProperty(key_name, value);
				} else if (incr_level > previous_level) {
					current_branch = new JsonObject();
					
				} else {
					
				}
				// TODO add to current value
				// result
				previous_level = incr_level;
			}
			// System.out.println(line);
		}
		return result;
	}
	// TODO parse identify result to ImageAttributes
}
