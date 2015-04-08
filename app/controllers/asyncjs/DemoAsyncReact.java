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
package controllers.asyncjs;

import hd3gtv.mydmam.web.AsyncJSController;
import hd3gtv.mydmam.web.AsyncJSControllerVerb;
import hd3gtv.mydmam.web.AsyncJSRequestObject;
import hd3gtv.mydmam.web.AsyncJSResponseObject;

import java.util.Arrays;
import java.util.List;

import controllers.asyncjs.demoreact.VerbAddComment;
import controllers.asyncjs.demoreact.VerbDelComment;
import controllers.asyncjs.demoreact.VerbGetCommentList;

public class DemoAsyncReact extends AsyncJSController {
	
	public String getRequestName() {
		return "demosasync";
	}
	
	@SuppressWarnings("unchecked")
	public <V extends AsyncJSControllerVerb<Rq, Rp>, Rq extends AsyncJSRequestObject, Rp extends AsyncJSResponseObject> List<V> getManagedVerbs() {
		return (List<V>) Arrays.asList(new VerbGetCommentList(), new VerbAddComment(), new VerbDelComment());
	}
	
}
