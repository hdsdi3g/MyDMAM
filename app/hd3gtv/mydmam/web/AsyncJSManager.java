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
package hd3gtv.mydmam.web;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.GsonIgnoreStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import play.vfs.VirtualFile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import controllers.Secure;

@SuppressWarnings("rawtypes")
public class AsyncJSManager<V extends AsyncJSControllerVerb<Rq, Rp>, Rq extends AsyncJSRequestObject, Rp extends AsyncJSResponseObject> implements AsyncJSGsonProvider {
	
	public static final String ASYNC_CLASS_PATH = "/app/controllers/asyncjs";
	public static final String ASYNC_PACKAGE_NAME = "controllers.asyncjs";
	public static final AsyncJSManager<?, ?, ?> global;
	private static final Gson gson_simple;
	private static final GsonIgnoreStrategy ignore_strategy;
	private static final JsonParser parser;
	
	static {
		GsonBuilder builder = new GsonBuilder();
		ignore_strategy = new GsonIgnoreStrategy();
		builder.addDeserializationExclusionStrategy(ignore_strategy);
		builder.addSerializationExclusionStrategy(ignore_strategy);
		builder.serializeNulls();
		builder.registerTypeAdapter(Class.class, new MyDMAM.GsonClassSerializer());
		gson_simple = builder.create();
		
		parser = new JsonParser();
		
		global = new AsyncJSManager();
	}
	
	private HashMap<String, HashMap<String, AsyncJSControllerVerb<Rq, Rp>>> declarations;
	private Gson gson;
	
	private HashMap<String, List<String>> controllers_mandatory_privileges;
	private HashMap<String, HashMap<String, List<String>>> verbs_mandatory_privileges;
	
	/**
	 * Beware, maybe duplicates entries.
	 */
	private List<String> all_privileges_names;
	
	private AsyncJSManager() {
		declarations = new HashMap<String, HashMap<String, AsyncJSControllerVerb<Rq, Rp>>>();
		controllers_mandatory_privileges = new HashMap<String, List<String>>();
		verbs_mandatory_privileges = new HashMap<String, HashMap<String, List<String>>>();
		all_privileges_names = new ArrayList<String>();
		
		/**
		 * For the moment, gson never be null.
		 */
		gson = gson_simple;
		reload();
	}
	
