/*
 * This file is part of MyDMAM.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.bcastautomation;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.function.Function;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.cli.CLIDefinition;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.tools.ApplicationArgs;
import hd3gtv.tools.TableList;
import hd3gtv.tools.TableList.Row;

public class CLIBCA implements CLIDefinition {
	
	public String getCliModuleName() {
		return "bca";
	}
	
	public String getCliModuleShortDescr() {
		return "Broadcast automation";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		if (args.getParamExist("-parse")) {
			File file_to_parse = new File(args.getSimpleParamValue("-parse"));
			
			BCAEngine engine = BCAWatcher.getEngine();
			
			TableList tl = new TableList();
			
			final HashMap<String, ConfigurationItem> import_other_properties_configuration = Configuration.getElement(Configuration.global.getElement("broadcast_automation"), "import_other_properties");
			
			SimpleDateFormat date_format = new SimpleDateFormat("HH:mm:ss");
			
			engine.processScheduleFile(file_to_parse, (event, schedule_type) -> {
				tl.addRow(date_format.format(new Date(event.getStartDate())), event.getName(), event.getDuration().toString(), event.getOtherProperties(import_other_properties_configuration).toString());
			}, BCAScheduleType.OTHER);
			
			tl.print();
			
			return;
		} else if (args.getParamExist("-dump")) {
			if (CassandraDb.isColumnFamilyExists(CassandraDb.getkeyspace(), BCAWatcher.CF_NAME) == false) {
				System.err.println("No BCA events in database");
				return;
			}
			
			TimedEventStore database = new TimedEventStore(CassandraDb.getkeyspace(), BCAWatcher.CF_NAME);
			final boolean show_json = args.getParamExist("-raw");
			final boolean show_key = args.getParamExist("-key");
			
			TableList table = new TableList();
			
			if (show_json) {
				database.getNonFilteredAll().forEach(event -> {
					Row row = table.createRow();
					if (show_key) {
						row.addCell(event.getKey());
					}
					row.addCell(Loggers.dateLog(event.getStartDate()));
					row.addCell(event.getCols().getColumnByName(BCAWatcher.DB_COL_CONTENT_NAME).getStringValue());
				});
			} else {
				final JsonParser p = new JsonParser();
				Function<String, ArrayList<String>> json_reducer = (event) -> {
					ArrayList<String> result = new ArrayList<>();
					JsonObject jo_event = p.parse(event).getAsJsonObject();
					result.add(jo_event.get("name").getAsString());
					result.add(jo_event.get("duration").getAsString());
					result.add(jo_event.get("channel").getAsString());
					result.add(jo_event.get("file_id").getAsString());
					result.add(jo_event.get("video_source").getAsString());
					return result;
				};
				
				database.getNonFilteredAll().forEach(event -> {
					event.toTable(table, show_key, json_reducer);
				});
				
				if (table.size() > 0) {
					Row footer = table.createRow();
					if (show_key) {
						footer.addCell("Event key");
					}
					footer.addCells("Start date", "End date", "Name", "Duration", "Channel", "File", "Source", "Aired");
					
					System.out.println("Showed " + (table.size() - 1) + " events");
				}
			}
			
			table.print();
			
			return;
			/*} else if (args.getParamExist("-catch")) {
				if (CassandraDb.isColumnFamilyExists(CassandraDb.getkeyspace(), BCAWatcher.CF_NAME) == false) {
					System.err.println("No BCA events in database");
					return;
				}
				
				TimedEventStore database = new TimedEventStore(CassandraDb.getkeyspace(), BCAWatcher.CF_NAME);
				
				TableList table = new TableList();
				
				final JsonParser p = new JsonParser();
				Function<String, ArrayList<String>> json_reducer = (event) -> {
					ArrayList<String> result = new ArrayList<>();
					JsonObject jo_event = p.parse(event).getAsJsonObject();
					result.add(jo_event.get("name").getAsString());
					result.add(jo_event.get("duration").getAsString());
					result.add(jo_event.get("channel").getAsString());
					result.add(jo_event.get("file_id").getAsString());
					result.add(jo_event.get("video_source").getAsString());
					result.add(jo_event.get("other").toString());
					return result;
				};
				
				database.getFilteredAll().forEach(event -> {
					if (event.isAired()) {
						return;
					}
					event.toTable(table, true, json_reducer);
				});
				
				if (table.size() > 0) {
					Row footer = table.createRow();
					footer.addCell("Event key");
					footer.addCells("Start date", "End date", "Name", "Duration", "Channel", "File", "Source", "Aired");
					
					System.out.println("Showed " + (table.size() - 1) + " events");
				}
				
				table.print();
				
				return;*/
		}
		
		showFullCliModuleHelp();
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage (with no confirm)");
		System.out.println(" * just parse playlist/asrun: " + getCliModuleName() + " -parse file.sch");
		System.out.println(" * get all events actually in database: " + getCliModuleName() + " -dump [-raw] [-key]");
		System.out.println("   with -raw for display raw content");
		System.out.println("   with -key for display event key");
		// System.out.println(" * event catcher debug: " + getCliModuleName() + " -catch");
	}
	
	public boolean isFunctionnal() {
		return Configuration.global.isElementExists("broadcast_automation");
	}
}
