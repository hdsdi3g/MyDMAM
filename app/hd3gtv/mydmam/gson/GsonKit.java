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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.gson;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.xml.datatype.XMLGregorianCalendar;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;

import hd3gtv.archivecircleapi.ACFileLocations;
import hd3gtv.archivecircleapi.ACNodesEntry;
import hd3gtv.archivecircleapi.ACPartition;
import hd3gtv.archivecircleapi.ACTape;
import hd3gtv.configuration.GithubIssue;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.auth.BasketNG;
import hd3gtv.mydmam.auth.UserActivity;
import hd3gtv.mydmam.auth.UserNotificationNG;
import hd3gtv.mydmam.auth.asyncjs.GroupChRole;
import hd3gtv.mydmam.auth.asyncjs.GroupView;
import hd3gtv.mydmam.auth.asyncjs.GroupViewList;
import hd3gtv.mydmam.auth.asyncjs.NewUser;
import hd3gtv.mydmam.auth.asyncjs.RoleChPrivileges;
import hd3gtv.mydmam.auth.asyncjs.RoleView;
import hd3gtv.mydmam.auth.asyncjs.RoleViewList;
import hd3gtv.mydmam.auth.asyncjs.UserAdminUpdate;
import hd3gtv.mydmam.auth.asyncjs.UserView;
import hd3gtv.mydmam.auth.asyncjs.UserViewList;
import hd3gtv.mydmam.bcastautomation.BCAEventCatched;
import hd3gtv.mydmam.ftpserver.AJSResponseActivities;
import hd3gtv.mydmam.ftpserver.AJSResponseUserList;
import hd3gtv.mydmam.ftpserver.AJSUser;
import hd3gtv.mydmam.ftpserver.FTPActivity;
import hd3gtv.mydmam.manager.AsyncJSBrokerResponseAction;
import hd3gtv.mydmam.manager.AsyncJSBrokerResponseList;
import hd3gtv.mydmam.manager.CyclicJobCreator;
import hd3gtv.mydmam.manager.GsonThrowable;
import hd3gtv.mydmam.manager.InstanceAction;
import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.mydmam.manager.JobCreator;
import hd3gtv.mydmam.manager.JobCreatorDeclarationSerializer;
import hd3gtv.mydmam.manager.JobNG;
import hd3gtv.mydmam.manager.TriggerJobCreator;
import hd3gtv.mydmam.manager.WorkerCapablitiesExporter;
import hd3gtv.mydmam.metadata.container.ContainerPreview;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.metadata.container.EntrySummary;
import hd3gtv.mydmam.metadata.container.RenderedContent;
import hd3gtv.mydmam.transcode.TranscodeProfile;
import hd3gtv.mydmam.transcode.TranscoderWorker;
import hd3gtv.mydmam.transcode.images.ImageAttributes;
import hd3gtv.mydmam.transcode.mtdcontainer.BBCBmx;
import hd3gtv.mydmam.transcode.mtdcontainer.FFProbeChapter;
import hd3gtv.mydmam.transcode.mtdcontainer.FFProbeFormat;
import hd3gtv.mydmam.transcode.mtdcontainer.FFProbeStream;
import hd3gtv.mydmam.transcode.mtdcontainer.FFmpegAudioDeepAnalyst;
import hd3gtv.mydmam.transcode.mtdcontainer.FFmpegAudioDeepAnalystChannelStat;
import hd3gtv.mydmam.transcode.mtdcontainer.FFmpegAudioDeepAnalystSilenceDetect;
import hd3gtv.mydmam.transcode.mtdcontainer.FFmpegInterlacingStats;
import hd3gtv.mydmam.transcode.mtdcontainer.FFprobe;
import hd3gtv.mydmam.transcode.watchfolder.AbstractFoundedFile;
import hd3gtv.mydmam.transcode.watchfolder.AsyncJSWatchfolderResponseList;
import hd3gtv.mydmam.transcode.watchfolder.WatchFolderEntry;
import hd3gtv.mydmam.web.JSSourceDatabase;
import hd3gtv.mydmam.web.JSSourceDatabase.JSSourceDBSerializer;
import hd3gtv.mydmam.web.JSSourceDatabaseEntry;
import hd3gtv.mydmam.web.search.SearchQuery;
import hd3gtv.mydmam.web.search.SearchQuery.SearchQuerySerializer;
import hd3gtv.mydmam.web.search.SearchResult;
import hd3gtv.mydmam.web.stat.AsyncStatRequest;
import hd3gtv.mydmam.web.stat.AsyncStatResult;
import hd3gtv.mydmam.web.stat.AsyncStatResultElement;
import hd3gtv.mydmam.web.stat.AsyncStatResultSubElement;
import hd3gtv.mydmam.web.stat.SortDirListing;
import hd3gtv.tools.Timecode;

