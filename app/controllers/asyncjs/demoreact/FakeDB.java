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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * ONLY FOR DEV TESTS !
 */
public class FakeDB {
	
	private static final List<Comment> comments = Collections.synchronizedList(new ArrayList<Comment>());
	private static final Random random = new Random();
	
	static {
		NewComment v = new NewComment();
		v.author = "Pete Hunt";
		v.text = "This is one comment";
		add(v);
		
		v = new NewComment();
		v.author = "Jordan Walke";
		v.text = "This is *another* comment (" + new Date() + ")";
		add(v);
	}
	
	static void add(NewComment comment) {
		Comment c = new Comment();
		c.author = comment.author;
		c.text = comment.text;
		c.key = "k" + String.valueOf(random.nextInt(100000));
		comments.add(c);
	}
	
	static void delete(String key) {
		for (int pos = comments.size() - 1; pos > -1; pos--) {
			if (comments.get(pos).key.equals(key)) {
				comments.remove(pos);
				return;
			}
		}
	}
	
	static List<Comment> getAll() {
		return Collections.unmodifiableList(comments);
	}
	
}
