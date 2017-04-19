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
package hd3gtv.mydmam.dareport;

import hd3gtv.mydmam.web.PlayBootstrap;
import play.data.validation.Validation;

public class AJS_DAR_AccountNew {
	
	String name;
	String email;
	String job;
	
	public void create() throws Exception {
		PlayBootstrap.validate(Validation.email("email", email), Validation.required("name", name), Validation.required("job", job));
		
		if (DARDB.get().getJobs().containsKey(job) == false) {
			throw new Exception("Can't found job " + job + " in configuration");
		}
		
		DARAccount account = new DARAccount();
		account.created_at = System.currentTimeMillis();
		account.email = email;
		account.job = job;
		account.name = name;
		account.save();
	}
	
}