public class GsonKit {
	
	public final static Type type_ArrayList_String = new TypeToken<ArrayList<String>>() {
	}.getType();
	public final static Type type_ArrayList_SortDirListing = new TypeToken<ArrayList<SortDirListing>>() {
	}.getType();
	public final static Type type_ArrayList_FTPActivity = new TypeToken<ArrayList<FTPActivity>>() {
	}.getType();
	public final static Type type_ArrayList_AJSUser = new TypeToken<ArrayList<AJSUser>>() {
	}.getType();
	public final static Type type_ArrayList_AbstractFoundedFile = new TypeToken<ArrayList<AbstractFoundedFile>>() {
	}.getType();
	public final static Type type_Map_String_JobNG = new TypeToken<Map<String, JobNG>>() {
	}.getType();
	public final static Type type_ArrayList_ACFileLocations = new TypeToken<ArrayList<ACFileLocations>>() {
	}.getType();
	public final static Type type_ArrayList_ACPartition = new TypeToken<ArrayList<ACPartition>>() {
	}.getType();
	public final static Type type_ArrayList_ACTape = new TypeToken<ArrayList<ACTape>>() {
	}.getType();
	public final static Type type_ArrayList_ACNodesEntry = new TypeToken<ArrayList<ACNodesEntry>>() {
	}.getType();
	public final static Type type_ArrayList_InetAddr = new TypeToken<ArrayList<InetAddress>>() {
	}.getType();
	public final static Type type_HashSet_String = new TypeToken<HashSet<String>>() {
	}.getType();
	public final static Type type_LinkedHashMap_StringBasketNG = new TypeToken<LinkedHashMap<String, BasketNG>>() {
	}.getType();
	public final static Type type_ArrayList_UserActivity = new TypeToken<ArrayList<UserActivity>>() {
	}.getType();
	public final static Type type_ArrayList_UserNotificationNG = new TypeToken<ArrayList<UserNotificationNG>>() {
	}.getType();
	public final static Type type_ArrayList_BCACatchEntry = new TypeToken<ArrayList<BCAEventCatched>>() {
	}.getType();
	public final static Type type_ArrayList_JobContext = new TypeToken<ArrayList<JobContext>>() {
	}.getType();
	public final static Type type_ArrayList_JobCreatorJobDeclaration = new TypeToken<ArrayList<JobCreator.Declaration>>() {
	}.getType();
	public final static Type type_Class_JobContext = new TypeToken<Class<? extends JobContext>>() {
	}.getType();
	public final static Type type_List_RenderedContent = new TypeToken<List<RenderedContent>>() {
	}.getType();
	public final static Type type_LinkedHashMap_String_String = new TypeToken<LinkedHashMap<String, String>>() {
	}.getType();
	public final static Type type_ArrayList_FFmpegAudioDeepAnalystChannelStat = new TypeToken<ArrayList<FFmpegAudioDeepAnalystChannelStat>>() {
	}.getType();
	public final static Type type_ArrayList_FFmpegAudioDeepAnalystSilenceDetect = new TypeToken<ArrayList<FFmpegAudioDeepAnalystSilenceDetect>>() {
	}.getType();
	public final static Type type_HashMap_String_String = new TypeToken<HashMap<String, String>>() {
	}.getType();
	public final static Type type_ArrayList_Chapter = new TypeToken<ArrayList<FFProbeChapter>>() {
	}.getType();
	public final static Type type_ArrayList_Stream = new TypeToken<ArrayList<FFProbeStream>>() {
	}.getType();
	public final static Type type_HashMap_String_JSSourceDatabaseEntry = new TypeToken<HashMap<String, JSSourceDatabaseEntry>>() {
	}.getType();
	public final static Type type_ArrayList_SearchResult = new TypeToken<ArrayList<SearchResult>>() {
	}.getType();
	public final static Type type_Map_String_Object = new TypeToken<Map<String, Object>>() {
	}.getType();
	public final static Type type_LinkedHashMap_Integer_GithubIssue = new TypeToken<LinkedHashMap<Integer, GithubIssue>>() {
	}.getType();
	
