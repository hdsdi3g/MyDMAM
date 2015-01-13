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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.useraction;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.orm.annotations.TypeNavigatorInputSelection;
import hd3gtv.mydmam.mail.notification.Notification;
import hd3gtv.mydmam.mail.notification.NotifyReason;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.JobNG;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import models.UserProfile;

import org.elasticsearch.action.index.IndexRequest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.netflix.astyanax.MutationBatch;

public final class UACreationRequest {
	
	public static final String ES_TYPE = "log";
	private static final long LOG_LIFETIME = 3600 * 24 * 365 * 2; // 2 years
	private static final String NOTIFICATION_REFERENCE = "useraction";
	
	static {
		try {
			Elasticsearch.enableTTL(Notification.ES_INDEX, ES_TYPE);
		} catch (Exception e) {
			Log2.log.error("Can't enable TTL in ES", e);
		}
	}
	
	private ArrayList<String> items;
	private String basket_name;
	private String comment;
	private ArrayList<NotifyReason> notification_reasons;
	private UAFinisherConfiguration finisher;
	private ArrayList<String> dependent_storages;
	
	@SuppressWarnings("unused")
	private long created_at;
	
	private ArrayList<String> user_restricted_privileges;
	private ArrayList<String> created_jobs_key;
	
	private transient ArrayList<JsonObject> json_configured_functionalities;
	private transient ArrayList<ConfiguredFunctionality> configured_functionalities;
	private transient UserProfile userprofile;
	
	private transient Explorer explorer = new Explorer();
	
	public static class Serializer implements JsonSerializer<UACreationRequest>, JsonDeserializer<UACreationRequest> {
		
		Gson gson_simple = new Gson();
		private static Type typeOfArrayString = new TypeToken<ArrayList<String>>() {
		}.getType();
		private static Type typeOfArrayNotifyReason = new TypeToken<ArrayList<NotifyReason>>() {
		}.getType();
		
		public JsonElement serialize(UACreationRequest src, Type typeOfSrc, JsonSerializationContext context) {
			/**
			 * Only use it for log to ES.
			 */
			JsonObject result = gson_simple.toJsonTree(src).getAsJsonObject();
			
			JsonArray ja_configured_functionalities = new JsonArray();
			for (int pos = 0; pos < src.json_configured_functionalities.size(); pos++) {
				ja_configured_functionalities.add(src.json_configured_functionalities.get(pos));
			}
			result.add("configured_functionalities", ja_configured_functionalities);
			result.remove("user_restricted_privileges");
			
			result.addProperty("userprofile_longname", src.userprofile.longname);
			result.addProperty("userprofile_language", src.userprofile.language);
			result.addProperty("userprofile_key", src.userprofile.key);
			result.addProperty("userprofile_email", src.userprofile.email);
			
			return result;
		}
		
		public UACreationRequest deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject request = json.getAsJsonObject();
			UACreationRequest result = gson_simple.fromJson(json, UACreationRequest.class);
			result.items = UAManager.getGson().fromJson(request.get("items"), typeOfArrayString);
			if (result.items.isEmpty()) {
				throw new JsonParseException("No items");
			}
			
			result.notification_reasons = UAManager.getGson().fromJson(request.get("notification_reasons"), typeOfArrayNotifyReason);
			result.finisher = UAManager.getGson().fromJson(request.get("finisher"), UAFinisherConfiguration.class);
			
			result.json_configured_functionalities = new ArrayList<JsonObject>();
			JsonArray ja_configured_functionalities = request.get("configured_functionalities").getAsJsonArray();
			for (int pos = 0; pos < ja_configured_functionalities.size(); pos++) {
				result.json_configured_functionalities.add(ja_configured_functionalities.get(pos).getAsJsonObject());
			}
			
