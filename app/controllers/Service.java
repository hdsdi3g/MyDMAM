/*
 * This file is part of MyDMAM
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package controllers;

import hd3gtv.javasimpleservice.IsAlive;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;

@With(Secure.class)
public class Service extends Controller {
	
	public static void index() {
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("service.pagename"));
		render();
	}
	
	public static void laststatusworkers() throws Exception {
		renderJSON(IsAlive.getLastStatusWorkers().toJSONString());
	}
	
}