	private class De_Serializator {
		private Type type;
		private Object typeAdapter;
		
		private <T> De_Serializator(Type type, GsonDeSerializer<T> typeAdapter) {
			Loggers.getGsonLoggersForSpecificClass(type).debug("Declare de/serializer " + type.getTypeName());
			
			this.type = type;
			if (Loggers.getGsonLoggersForSpecificClass(type).isTraceEnabled()) {
				this.typeAdapter = new GsonDeSerializer<T>() {
					public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
						JsonElement result = typeAdapter.serialize(src, typeOfSrc, context);
						Loggers.getGsonLoggersForSpecificClass(type).trace("Serialize to " + result.toString());
						return result;
					}
					
					public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
						Loggers.getGsonLoggersForSpecificClass(type).trace("Deserialize from " + json.toString());
						return typeAdapter.deserialize(json, typeOfT, context);
					}
				};
			} else {
				this.typeAdapter = typeAdapter;
			}
		}
		
		private <T> De_Serializator(Type type, JsonSerializer<T> typeAdapter) {
			Loggers.getGsonLoggersForSpecificClass(type).debug("Declare serializer " + type.getTypeName());
			
			this.type = type;
			
			if (Loggers.getGsonLoggersForSpecificClass(type).isTraceEnabled()) {
				this.typeAdapter = new JsonSerializer<T>() {
					public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
						JsonElement result = typeAdapter.serialize(src, typeOfSrc, context);
						Loggers.getGsonLoggersForSpecificClass(type).trace("Serialize to " + result.toString());
						return result;
					}
				};
			} else {
				this.typeAdapter = typeAdapter;
			}
		}
		
		private <T> De_Serializator(Type type, JsonDeserializer<T> typeAdapter) {
			Loggers.Gson.debug("Declare deserializer " + type.getTypeName());
			
			this.type = type;
			if (Loggers.getGsonLoggersForSpecificClass(type).isTraceEnabled()) {
				this.typeAdapter = new JsonDeserializer<T>() {
					public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
						Loggers.getGsonLoggersForSpecificClass(type).trace("Deserialize from " + json.toString());
						return typeAdapter.deserialize(json, typeOfT, context);
					}
				};
			} else {
				this.typeAdapter = typeAdapter;
			}
		}
	}
	
	private GsonIgnoreStrategy ignore_strategy;
	private Gson gson_simple;
	private ArrayList<De_Serializator> gson_simple_serializator;
	private ArrayList<De_Serializator> gson_full_serializator;
	private boolean full_pretty_printing = false;
	
	/**
	 * @return Gson with GsonIgnoreStrategy, SerializeNulls and BaseSerializers
	 */
	
	public GsonKit() {
		ignore_strategy = new GsonIgnoreStrategy();
		
		gson_simple_serializator = new ArrayList<>();
		gson_full_serializator = new ArrayList<>();
		
		/**
		 * JsonArray
		 */
		registerGsonSimpleDeSerializer(JsonArray.class, JsonArray.class, src -> {
			if (src == null) {
				return null;
			}
			return src;
		}, json -> {
			try {
				return json.getAsJsonArray();
			} catch (Exception e) {
				Loggers.Manager.error("Can't deserialize JsonArray", e);
				return null;
			}
		});
		
		/**
		 * JsonObject
		 */
		registerGsonSimpleDeSerializer(JsonObject.class, JsonObject.class, src -> {
			if (src == null) {
				return null;
			}
			return src;
		}, json -> {
			try {
				return json.getAsJsonObject();
			} catch (Exception e) {
				Loggers.Manager.error("Can't deserialize JsonObject", e);
				return null;
			}
		});
		
		/**
		 * XMLGregorianCalendar
		 */
		registerGsonSimpleDeSerializer(XMLGregorianCalendar.class, XMLGregorianCalendar.class, src -> {
			return new JsonPrimitive(src.toGregorianCalendar().getTimeInMillis());
		}, json -> {
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTimeInMillis(json.getAsBigInteger().longValue());
			return new XMLGregorianCalendarImpl(gc);
		});
		
		/**
		 * Class
		 */
		registerGsonSimpleDeSerializer(Class.class, Class.class, src -> {
			if (src == null) {
				return null;
			}
			return new JsonPrimitive(src.getName());
		}, json -> {
			try {
				return MyDMAM.factory.getClassByName(json.getAsString());
			} catch (Exception e) {
				return null;
			}
		});
		
		/**
		 * InetAddress
		 */
		registerGsonSimpleDeSerializer(InetAddress.class, InetAddress.class, src -> {
			return new JsonPrimitive(src.getHostAddress());
		}, json -> {
			try {
				return InetAddress.getByName(json.getAsString());
			} catch (UnknownHostException e) {
				throw new JsonParseException(json.getAsString(), e);
			}
		});
		
		/**
		 * InetSocketAddress
		 */
		registerGsonSimpleDeSerializer(InetSocketAddress.class, InetSocketAddress.class, src -> {
			JsonObject jo = new JsonObject();
			jo.addProperty("addr", src.getHostString());
			jo.addProperty("port", src.getPort());
			return jo;
		}, json -> {
			JsonObject jo = json.getAsJsonObject();
			return new InetSocketAddress(jo.get("addr").getAsString(), jo.get("port").getAsInt());
		});
		
		/**
		 * URL
		 */
		registerGsonSimpleDeSerializer(URL.class, URL.class, src -> {
			return new JsonPrimitive(src.toString());
		}, json -> {
			try {
				return new URL(json.getAsString());
			} catch (MalformedURLException e) {
				throw new JsonParseException(json.getAsString(), e);
			}
		});
		
		/**
		 * Timecode
		 */
		registerGsonSimpleDeSerializer(Timecode.class, Timecode.class, src -> {
			if (Math.floor(src.getFps()) == Math.ceil(src.getFps())) {
				return new JsonPrimitive(src.toString() + "/" + Math.round(src.getFps()));
			}
			return new JsonPrimitive(src.toString() + "/" + src.getFps());
		}, json -> {
			String raw = json.getAsString();
			if (raw.indexOf("/") == -1) {
				throw new JsonParseException("Missing / in Timecode: " + json.toString());
			}
			String[] vars = json.getAsString().split("/");
			if (vars.length != 2) {
				throw new JsonParseException("Too many / in Timecode: " + json.toString());
			}
			float fps = -1;
			try {
				fps = Float.parseFloat(vars[1]);
			} catch (NumberFormatException e) {
				throw new JsonParseException("Invalid fps in Timecode: " + json.toString(), e);
			}
			
			return new Timecode(vars[0], fps);
		});
		
		/**
		 * Properties
		 */
		registerGsonSimpleDeSerializer(Properties.class, Properties.class, src -> {
			StringWriter pw = new StringWriter();
			try {
				src.store(pw, null);
			} catch (IOException e) {
				Loggers.Auth.warn("Can't serialize properties", e);
			}
			pw.flush();
			
			return new JsonPrimitive(pw.toString());
		}, json -> {
			Properties result = new Properties();
			StringReader sr = new StringReader(json.getAsString());
			try {
				result.load(sr);
			} catch (IOException e) {
				Loggers.Auth.warn("Can't deserialize properties", e);
			}
			
			return result;
		});
		
		/**
		 * InternetAddress
		 */
		registerGsonSimpleDeSerializer(InternetAddress.class, InternetAddress.class, src -> {
			return new JsonPrimitive(src.toString());
		}, json -> {
			try {
				return new InternetAddress(json.getAsString());
			} catch (AddressException e) {
				throw new JsonParseException(json.getAsString(), e);
			}
		});
		
		/**
		 * Locale
		 */
		registerGsonSimpleDeSerializer(Locale.class, Locale.class, src -> {
			return new JsonPrimitive(src.toLanguageTag());
		}, json -> {
			return Locale.forLanguageTag(json.getAsString());
		});
		
		gson_simple_serializator.add(new De_Serializator(JSSourceDatabaseEntry.class, new JSSourceDatabaseEntry.Serializer()));
		gson_simple_serializator.add(new De_Serializator(ContainerPreview.class, new ContainerPreview.Serializer()));
		gson_simple_serializator.add(new De_Serializator(ContainerPreview.class, new ContainerPreview.Deserializer()));
		
		/**
		 * ===========================================
		 * Gson declarations
		 * ===========================================
		 */
		
		gson_full_serializator.add(new De_Serializator(InstanceAction.class, new InstanceAction.Serializer()));
		gson_full_serializator.add(new De_Serializator(JobNG.class, new JobNG.Serializer()));
		gson_full_serializator.add(new De_Serializator(GsonThrowable.class, new GsonThrowable.Serializer()));
		gson_full_serializator.add(new De_Serializator(WorkerCapablitiesExporter.class, new WorkerCapablitiesExporter.Serializer()));
		
		gson_full_serializator.add(new De_Serializator(JobContext.class, new JobContext.Serializer()));
		gson_full_serializator.add(new De_Serializator(new TypeToken<ArrayList<JobContext>>() {
		}.getType(), new JobContext.SerializerList()));
		
		gson_full_serializator.add(new De_Serializator(JobCreatorDeclarationSerializer.class, new JobCreatorDeclarationSerializer()));
		gson_full_serializator.add(new De_Serializator(TriggerJobCreator.class, new TriggerJobCreator.Serializer()));
		gson_full_serializator.add(new De_Serializator(CyclicJobCreator.class, new CyclicJobCreator.Serializer()));
		
		gson_full_serializator.add(new De_Serializator(JSSourceDatabase.class, new JSSourceDBSerializer()));
		gson_full_serializator.add(new De_Serializator(AsyncStatResult.class, new AsyncStatResult.Serializer()));
		gson_full_serializator.add(new De_Serializator(AsyncStatResultElement.class, new AsyncStatResultElement.Serializer()));
		gson_full_serializator.add(new De_Serializator(AsyncStatResultSubElement.class, new AsyncStatResultSubElement.Serializer()));
		gson_full_serializator.add(new De_Serializator(AbstractFoundedFile.class, new AbstractFoundedFile.Serializer()));
		gson_full_serializator.add(new De_Serializator(WatchFolderEntry.class, new WatchFolderEntry.Serializer()));
		gson_full_serializator.add(new De_Serializator(TranscoderWorker.class, new TranscoderWorker.Serializer()));
		gson_full_serializator.add(new De_Serializator(TranscodeProfile.class, new TranscodeProfile.Serializer()));
		gson_full_serializator.add(new De_Serializator(SearchQuery.class, new SearchQuerySerializer()));
		gson_full_serializator.add(new De_Serializator(AJSResponseActivities.class, new JsonSerializer<AJSResponseActivities>() {
			public JsonElement serialize(AJSResponseActivities src, Type typeOfSrc, JsonSerializationContext context) {
				JsonObject result = new JsonObject();
				result.add("activities", MyDMAM.gson_kit.getGson().toJsonTree(src.activities, GsonKit.type_ArrayList_FTPActivity));
				return result;
			}
		}));
		gson_full_serializator.add(new De_Serializator(AJSResponseUserList.class, new JsonSerializer<AJSResponseUserList>() {
			public JsonElement serialize(AJSResponseUserList src, Type typeOfSrc, JsonSerializationContext context) {
				JsonObject result = new JsonObject();
				result.add("users", MyDMAM.gson_kit.getGson().toJsonTree(src.users, GsonKit.type_ArrayList_AJSUser));
				return result;
			}
		}));
		gson_full_serializator.add(new De_Serializator(AsyncJSBrokerResponseList.class, new JsonSerializer<AsyncJSBrokerResponseList>() {
			public JsonElement serialize(AsyncJSBrokerResponseList src, Type typeOfSrc, JsonSerializationContext context) {
				return src.list;
			}
		}));
		
		gson_full_serializator.add(new De_Serializator(AsyncJSBrokerResponseAction.class, new JsonSerializer<AsyncJSBrokerResponseAction>() {
			public JsonElement serialize(AsyncJSBrokerResponseAction src, Type typeOfSrc, JsonSerializationContext context) {
				return src.modified_jobs;
			}
		}));
		gson_full_serializator.add(new De_Serializator(AsyncJSWatchfolderResponseList.class, new JsonSerializer<AsyncJSWatchfolderResponseList>() {
			public JsonElement serialize(AsyncJSWatchfolderResponseList src, Type typeOfSrc, JsonSerializationContext context) {
				JsonObject result = new JsonObject();
				result.add("items", MyDMAM.gson_kit.getGson().toJsonTree(src.items, GsonKit.type_ArrayList_AbstractFoundedFile));
				result.add("jobs", MyDMAM.gson_kit.getGson().toJsonTree(src.jobs, GsonKit.type_Map_String_JobNG));
				return result;
			}
		}));
		gson_full_serializator.add(new De_Serializator(AsyncStatResult.class, new AsyncStatResult.Serializer()));
		gson_full_serializator.add(new De_Serializator(AsyncStatResultElement.class, new AsyncStatResultElement.Serializer()));
		gson_full_serializator.add(new De_Serializator(AsyncStatResultSubElement.class, new AsyncStatResultSubElement.Serializer()));
		gson_full_serializator.add(new De_Serializator(AsyncStatRequest.class, new AsyncStatRequest.Deserializer()));
		
		gson_full_serializator.add(new De_Serializator(UserView.class, new UserView.Serializer()));
		gson_full_serializator.add(new De_Serializator(UserViewList.class, new UserViewList.Serializer()));
		gson_full_serializator.add(new De_Serializator(GroupView.class, new GroupView.Serializer()));
		gson_full_serializator.add(new De_Serializator(GroupViewList.class, new GroupViewList.Serializer()));
		gson_full_serializator.add(new De_Serializator(RoleView.class, new RoleView.Serializer()));
		gson_full_serializator.add(new De_Serializator(RoleViewList.class, new RoleViewList.Serializer()));
		gson_full_serializator.add(new De_Serializator(NewUser.class, new NewUser.Deserializer()));
		gson_full_serializator.add(new De_Serializator(UserAdminUpdate.class, new UserAdminUpdate.Deserializer()));
		gson_full_serializator.add(new De_Serializator(GroupChRole.class, new GroupChRole.Deserializer()));
		gson_full_serializator.add(new De_Serializator(RoleChPrivileges.class, new RoleChPrivileges.Deserializer()));
		
		gson_full_serializator.add(new De_Serializator(EntrySummary.class, new EntrySummary.Serializer()));
		gson_full_serializator.add(new De_Serializator(EntryRenderer.class, new EntryRenderer.Serializer()));
		gson_full_serializator.add(new De_Serializator(ImageAttributes.class, new ImageAttributes.Serializer()));
		gson_full_serializator.add(new De_Serializator(FFmpegAudioDeepAnalyst.class, new FFmpegAudioDeepAnalyst.Serializer()));
		gson_full_serializator.add(new De_Serializator(FFmpegInterlacingStats.class, new FFmpegInterlacingStats.Serializer()));
		
		gson_full_serializator.add(new De_Serializator(FFprobe.class, new FFprobe.Serializer()));
		gson_full_serializator.add(new De_Serializator(FFProbeStream.class, new FFProbeStream.Serializer()));
		gson_full_serializator.add(new De_Serializator(FFProbeChapter.class, new FFProbeChapter.Serializer()));
		gson_full_serializator.add(new De_Serializator(FFProbeFormat.class, new FFProbeFormat.Serializer()));
		gson_full_serializator.add(new De_Serializator(BBCBmx.class, new BBCBmx.Serializer()));
		
		/*
		 * 	public class Serializer implements JsonSerializer<SelfSerializing> {
		public JsonElement serialize(SelfSerializing src, Type typeOfSrc, JsonSerializationContext context) {
		return selfserializer.serialize(src, MyDMAM.gson_kit.getGson());
		}
		}
		
		public class Deserializer implements JsonDeserializer<SelfSerializing> {
		public SelfSerializing deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		return (SelfSerializing) selfserializer.deserialize(ContainerOperations.getJsonObject(json, false), MyDMAM.gson_kit.getGson());
		}
		}
		
		registerDeSerializer(, .class, src -> {
		}, json -> {
		});
		*/
		
		rebuildGsonSimple();
	}
	
	private synchronized void rebuildGsonSimple() {
		GsonBuilder builder = new GsonBuilder();
		builder.serializeNulls();
		
		builder.addDeserializationExclusionStrategy(ignore_strategy);
		builder.addSerializationExclusionStrategy(ignore_strategy);
		
		gson_simple_serializator.forEach(ser -> {
			builder.registerTypeAdapter(ser.type, ser.typeAdapter);
		});
		
		if (full_pretty_printing) {
			builder.setPrettyPrinting();
		}
		
		gson_simple = builder.create();
	}
	
	public Gson getGsonSimple() {
		return gson_simple;
	}
	
	private Gson gson_full;
	private Gson gson_full_pretty;
	
	private synchronized void rebuildGson() {
		GsonBuilder builder = new GsonBuilder();
		builder.serializeNulls();
		
		builder.addDeserializationExclusionStrategy(ignore_strategy);
		builder.addSerializationExclusionStrategy(ignore_strategy);
		
		gson_simple_serializator.forEach(ser -> {
			builder.registerTypeAdapter(ser.type, ser.typeAdapter);
		});
		gson_full_serializator.forEach(ser -> {
			builder.registerTypeAdapter(ser.type, ser.typeAdapter);
		});
		
		if (full_pretty_printing) {
			builder.setPrettyPrinting();
		}
		
		gson_full = builder.create();
		
		builder.setPrettyPrinting();
		gson_full_pretty = builder.create();
	}
	
	/**
	 * @return GsonSimple + all actual registerTypeAdapter()
	 */
	public Gson getGson() {
		if (gson_full == null) {
			rebuildGson();
		}
		return gson_full;
	}
	
	/**
	 * @return GsonSimple + all actual registerTypeAdapter() + pretty printing
	 */
	public Gson getGsonPretty() {
		if (gson_full_pretty == null) {
			rebuildGson();
		}
		return gson_full_pretty;
	}
	
	public synchronized <T> GsonKit registerDeserializer(Type type, Class<T> dest_type, Function<JsonElement, T> deserializer) {
		gson_full_serializator.add(new De_Serializator(type, new JsonDeserializer<T>() {
			public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
				try {
					return deserializer.apply(json);
				} catch (Exception e) {
					Loggers.Gson.error("Can't deserialize to " + dest_type.getName(), e);
					return null;
				}
			}
		}));
		gson_full = null;
		gson_full_pretty = null;
		return this;
	}
	
	public synchronized <T> GsonKit registerSerializer(Type type, Class<T> source_type, Function<T, JsonElement> serializer) {
		gson_full_serializator.add(new De_Serializator(type, new JsonSerializer<T>() {
			public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
				try {
					return serializer.apply(src);
				} catch (Exception e) {
					Loggers.Gson.error("Can't serialize from " + source_type.getName(), e);
					return null;
				}
			}
		}));
		gson_full = null;
		gson_full_pretty = null;
		return this;
	}
	
	public synchronized <T> GsonKit registerDeSerializer(Type type, Class<T> object_type, Function<T, JsonElement> adapter_serializer, Function<JsonElement, T> adapter_deserializer) {
		gson_full_serializator.add(new De_Serializator(type, makeDeSerializer(object_type, adapter_serializer, adapter_deserializer)));
		gson_full = null;
		gson_full_pretty = null;
		return this;
	}
	
	private <T> void registerGsonSimpleDeSerializer(Type type, Class<T> object_type, Function<T, JsonElement> adapter_serializer, Function<JsonElement, T> adapter_deserializer) {
		gson_simple_serializator.add(new De_Serializator(type, makeDeSerializer(object_type, adapter_serializer, adapter_deserializer)));
	}
	
	private <T> GsonDeSerializer<T> makeDeSerializer(Class<T> object_type, Function<T, JsonElement> adapter_serializer, Function<JsonElement, T> adapter_deserializer) {
		return new GsonDeSerializer<T>() {
			public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
				try {
					return adapter_serializer.apply(src);
				} catch (Exception e) {
					Loggers.Gson.error("Can't serialize from " + object_type.getName(), e);
					return null;
				}
			}
			
			public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
				try {
					return adapter_deserializer.apply(json);
				} catch (Exception e) {
					Loggers.Gson.error("Can't deserialize from " + object_type.getName(), e);
					return null;
				}
			}
		};
	}
	
	public void setFullPrettyPrinting() {
		if (full_pretty_printing == false) {
			synchronized (this) {
				full_pretty_printing = true;
				gson_full = null;
				gson_full_pretty = null;
			}
			rebuildGsonSimple();
		}
	}
	
}
