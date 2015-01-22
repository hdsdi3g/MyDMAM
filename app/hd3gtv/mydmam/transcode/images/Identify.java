package hd3gtv.mydmam.transcode.images;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.metadata.GeneratorAnalyser;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.EntryAnalyser;
import hd3gtv.tools.ExecprocessBadExecutionException;
import hd3gtv.tools.ExecprocessGettext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class Identify implements GeneratorAnalyser {
	
	private String identify_bin;
	
	// TODO limits
	// -limit memory 100MB -limit map 100MB -limit area 100MB -limit disk 30MB -limit file 50 -limit time 50
	
	// TODO @see FFprobeAnalyser
	
	public Identify() {
		identify_bin = Configuration.global.getValue("transcoding", "identify_bin", "identify");
	}
	
	@Deprecated
	void testProcessAll(File[] files) throws IOException {
		GsonBuilder gb = new GsonBuilder();
		gb.setPrettyPrinting();
		for (int pos = 0; pos < files.length; pos++) {
			System.out.println(files[pos]);
			System.out.println(gb.create().toJson(analyst(files[pos])));
		}
	}
	
	private class IdentifyLine {
		int incr_level;
		String key_name;
		String value;
		
		IdentifyLine(int incr_level, String key_name, String value) {
			this.incr_level = incr_level;
			this.key_name = key_name;
			this.value = value;
		}
		
		public String toString() {
			return incr_level + "\t" + key_name + " -> " + value;
		}
	}
	
	private class JsonObjectParent {
		JsonObject item;
		
		/**
		 * Null if item is the root.
		 */
		JsonObjectParent parent;
		
		JsonObjectParent(JsonObject item, JsonObjectParent parent) {
			this.item = item;
			this.parent = parent;
		}
		
		public String toString() {
			if (parent != null) {
				return hashCode() + ":" + parent.toString();
			}
			return hashCode() + ":";
		}
	}
	
	@Deprecated
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
		
		ArrayList<IdentifyLine> raw_lines = new ArrayList<Identify.IdentifyLine>(lines.length);
		String current_sub_key = null;
		String line;
		String key_name;
		String value;
		final String SPACE_SEPARATOR = "  ";
		int pos_colon;
		int pos_sub_colon;
		int pos_first_space;
		int incr_level;
		String incr_header;
		String sub_key_name;
		
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
				pos_first_space = line.indexOf(" ", pos_colon);
				
				if (line.trim().endsWith(":")) {
					pos_sub_colon = -1;
				} else {
					pos_sub_colon = line.indexOf(":", pos_colon);
					if (pos_sub_colon == (pos_first_space - 1)) {
						pos_sub_colon = -1;
					}
				}
				
				incr_level = 0;
				incr_header = SPACE_SEPARATOR;
				
				for (int pos_incr = 0; pos_incr < 10; pos_incr++) {
					/**
					 * Count the number of double-spaces line header. +One pair = +1 incr_level
					 */
					incr_level = pos_incr;
					incr_header = incr_header + SPACE_SEPARATOR;
					if (line.startsWith(incr_header) == false) {
						break;
					}
				}
				
				if (pos_sub_colon > -1) {
					/**
					 * Line is type AAAA:BBBB: cccc
					 */
					key_name = line.substring(0, pos_sub_colon).trim();
					pos_sub_colon = line.indexOf(":", pos_colon + 1);
					sub_key_name = line.substring(pos_colon + 1, pos_sub_colon);
					
					value = line.substring(pos_sub_colon + 1).trim();
					if (current_sub_key == null) {
						raw_lines.add(new IdentifyLine(incr_level, key_name, ""));
						// System.out.println(raw_lines.get(raw_lines.size() - 1));
						current_sub_key = key_name;
					} else if (current_sub_key.equals(key_name) == false) {
						raw_lines.add(new IdentifyLine(incr_level, key_name, ""));
						// System.out.println(raw_lines.get(raw_lines.size() - 1));
						current_sub_key = key_name;
					}
					raw_lines.add(new IdentifyLine(incr_level + 1, sub_key_name, value));
					
					// previous_line.incr_level + 1
					// System.out.println(raw_lines.get(raw_lines.size() - 1));
				} else {
					/**
					 * Line is type AAAA: bbbb
					 */
					current_sub_key = null;
					key_name = line.substring(0, pos_colon).trim();
					if (key_name.equals("Histogram")) {
						/**
						 * Remove "Histogram" key. Its values are very difficult to parse.
						 */
						continue;
					}
					
					value = line.substring(pos_colon + 1).trim();
					try {
						/**
						 * Remove int key name.
						 */
						Integer.parseInt(key_name);
						continue;
					} catch (NumberFormatException e) {
					}
					
					raw_lines.add(new IdentifyLine(incr_level, key_name, value));
					// System.out.println(raw_lines.get(raw_lines.size() - 1));
				}
			}
		}
		
		JsonObject result = new JsonObject();
		JsonObject current_node = result;
		JsonObjectParent previous_parent_node = null;
		
		IdentifyLine current_line;
		IdentifyLine previous_line = null;
		for (int pos = 0; pos < raw_lines.size(); pos++) {
			current_line = raw_lines.get(pos);
			// System.out.println(current_line);
			if (pos > 0) {
				previous_line = raw_lines.get(pos - 1);
			} else {
				previous_line = current_line;
			}
			
			if (previous_line.incr_level < current_line.incr_level) {
				/**
				 * Go to child level
				 */
				JsonObject child_node = new JsonObject();
				current_node.add(previous_line.key_name, child_node);
				previous_parent_node = new JsonObjectParent(current_node, previous_parent_node);
				current_node = child_node;
			} else if (previous_line.incr_level > current_line.incr_level) {
				/**
				 * Return to next parent level
				 */
				int delta = (previous_line.incr_level - current_line.incr_level) - 1;
				for (int pos_delta = 0; pos_delta < delta; pos_delta++) {
					previous_parent_node = previous_parent_node.parent;
				}
				current_node = previous_parent_node.item;
				previous_parent_node = previous_parent_node.parent;
			}
			current_node.addProperty(current_line.key_name, current_line.value);
			
		}
		
		return result;
	}
	
	// TODO parse identify result to ImageAttributes
	
	@Override
	public boolean canProcessThis(String mimetype) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public String getLongName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public EntryAnalyser process(Container container) throws Exception {
		// TODO Auto-generated method stub
		
		// TODO return ImageAttributes
		// ImageAttributes result = Operations.getGson().fromJson(json, ImageAttributes.class);
		
		return null;
	}
	
	@Override
	public List<String> getMimeFileListCanUsedInMasterAsPreview() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean isCanUsedInMasterAsPreview(Container container) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public Class<? extends EntryAnalyser> getRootEntryClass() {
		// TODO Auto-generated method stub
		return null;
	}
}
