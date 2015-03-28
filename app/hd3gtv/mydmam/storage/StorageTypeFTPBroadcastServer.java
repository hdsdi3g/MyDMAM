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
package hd3gtv.mydmam.storage;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class StorageTypeFTPBroadcastServer extends StorageType {
	
	List<String> getPathPrefixName() {
		return Arrays.asList("ftp_broadcastserver");
	}
	
	Storage createStorage(String configuration_path, LinkedHashMap<String, ?> optional_storage_def) throws Exception {
		URILoginPasswordConfiguration configuration = new URILoginPasswordConfiguration(configuration_path, optional_storage_def);
		
		// TODO Auto-generated method stub
		return null;
	}
	
}
