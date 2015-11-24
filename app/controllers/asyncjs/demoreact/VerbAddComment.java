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
package controllers.asyncjs.demoreact;

import hd3gtv.mydmam.web.AsyncJSControllerVerb;

public class VerbAddComment extends AsyncJSControllerVerb<NewComment, CommentList> {
	
	public String getVerbName() {
		return "add";
	}
	
	public Class<NewComment> getRequestClass() {
		return NewComment.class;
	}
	
	public Class<CommentList> getResponseClass() {
		return CommentList.class;
	}
	
	public CommentList onRequest(NewComment request) throws Exception {
		FakeDB.add(request);
		CommentList result = new CommentList();
		result.commentlist = FakeDB.getAll();
		return result;
	}
	
}
