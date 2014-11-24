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

// TODO remove this test class
public class MainClass {
	
	public static void main(String[] args) throws Exception {
		/**
		 * TODO centralize status and action
		 * - isAlive functions
		 * - remote exec actions to workers (start, stop, change cyclic times...)
		 * - workers status (+ add current Thread (exec class & line))
		 * TODO get all declared workers and all configured workers
		 * TODO push new job
		 * Serialize job context
		 * TODO get a waiting job an attribute to an worker
		 * Deserialize context
		 * TODO manage jobs lifecycle (too old, error...)
		 */
		
		/*new AppManager();
		
		InstanceStatus is = new InstanceStatus();
		is.populateFromThisInstance();
		DatabaseLayer.updateInstanceStatus(is);
		
		Log2Dump dump = new Log2Dump();
		dump.add("result", DatabaseLayer.getAllInstancesStatus());
		Log2.log.info("Do", dump);*/
		
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
