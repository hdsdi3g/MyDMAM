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

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;

// TODO phase 2, remove this test class
public class MainClass {
	
	// TODO phase 2, check if UA capacity need no read only for storage
	
	public static void main(String[] args) throws Exception {
		/*new AppManager();
		
		InstanceStatus is = new InstanceStatus();
		is.populateFromThisInstance();
		DatabaseLayer.updateInstanceStatus(is);
		
		Log2Dump dump = new Log2Dump();
		dump.add("result", DatabaseLayer.getAllInstancesStatus());
		Log2.log.info("Do", dump);*/
		
		// builder.registerTypeAdapter(JobCreatorCyclic.class, JobCreatorCyclic.serializer);
		// builder.registerTypeAdapter(JobCreatorDeclarationCyclic.class, JobCreatorDeclarationCyclic.serializer);
		
		AppManager manager = new AppManager();
		
		CyclicJobCreator cy = new CyclicJobCreator(manager, 50, TimeUnit.SECONDS, true);
		JobContext context = new JobContext() {
			
			@Override
			public List<String> getNeededIndexedStoragesNames() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public JsonObject contextToJson() {
				return new JsonObject();
			}
			
			@Override
			public void contextFromJson(JsonObject json_object) {
				// TODO Auto-generated method stub
			}
		};
		cy.add("toto", context);
		System.out.println(AppManager.getGson().toJson(cy));
		
		System.exit(0);
		
		GsonThrowable gt1 = null;
		try {
			Class.forName("toto").newInstance();
		} catch (Exception e) {
			Exception e2 = new Exception("test", e);
			// e2.printStackTrace();
			gt1 = new GsonThrowable(e2);
		}
		
		String js = AppManager.getPrettyGson().toJson(gt1);
		System.out.println(js);
		System.err.println("===================================");
		System.err.println(AppManager.getPrettyGson().fromJson(js, GsonThrowable.class).getPrintedStackTrace());
		System.err.println("===================================");
	}
}
