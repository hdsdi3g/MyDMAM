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
package controllers.ajs;

import controllers.Check;
import controllers.asyncjs.demo.Comment;
import controllers.asyncjs.demo.CommentList;
import controllers.asyncjs.demo.DeleteComment;
import controllers.asyncjs.demo.FakeDB;
import controllers.asyncjs.demo.NewComment;
import hd3gtv.mydmam.web.AJSController;

public class DemosAsync extends AJSController {
	
	@Check("demoAsync3")
	public static CommentList edit(Comment request) {
		FakeDB.update(request);
		CommentList result = new CommentList();
		result.commentlist = FakeDB.getAll();
		return result;
	}
	
	@Check("demoAsync2")
	public static CommentList add(NewComment request) throws Exception {
		FakeDB.add(request);
		CommentList result = new CommentList();
		result.commentlist = FakeDB.getAll();
		return result;
	}
	
	@Check("demoAsync2")
	public static CommentList del(DeleteComment request) throws Exception {
		FakeDB.delete(request.key);
		CommentList result = new CommentList();
		result.commentlist = FakeDB.getAll();
		return result;
	}
	
	@Check("demoAsync2")
	public static CommentList get() throws Exception {
		CommentList response = new CommentList();
		response.commentlist = FakeDB.getAll();
		return response;
	}
	
	@Check("demoAsync2")
	public static void nothing() throws Exception {
		return;
	}
	
}