	@SuppressWarnings("unchecked")
	public void reload() {
		Log2.log.debug("Reload Async classes");
		
		List<VirtualFile> main_dirs = JsCompile.getAllfromRelativePath(ASYNC_CLASS_PATH, true, true);
		List<VirtualFile> class_vfiles = new ArrayList<VirtualFile>();
		for (int pos = 0; pos < main_dirs.size(); pos++) {
			class_vfiles.addAll(main_dirs.get(pos).list());
		}
		
		List<AsyncJSController> all_js_controllers = new ArrayList<AsyncJSController>();
		Class<?> class_candidate;
		for (int pos = 0; pos < class_vfiles.size(); pos++) {
			String name = class_vfiles.get(pos).getName();
			if (name.endsWith(".java") == false) {
				continue;
			}
			try {
				class_candidate = Class.forName(ASYNC_PACKAGE_NAME + "." + name.substring(0, name.length() - ".java".length()));
			} catch (Exception e) {
				Log2.log.error("Invalid class loading", e);
				continue;
			}
			if (AsyncJSController.class.isAssignableFrom(class_candidate)) {
				try {
					all_js_controllers.add((AsyncJSController) class_candidate.newInstance());
				} catch (Exception e) {
					Log2.log.error("Invalid class instancing", e);
					continue;
				}
			} else {
				Log2.log.debug("Class declaration is not a valid expected type", new Log2Dump("java file", class_vfiles.get(pos).getRealFile()));
			}
		}
		
		/**
		 * Prepare declarations
		 */
		declarations.clear();
		controllers_mandatory_privileges.clear();
		verbs_mandatory_privileges.clear();
		List<AsyncJSControllerVerb<Rq, Rp>> all_verbs = new ArrayList<AsyncJSControllerVerb<Rq, Rp>>();
		
		AsyncJSController controller;
		
		List<AsyncJSControllerVerb<AsyncJSRequestObject, AsyncJSResponseObject>> manager_verbs;
		AsyncJSControllerVerb<AsyncJSRequestObject, AsyncJSResponseObject> verb;
		HashMap<String, AsyncJSControllerVerb<Rq, Rp>> map_managed_verbs;
		HashMap<String, List<String>> map_privileges_verbs;
		Log2Dump dump = new Log2Dump();
		
		for (int pos_ctrl = 0; pos_ctrl < all_js_controllers.size(); pos_ctrl++) {
			controller = all_js_controllers.get(pos_ctrl);
			manager_verbs = controller.getManagedVerbs();
			map_managed_verbs = new HashMap<String, AsyncJSControllerVerb<Rq, Rp>>();
			map_privileges_verbs = new HashMap<String, List<String>>();
			
			for (int pos_mv = 0; pos_mv < manager_verbs.size(); pos_mv++) {
				verb = manager_verbs.get(pos_mv);
				map_managed_verbs.put(verb.getVerbName(), (AsyncJSControllerVerb<Rq, Rp>) verb);
				all_verbs.add((AsyncJSControllerVerb<Rq, Rp>) verb);
				map_privileges_verbs.put(verb.getVerbName(), verb.getMandatoryPrivileges());
				all_privileges_names.addAll(verb.getMandatoryPrivileges());
				dump.add("controller", controller.getRequestName() + "/" + verb.getVerbName());
			}
			declarations.put(controller.getRequestName(), map_managed_verbs);
			controllers_mandatory_privileges.put(controller.getRequestName(), controller.getMandatoryPrivileges());
			all_privileges_names.addAll(controller.getMandatoryPrivileges());
			verbs_mandatory_privileges.put(controller.getRequestName(), map_privileges_verbs);
		}
		
		Log2.log.debug("Declare all AsyncJS controllers", dump);
		
		/**
		 * Prepare serialisation engine
		 */
		GsonBuilder builder = new GsonBuilder();
		builder.addDeserializationExclusionStrategy(ignore_strategy);
		builder.addSerializationExclusionStrategy(ignore_strategy);
		builder.serializeNulls();
		builder.registerTypeAdapter(Class.class, new MyDMAM.GsonClassSerializer());
		
		List<? extends AsyncJSSerializer<?>> map_serializers;
		List<? extends AsyncJSDeserializer<?>> map_deserializers;
		List<Class<?>> serializer_enclosing_classes = new ArrayList<Class<?>>();
		List<Class<?>> deserializer_enclosing_classes = new ArrayList<Class<?>>();
		Class<?> enclosing_class;
		List<AsyncJSSerializer<?>> all_declared_serializers = new ArrayList<AsyncJSSerializer<?>>();
		List<AsyncJSDeserializer<?>> all_declared_deserializers = new ArrayList<AsyncJSDeserializer<?>>();
		
		for (int pos_vbs = 0; pos_vbs < all_verbs.size(); pos_vbs++) {
			map_serializers = all_verbs.get(pos_vbs).getJsonSerializers(this);
			for (int pos = 0; pos < map_serializers.size(); pos++) {
				enclosing_class = map_serializers.get(pos).getEnclosingClass();
				if (all_declared_serializers.contains(map_serializers.get(pos))) {
					/**
					 * Twice declaration for this serializer.
					 */
					continue;
				}
				if (serializer_enclosing_classes.contains(enclosing_class)) {
					/**
					 * Twice declaration for this serializer class, but not with the same serializer !
					 */
					dump = new Log2Dump();
					dump.add("class", enclosing_class);
					dump.add("serializer", map_serializers.get(pos).getClass());
					Log2.log.debug("Duplicate serializer entry !", dump);
					continue;
				}
				serializer_enclosing_classes.add(enclosing_class);
				builder.registerTypeAdapter(enclosing_class, map_serializers.get(pos));
				all_declared_serializers.add(map_serializers.get(pos));
			}
			
			map_deserializers = all_verbs.get(pos_vbs).getJsonDeserializers(this);
			for (int pos = 0; pos < map_deserializers.size(); pos++) {
				enclosing_class = map_deserializers.get(pos).getEnclosingClass();
				if (all_declared_deserializers.contains(map_deserializers.get(pos))) {
					/**
					 * Twice declaration for this deserializer.
					 */
					continue;
				}
				if (deserializer_enclosing_classes.contains(enclosing_class)) {
					/**
					 * Twice declaration for this deserializer class, but not with the same deserializer !
					 */
					dump = new Log2Dump();
					dump.add("class", enclosing_class);
					dump.add("serializer", map_deserializers.get(pos).getClass());
					Log2.log.debug("Duplicate deserializer entry !", dump);
					continue;
				}
				deserializer_enclosing_classes.add(enclosing_class);
				builder.registerTypeAdapter(enclosing_class, map_deserializers.get(pos));
				all_declared_deserializers.add(map_deserializers.get(pos));
			}
		}
		
		for (int pos = 0; pos < serializer_enclosing_classes.size(); pos++) {
			try {
				serializer_enclosing_classes.get(pos).newInstance();
			} catch (Exception e) {
				Log2.log.error("Can't instance (for serializing test) class", e, new Log2Dump("class name", deserializer_enclosing_classes.get(pos).getName()));
			}
		}
		for (int pos = 0; pos < deserializer_enclosing_classes.size(); pos++) {
			try {
				deserializer_enclosing_classes.get(pos).newInstance();
			} catch (Exception e) {
				Log2.log.error("Can't instance (for deserializing test) class", e, new Log2Dump("class name", deserializer_enclosing_classes.get(pos).getName()));
			}
		}
		
		gson = builder.create();
	}
	
