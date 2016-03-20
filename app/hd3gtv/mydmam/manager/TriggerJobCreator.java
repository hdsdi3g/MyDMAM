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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.manager;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import hd3gtv.tools.GsonIgnore;

public final class TriggerJobCreator extends JobCreator {
	
	private String context_hook_trigger_key;
	private @GsonIgnore JobContext context_hook;
	
	public TriggerJobCreator(AppManager manager, JobContext context_hook) {
		super(manager);
		// this.context_hook = context_hook;
		if (context_hook == null) {
			throw new NullPointerException("\"context_hook\" can't to be null");
		}
		context_hook_trigger_key = JobContext.Utility.prepareContextKeyForTrigger(context_hook);
		this.context_hook = context_hook;
	}
	
	public String getContextHookTriggerKey() {
		return context_hook_trigger_key;
	}
	
	static class Serializer extends JobCreatorSerializer<TriggerJobCreator> {
		Serializer() {
			super(TriggerJobCreator.class);
		}
		
		public JsonElement serialize(TriggerJobCreator src, Type typeOfSrc, JsonSerializationContext context) {
			JsonElement result = super.serialize(src, typeOfSrc, context);
			result.getAsJsonObject().add("context_hook", AppManager.getGson().toJsonTree(src.context_hook, JobContext.class));
			return result;
		}
		
		public TriggerJobCreator deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			TriggerJobCreator result = super.deserialize(json, typeOfT, context);
			result.context_hook = AppManager.getGson().fromJson(json.getAsJsonObject().get("context_hook"), JobContext.class);
			return result;
		}
	}
	
	static Serializer serializer = new Serializer();
	
	public Class<? extends InstanceActionReceiver> getClassToCallback() {
		return TriggerJobCreator.class;
	}
	
	public Class<?> getInstanceStatusItemReferenceClass() {
		return TriggerJobCreator.class;
	}
	
}
