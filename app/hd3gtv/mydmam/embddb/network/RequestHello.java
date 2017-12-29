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
 * Copyright (C) hdsdi3g for hd3g.tv 8 janv. 2017
 * 
*/
package hd3gtv.mydmam.embddb.network;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.google.gson.JsonObject;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonKit;

public class RequestHello extends RequestHandler<Void> {
	
	private static Logger log = Logger.getLogger(RequestHello.class);
	
	private final Consumer<Node> afterTheHello;
	
	public RequestHello(PoolManager pool_manager, Consumer<Node> afterTheHello) {
		super(pool_manager);
		this.afterTheHello = afterTheHello;
		if (afterTheHello == null) {
			throw new NullPointerException("\"afterTheHello\" can't to be null");
		}
	}
	
	public HandleName getHandleName() {
		return new HandleName("hello");
	}
	
	public void onRequest(DataBlock block, Node source_node) {
		try {
			source_node.setDistantDate(block.getCreateDate());
			
			JsonObject jo = block.getJsonDatas().getAsJsonObject();
			source_node.setUUIDRef(UUID.fromString(jo.get("uuid").getAsString()));
			source_node.setLocalServerNodeAddresses(MyDMAM.gson_kit.getGsonSimple().fromJson(jo.get("listen_on"), GsonKit.type_ArrayList_InetSocketAddr));
			
			String clear_challenge = jo.get("clear_challenge").getAsString();
			String ciphed_challenge = jo.get("ciphed_challenge").getAsString();
			
			boolean challenge_is_ok = Arrays.equals(Base64.decodeBase64(clear_challenge), pool_manager.getProtocol().decrypt(Base64.decodeBase64(ciphed_challenge)));
			if (challenge_is_ok == false) {
				throw new GeneralSecurityException("Invalid challenge result");
			}
			
			afterTheHello.accept(source_node);
		} catch (IOException | GeneralSecurityException e) {
			log.warn(e.getMessage() + ". Disconnect it.");
			source_node.close(getClass());
		}
	}
	
	public DataBlock createRequest(Void options) {
		JsonObject jo = new JsonObject();
		jo.addProperty("uuid", pool_manager.getUUIDRef().toString());
		jo.add("listen_on", MyDMAM.gson_kit.getGsonSimple().toJsonTree(pool_manager.getListenedServerAddress().collect(Collectors.toList())));
		
		byte[] challenge = new byte[ThreadLocalRandom.current().nextInt(500, 2000)];
		ThreadLocalRandom.current().nextBytes(challenge);
		
		jo.addProperty("clear_challenge", new String(Base64.encodeBase64(challenge), MyDMAM.UTF8));
		try {
			jo.addProperty("ciphed_challenge", new String(Base64.encodeBase64(pool_manager.getProtocol().encrypt(challenge)), MyDMAM.UTF8));
		} catch (GeneralSecurityException e) {
			throw new RuntimeException("Can't cipher challenge", e);
		}
		
		return new DataBlock(this, jo);
	}
	
}
