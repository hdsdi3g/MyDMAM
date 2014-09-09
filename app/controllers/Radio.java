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

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.web.stat.Stat;
import hd3gtv.mydmam.web.stat.StatElement;

import java.util.List;

import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;

@With(Secure.class)
public class Radio extends Controller {
	
	@Check("Radio")
	public static void index() {
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("radio.pagename"));
		List<String> items = Configuration.global.getValues("radio", "instantkey", null);
		String content = "{}";
		if (items == null) {
			render(content);
		}
		if (items.isEmpty()) {
			render(content);
		}
		String[] pathinstantkeys = new String[items.size()];
		for (int pos = 0; pos < pathinstantkeys.length; pos++) {
			pathinstantkeys[pos] = items.get(pos);
		}
		Stat stat = new Stat(pathinstantkeys, new String[] { StatElement.SCOPE_PATHINFO, StatElement.SCOPE_MTD_SUMMARY }, null);
		content = stat.toJSONString();
		render(content);
	}
}