			result.created_at = System.currentTimeMillis();
			return result;
		}
		
	}
	
	public UACreationRequest setUserprofile(UserProfile userprofile) {
		this.userprofile = userprofile;
		return this;
	}
	
	public UACreationRequest setUserRestrictedPrivileges(ArrayList<String> user_restricted_privileges) {
		this.user_restricted_privileges = user_restricted_privileges;
		return this;
	}
	
	private void addUALogEntry() {
		String json_data = UAManager.getGson().toJson(this);
		
		long now = System.currentTimeMillis();
		
		StringBuffer sb = new StringBuffer();
		sb.append(now);
		sb.append(":");
		sb.append(userprofile.key);
		
		IndexRequest ir = new IndexRequest(Notification.ES_INDEX, ES_TYPE, sb.toString());
		ir.source(json_data);
		ir.ttl(LOG_LIFETIME);
		Elasticsearch.getClient().index(ir);
		
		Log2Dump dump = new Log2Dump();
		dump.add("id", sb.toString());
		dump.add("raw_json", json_data);
		Log2.log.debug("Create UserAction", dump);
	}
	
	private class ConfiguredFunctionality {
		UAConfigurator associated_user_configuration;
		UAFunctionalityContext functionality;
		ArrayList<String> navigator_input_selection_storages;
		
		ConfiguredFunctionality(JsonObject json_functionality) throws ClassNotFoundException, SecurityException, IllegalArgumentException, IllegalAccessException, IOException {
			String functionality_classname = json_functionality.get("functionality_classname").getAsString();
			JsonElement raw_associated_user_configuration = json_functionality.get("raw_associated_user_configuration");
			
			if (user_restricted_privileges.contains(functionality_classname) == false) {
				throw new SecurityException("Functionality: " + functionality_classname);
			}
			
			functionality = UAManager.getByName(functionality_classname);
			if (functionality == null) {
				throw new ClassNotFoundException("Can't found functionality " + functionality_classname + ".");
			}
			
			navigator_input_selection_storages = new ArrayList<String>();
			
			associated_user_configuration = functionality.createEmptyConfiguration();
			if (associated_user_configuration != null) {
				Object object_user_configuration = UAManager.getGson().fromJson(raw_associated_user_configuration, associated_user_configuration.getObjectClass());
				associated_user_configuration.setObject(object_user_configuration);
				
				Field[] fields = object_user_configuration.getClass().getFields();
				Field field;
				Object extracted_field;
				TypeNavigatorInputSelection field_conf;
				SourcePathIndexerElement item_spie;
				String string_extracted_field;
				for (int pos = 0; pos < fields.length; pos++) {
					field = fields[pos];
					if (field.isAnnotationPresent(TypeNavigatorInputSelection.class) == false) {
						continue;
					}
					extracted_field = field.get(object_user_configuration);
					if ((extracted_field instanceof String) == false) {
						throw new ClassNotFoundException("Invalid extracted field (" + field.getName() + ") from " + object_user_configuration.getClass().getName());
					}
					string_extracted_field = (String) extracted_field;
					if (string_extracted_field.equals("")) {
						continue;
					}
					item_spie = explorer.getelementByIdkey(string_extracted_field);
					if (item_spie == null) {
						throw new FileNotFoundException("Can't found pathindex file from extracted field (" + field.getName() + ") from " + object_user_configuration.getClass().getName());
					}
					field_conf = field.getAnnotation(TypeNavigatorInputSelection.class);
					
					if (item_spie.directory) {
						if (field_conf.canselectdirs() == false) {
							throw new IOException("Invalid pathindex file type (directory) " + item_spie.toString(" ") + " from extracted field (" + field.getName() + ") from "
									+ object_user_configuration.getClass().getName());
						}
						if (item_spie.currentpath.equals("/") & (field_conf.canselectstorages() == false)) {
							throw new IOException("Invalid pathindex file type (storage) " + item_spie.toString(" ") + " from extracted field (" + field.getName() + ") from "
									+ object_user_configuration.getClass().getName());
						}
					} else {
						if (field_conf.canselectfiles() == false) {
							throw new IOException("Invalid pathindex file type (file) " + item_spie.toString(" ") + " from extracted field (" + field.getName() + ") from "
									+ object_user_configuration.getClass().getName());
						}
					}
					
					if (navigator_input_selection_storages.contains(item_spie.storagename) == false) {
						navigator_input_selection_storages.add(item_spie.storagename);
					}
				}
			}
		}
	}
	
	public void createJobs() throws Exception {
		
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		
		ArrayList<SourcePathIndexerElement> basket_content = new ArrayList<SourcePathIndexerElement>(items.size());
		LinkedHashMap<String, SourcePathIndexerElement> elements = explorer.getelementByIdkeys(items);
		basket_content.addAll(elements.values());
		items.clear();
		items.addAll(elements.keySet());
		
		configured_functionalities = new ArrayList<UACreationRequest.ConfiguredFunctionality>();
		for (int pos = 0; pos < json_configured_functionalities.size(); pos++) {
			configured_functionalities.add(new ConfiguredFunctionality(json_configured_functionalities.get(pos)));
		}
		
		if (comment == null) {
			comment = "";
		}
		
		Notification notification = Notification.create(userprofile, comment, NOTIFICATION_REFERENCE);
		
		for (int pos_nr = 0; pos_nr < notification_reasons.size(); pos_nr++) {
			notification.updateNotifyReasonForUser(userprofile, notification_reasons.get(pos_nr), true);
		}
		
		// System.out.println(UAManager.getGson().toJson(this));
		
		LinkedHashMap<String, ArrayList<SourcePathIndexerElement>> storageindexname_to_itemlist = new LinkedHashMap<String, ArrayList<SourcePathIndexerElement>>();
		ArrayList<SourcePathIndexerElement> items_list;
		for (Map.Entry<String, SourcePathIndexerElement> item : elements.entrySet()) {
			if (storageindexname_to_itemlist.containsKey(item.getValue().storagename)) {
				items_list = storageindexname_to_itemlist.get(item.getValue().storagename);
			} else {
				items_list = new ArrayList<SourcePathIndexerElement>();
				storageindexname_to_itemlist.put(item.getValue().storagename, items_list);
			}
			items_list.add(item.getValue());
		}
		
		created_jobs_key = new ArrayList<String>();
		dependent_storages = new ArrayList<String>();
		
		UACreationRequest.ConfiguredFunctionality configured_functionality;
		UAConfigurator associated_user_configuration;
		UAFunctionalityContext functionality;
		String storage_name;
		ArrayList<SourcePathIndexerElement> items;
		
		for (int pos_cf = 0; pos_cf < configured_functionalities.size(); pos_cf++) {
			configured_functionality = configured_functionalities.get(pos_cf);
			associated_user_configuration = configured_functionality.associated_user_configuration;
			functionality = configured_functionality.functionality;
			functionality.content = new UAJobFunctionalityContextContent();
			functionality.content.basket_name = basket_name;
			functionality.content.creator_user_key = userprofile.key;
			functionality.content.finisher = finisher;
			functionality.content.functionality_class = functionality.getClass();
			functionality.neededstorages = new ArrayList<String>();
			functionality.content.items = new ArrayList<String>();
			functionality.content.user_configuration = associated_user_configuration;
			
			notification.addProfileReference(functionality.getMessageBaseName());
			
			for (Map.Entry<String, ArrayList<SourcePathIndexerElement>> item : storageindexname_to_itemlist.entrySet()) {
				/**
				 * Create a job by storage.
				 */
				storage_name = item.getKey();
				items = item.getValue();
				
				if (dependent_storages.contains(storage_name) == false) {
					dependent_storages.add(storage_name);
				}
				
				functionality.neededstorages.clear();
				functionality.neededstorages.add(storage_name);
				
				for (int pos_tnis = 0; pos_tnis < configured_functionality.navigator_input_selection_storages.size(); pos_tnis++) {
					if (functionality.neededstorages.contains(configured_functionality.navigator_input_selection_storages.get(pos_tnis)) == false) {
						functionality.neededstorages.add(configured_functionality.navigator_input_selection_storages.get(pos_tnis));
					}
					if (dependent_storages.contains(configured_functionality.navigator_input_selection_storages.get(pos_tnis)) == false) {
						dependent_storages.add(configured_functionality.navigator_input_selection_storages.get(pos_tnis));
					}
				}
				
				functionality.content.items.clear();
				for (int pos_it = 0; pos_it < items.size(); pos_it++) {
					functionality.content.items.add(items.get(pos_it).prepare_key());
				}
				
				StringBuffer name = new StringBuffer();
				name.append(functionality.getLongName());
				name.append(" for ");
				name.append(userprofile.longname);
				name.append(" (");
				name.append(functionality.content.items.size());
				name.append(" items in ");
				name.append(storage_name);
				name.append(")");
				
				JobNG new_job = AppManager.createJob(functionality);
				new_job.setCreator(getClass());
				
				long max_execution_time = functionality.getMaxExecutionTime();
				if (max_execution_time > 0) {
					new_job.setMaxExecutionTime(max_execution_time, TimeUnit.MILLISECONDS);
				}
				
				new_job.setName(name.toString());
				
				new_job.publish(mutator);
				
				created_jobs_key.add(new_job.getKey());
				notification.addLinkedJob(new_job);
			}
		}
		
		if (mutator.isEmpty() == false) {
			mutator.execute();
		}
		
		notification.save();
		
		addUALogEntry();
		
	}
}
