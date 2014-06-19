/*
 * Copyright 2011 Netflix
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Forked for MyDMAM - Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.db;

import hd3gtv.log2.Log2;

import com.netflix.astyanax.retry.BoundedExponentialBackoff;
import com.netflix.astyanax.retry.RetryPolicy;

public class BoundedExponentialBackoffLog extends BoundedExponentialBackoff {
	
	private long baseSleepTimeMs;
	private long maxSleepTimeMs;
	private int max;
	private String keyspacename;
	
	public BoundedExponentialBackoffLog(String keyspacename, long baseSleepTimeMs, long maxSleepTimeMs, int max) {
		super(baseSleepTimeMs, maxSleepTimeMs, max);
		
		this.keyspacename = keyspacename;
		if (keyspacename == null) {
			throw new NullPointerException("\"keyspacename\" can't to be null");
		}
		
		this.baseSleepTimeMs = baseSleepTimeMs;
		this.maxSleepTimeMs = maxSleepTimeMs;
		this.max = max;
	}
	
	public void failure(Exception e) {
		if (e.getMessage().endsWith("InvalidRequestException(why:Keyspace '" + keyspacename + "' does not exist)")) {
			return;
		}
		super.failure(e);
		Log2.log.error("Lost Cassandra connection", e);
	}
	
	public RetryPolicy duplicate() {
		return new BoundedExponentialBackoffLog(keyspacename, baseSleepTimeMs, maxSleepTimeMs, max);
	}
	
}
