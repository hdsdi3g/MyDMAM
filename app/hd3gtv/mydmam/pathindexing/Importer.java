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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.mydmam.pathindexing;

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.db.Elasticsearch;

public abstract class Importer {
	
	public static final String ES_INDEX = "pathindex";
	public static final String ES_TYPE_FILE = "file";
	public static final String ES_TYPE_DIRECTORY = "directory";
	
	static {
		try {
			Elasticsearch.enableTTL(ES_INDEX, ES_TYPE_FILE);
			Elasticsearch.enableTTL(ES_INDEX, ES_TYPE_DIRECTORY);
		} catch (Exception e) {
			Log2.log.error("Can't to set TTL for ES", e);
		}
	}
	
	/**
	 * Don"t forget to add root path (Storage).
	 * @return number of imported elements
	 */
	protected abstract long doIndex(IndexingEvent elementpush) throws Exception;
	
	protected abstract String getName();
	
	/**
	 * @return in seconds, set 0 to disable.
	 */
	protected abstract long getTTL();
	
	public final long index() throws Exception {
		ElasticSearchPushElement push = new ElasticSearchPushElement(this);
		long result = doIndex(push);
		push.end();
		
		// if (push.l_elements_problems.size() > 0) {
		/**
		 * Alert ?
		 * Disabled this : there is too many bad file name
		 */
		// }
		
		return result;
	}
}