	public String doRequest(String raw_json_request) throws SecurityException, ClassNotFoundException {
		/**
		 * Extract Json
		 */
		JsonObject json_request = parser.parse(raw_json_request).getAsJsonObject();
		String request_name = json_request.get("name").getAsString();
		String verb = json_request.get("verb").getAsString();
		JsonObject request_content = json_request.get("content").getAsJsonObject();
		
		/**
		 * Check request privilege
		 */
		if (controllers_mandatory_privileges.containsKey(request_name)) {
			if (Secure.checkview(controllers_mandatory_privileges.get(request_name)) == false) {
				throw new SecurityException("Missing privileges for this request");
			}
		}
		
		/**
		 * Check verb privilege
		 */
		if (verbs_mandatory_privileges.containsKey(request_name)) {
			HashMap<String, List<String>> verb_privileges = verbs_mandatory_privileges.get(request_name);
			if (verb_privileges.containsKey(verb)) {
				if (Secure.checkview(verb_privileges.get(verb)) == false) {
					throw new SecurityException("Missing privileges for this request verb");
				}
			}
		}
		
		/**
		 * Get controller
		 */
		if (declarations.containsKey(request_name) == false) {
			throw new ClassNotFoundException("Invalid request \"" + request_name + "\"");
		}
		HashMap<String, AsyncJSControllerVerb<Rq, Rp>> verbs_ctrl = declarations.get(request_name);
		if (verbs_ctrl.containsKey(verb) == false) {
			throw new ClassNotFoundException("Invalid verb \"" + verb + "\" for request \"" + request_name + "\"");
		}
		AsyncJSControllerVerb<Rq, Rp> verb_ctrl = verbs_ctrl.get(verb);
		
		/**
		 * Deserialise request
		 */
		Rq request_object = gson.fromJson(request_content, verb_ctrl.getRequestClass());
		
		/**
		 * Execute request
		 */
		Rp response = null;
		try {
			response = verb_ctrl.onRequest(request_object);
		} catch (Exception e) {
			Log2Dump dump = new Log2Dump();
			dump.add("request_name", request_name);
			dump.add("verb", verb);
			dump.add("request", request_content.toString());
			Log2.log.error("Can't process request", e, dump);
			response = verb_ctrl.failResponse();
		}
		
		/**
		 * Serialize response
		 */
		
		return gson.toJson(response, verb_ctrl.getResponseClass());
	}
	
	public void putAllPrivilegesNames(List<String> mergue_with_list) {
		for (int pos = 0; pos < all_privileges_names.size(); pos++) {
			if (mergue_with_list.contains(all_privileges_names.get(pos)) == false) {
				mergue_with_list.add(all_privileges_names.get(pos));
			}
		}
	}
	
	public Gson getGson() {
		return gson;
	}
	
	public Gson getGsonSimple() {
		return gson_simple;
	}
}
