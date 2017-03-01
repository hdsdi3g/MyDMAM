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
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

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

import controllers.asyncjs.demo.Comment;
import controllers.asyncjs.demo.CommentList;
import hd3gtv.archivecircleapi.ACFileLocations;
import hd3gtv.archivecircleapi.ACNodesEntry;
import hd3gtv.archivecircleapi.ACPartition;
import hd3gtv.archivecircleapi.ACTape;
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
import hd3gtv.mydmam.bcastautomation.BCACatchEntry;
import hd3gtv.mydmam.ftpserver.AJSResponseActivities;
import hd3gtv.mydmam.ftpserver.AJSResponseUserList;
import hd3gtv.mydmam.ftpserver.AJSUser;
import hd3gtv.mydmam.ftpserver.FTPActivity;
import hd3gtv.mydmam.manager.AsyncJSBrokerResponseAction;
import hd3gtv.mydmam.manager.AsyncJSBrokerResponseList;
import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.mydmam.manager.JobCreator;
import hd3gtv.mydmam.manager.JobNG;
import hd3gtv.mydmam.metadata.container.ContainerPreview;
import hd3gtv.mydmam.metadata.container.RenderedContent;
import hd3gtv.mydmam.transcode.TranscodeProfile;
import hd3gtv.mydmam.transcode.TranscoderWorker;
import hd3gtv.mydmam.transcode.mtdcontainer.Chapter;
import hd3gtv.mydmam.transcode.mtdcontainer.FFmpegAudioDeepAnalystChannelStat;
import hd3gtv.mydmam.transcode.mtdcontainer.FFmpegAudioDeepAnalystSilenceDetect;
import hd3gtv.mydmam.transcode.mtdcontainer.Stream;
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
	public final static Type type_ArrayList_BCACatchEntry = new TypeToken<ArrayList<BCACatchEntry>>() {
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
	public final static Type type_ArrayList_Chapter = new TypeToken<ArrayList<Chapter>>() {
	}.getType();
	public final static Type type_ArrayList_Stream = new TypeToken<ArrayList<Stream>>() {
	}.getType();
	public final static Type type_HashMap_String_JSSourceDatabaseEntry = new TypeToken<HashMap<String, JSSourceDatabaseEntry>>() {
	}.getType();
	public final static Type type_ArrayList_SearchResult = new TypeToken<ArrayList<SearchResult>>() {
	}.getType();
	public final static Type type_Map_String_Object = new TypeToken<Map<String, Object>>() {
	}.getType();
	
	public GsonKit() {
	}
	
	private GsonBuilder builder;
	private Gson gson_simple;
	private Gson gson_full;
	
	private void init() {
		if (builder != null) {
			return;
		}
		synchronized (this) {
			builder = new GsonBuilder();
			builder.serializeNulls();
			
			GsonIgnoreStrategy ignore_strategy = new GsonIgnoreStrategy();
			builder.addDeserializationExclusionStrategy(ignore_strategy);
			builder.addSerializationExclusionStrategy(ignore_strategy);
			
			/**
			 * JsonArray
			 */
			registerDeSerializer(JsonArray.class, JsonArray.class, src -> {
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
			registerDeSerializer(JsonObject.class, JsonObject.class, src -> {
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
			registerDeSerializer(XMLGregorianCalendar.class, XMLGregorianCalendar.class, src -> {
				return new JsonPrimitive(src.toGregorianCalendar().getTimeInMillis());
			}, json -> {
				GregorianCalendar gc = new GregorianCalendar();
				gc.setTimeInMillis(json.getAsBigInteger().longValue());
				return new XMLGregorianCalendarImpl(gc);
			});
			
			/**
			 * Class
			 */
			registerDeSerializer(Class.class, Class.class, src -> {
				if (src == null) {
					return null;
				}
				return new JsonPrimitive(src.getName());
			}, json -> {
				try {
					return Class.forName(json.getAsString());
				} catch (Exception e) {
					return null;
				}
			});
			
			/**
			 * InetAddress
			 */
			registerDeSerializer(InetAddress.class, InetAddress.class, src -> {
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
			registerDeSerializer(InetSocketAddress.class, InetSocketAddress.class, src -> {
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
			registerDeSerializer(URL.class, URL.class, src -> {
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
			registerDeSerializer(Timecode.class, Timecode.class, src -> {
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
			registerDeSerializer(Properties.class, Properties.class, src -> {
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
			
			builder.registerTypeAdapter(JSSourceDatabaseEntry.class, new JSSourceDatabaseEntry.Serializer());
			builder.registerTypeAdapter(ContainerPreview.class, new ContainerPreview.Serializer());
			builder.registerTypeAdapter(ContainerPreview.class, new ContainerPreview.Deserializer());
			
			gson_simple = builder.create();
			
			/**
			 * ===========================================
			 * Gson declarations
			 * ===========================================
			 */
			
			builder.registerTypeAdapter(JSSourceDatabase.class, new JSSourceDBSerializer());
			builder.registerTypeAdapter(AsyncStatResult.class, new AsyncStatResult.Serializer());
			builder.registerTypeAdapter(AsyncStatResultElement.class, new AsyncStatResultElement.Serializer());
			builder.registerTypeAdapter(AsyncStatResultSubElement.class, new AsyncStatResultSubElement.Serializer());
			builder.registerTypeAdapter(AbstractFoundedFile.class, new AbstractFoundedFile.Serializer());
			builder.registerTypeAdapter(WatchFolderEntry.class, new WatchFolderEntry.Serializer());
			builder.registerTypeAdapter(TranscoderWorker.class, new TranscoderWorker.Serializer());
			builder.registerTypeAdapter(TranscodeProfile.class, new TranscodeProfile.Serializer());
			builder.registerTypeAdapter(SearchQuery.class, new SearchQuerySerializer());
			builder.registerTypeAdapter(AJSResponseActivities.class, new JsonSerializer<AJSResponseActivities>() {
				public JsonElement serialize(AJSResponseActivities src, Type typeOfSrc, JsonSerializationContext context) {
					JsonObject result = new JsonObject();
					result.add("activities", MyDMAM.gson_kit.getGson().toJsonTree(src.activities, GsonKit.type_ArrayList_FTPActivity));
					return result;
				}
			});
			builder.registerTypeAdapter(AJSResponseUserList.class, new JsonSerializer<AJSResponseUserList>() {
				public JsonElement serialize(AJSResponseUserList src, Type typeOfSrc, JsonSerializationContext context) {
					JsonObject result = new JsonObject();
					result.add("users", MyDMAM.gson_kit.getGson().toJsonTree(src.users, GsonKit.type_ArrayList_AJSUser));
					return result;
				}
			});
			builder.registerTypeAdapter(AsyncJSBrokerResponseList.class, new JsonSerializer<AsyncJSBrokerResponseList>() {
				public JsonElement serialize(AsyncJSBrokerResponseList src, Type typeOfSrc, JsonSerializationContext context) {
					return src.list;
				}
			});
			
			builder.registerTypeAdapter(AsyncJSBrokerResponseAction.class, new JsonSerializer<AsyncJSBrokerResponseAction>() {
				public JsonElement serialize(AsyncJSBrokerResponseAction src, Type typeOfSrc, JsonSerializationContext context) {
					return src.modified_jobs;
				}
			});
			builder.registerTypeAdapter(AsyncJSWatchfolderResponseList.class, new JsonSerializer<AsyncJSWatchfolderResponseList>() {
				public JsonElement serialize(AsyncJSWatchfolderResponseList src, Type typeOfSrc, JsonSerializationContext context) {
					JsonObject result = new JsonObject();
					result.add("items", MyDMAM.gson_kit.getGson().toJsonTree(src.items, GsonKit.type_ArrayList_AbstractFoundedFile));
					result.add("jobs", MyDMAM.gson_kit.getGson().toJsonTree(src.jobs, GsonKit.type_Map_String_JobNG));
					return result;
				}
			});
			builder.registerTypeAdapter(ContainerPreview.class, new JsonSerializer<ContainerPreview>() {
				JsonSerializer<ContainerPreview> internal_s = new ContainerPreview.Serializer();
				
				public JsonElement serialize(ContainerPreview src, Type typeOfSrc, JsonSerializationContext context) {
					return internal_s.serialize(src, typeOfSrc, context);
				}
			});
			builder.registerTypeAdapter(AsyncStatResult.class, new AsyncStatResult.Serializer());
			builder.registerTypeAdapter(AsyncStatResultElement.class, new AsyncStatResultElement.Serializer());
			builder.registerTypeAdapter(AsyncStatResultSubElement.class, new AsyncStatResultSubElement.Serializer());
			builder.registerTypeAdapter(AsyncStatRequest.class, new AsyncStatRequest.Deserializer());
			builder.registerTypeAdapter(CommentList.class, new JsonSerializer<CommentList>() {
				public JsonElement serialize(CommentList src, Type typeOfSrc, JsonSerializationContext context) {
					JsonObject result = MyDMAM.gson_kit.getGsonSimple().toJsonTree(src).getAsJsonObject();
					result.add("commentlist", MyDMAM.gson_kit.getGsonSimple().toJsonTree(src.commentlist, new TypeToken<ArrayList<Comment>>() {
					}.getType()));
					result.addProperty("hey", "ohoh");
					return result;
				}
			});
			builder.registerTypeAdapter(UserView.class, new UserView.Serializer());
			builder.registerTypeAdapter(UserViewList.class, new UserViewList.Serializer());
			builder.registerTypeAdapter(GroupView.class, new GroupView.Serializer());
			builder.registerTypeAdapter(GroupViewList.class, new GroupViewList.Serializer());
			builder.registerTypeAdapter(RoleView.class, new RoleView.Serializer());
			builder.registerTypeAdapter(RoleViewList.class, new RoleViewList.Serializer());
			builder.registerTypeAdapter(NewUser.class, new NewUser.Deserializer());
			builder.registerTypeAdapter(UserAdminUpdate.class, new UserAdminUpdate.Deserializer());
			builder.registerTypeAdapter(GroupChRole.class, new GroupChRole.Deserializer());
			builder.registerTypeAdapter(RoleChPrivileges.class, new RoleChPrivileges.Deserializer());
			
			builder.registerTypeAdapter(ContainerPreview.class, new ContainerPreview.Serializer());
			builder.registerTypeAdapter(ContainerPreview.class, new ContainerPreview.Deserializer());
			// TODO add all ContainerEntry de/serializer
			
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
			
			 * 
			 */
			
			/*
			 * 
			registerDeSerializer(, .class, src -> {
			}, json -> {
			});
			*/
			
			// builder.setPrettyPrinting();
		}
		
	}
	
	/**
	 * @return Gson with GsonIgnoreStrategy, SerializeNulls and BaseSerializers
	 */
	public Gson getGsonSimple() {
		init();
		return gson_simple;
	}
	
	/**
	 * @param type_adapter @see GsonBuilder.registerTypeAdapter()
	 * @return this
	 */
	public synchronized GsonKit registerTypeAdapter(Type type, Object type_adapter) {
		init();
		synchronized (this) {
			builder.registerTypeAdapter(type, type_adapter);
			gson_full = null;
		}
		return this;
	}
	
	/**
	 * @return GsonSimple + all actual registerTypeAdapter()
	 */
	public Gson getGson() {
		init();
		if (gson_full == null) {
			synchronized (this) {
				gson_full = builder.create();
			}
		}
		return gson_full;
	}
	
	public <T> GsonKit registerDeserializer(Type type, Class<T> dest_type, Function<JsonElement, T> deserializer) {
		return registerTypeAdapter(type, new JsonDeserializer<T>() {
			public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
				try {
					return deserializer.apply(json);
				} catch (Exception e) {
					Loggers.Gson.error("Can't deserialize to " + dest_type.getName(), e);
					return null;
				}
			}
		});
	}
	
	public <T> GsonKit registerSerializer(Type type, Class<T> source_type, Function<T, JsonElement> serializer) {
		return registerTypeAdapter(type, new JsonSerializer<T>() {
			public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
				try {
					return serializer.apply(src);
				} catch (Exception e) {
					Loggers.Gson.error("Can't serialize from " + source_type.getName(), e);
					return null;
				}
			}
		});
	}
	
	public <T> GsonKit registerDeSerializer(Type type, Class<T> object_type, Function<T, JsonElement> adapter_serializer, Function<JsonElement, T> adapter_deserializer) {
		return registerTypeAdapter(type, new GsonDeSerializer<T>() {
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
					Loggers.Gson.error("Can't deserialize to " + object_type.getName(), e);
					return null;
				}
			}
		});
	}
	
}
