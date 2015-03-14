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
package hd3gtv.mydmam.web.stat;

import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.metadata.container.Containers;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.Explorer.DirectoryContent;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

import java.util.LinkedHashMap;
import java.util.List;

public class RequestResponseCache {
	// TODO cache
	
	private Explorer explorer;
	
	public RequestResponseCache() {
		explorer = new Explorer();
	}
	
	public LinkedHashMap<String, SourcePathIndexerElement> getelementByIdkeys(List<String> _ids) throws Exception {
		return explorer.getelementByIdkeys(_ids);
	}
	
	public long countDirectoryContentElements(String _id) {
		return explorer.countDirectoryContentElements(_id);
	}
	
	public LinkedHashMap<String, DirectoryContent> getDirectoryContentByIdkeys(List<String> _ids, int from, int size, boolean only_directories, String search) {
		return explorer.getDirectoryContentByIdkeys(_ids, from, size, only_directories, search);
	}
	
	public Containers getContainersByPathIndex(List<SourcePathIndexerElement> pathelements, boolean only_summaries) throws Exception {
		return ContainerOperations.getByPathIndex(pathelements, only_summaries);
	}
	
	public Containers getContainersByPathIndexId(List<String> pathelement_keys, boolean only_summaries) throws Exception {
		return ContainerOperations.getByPathIndexId(pathelement_keys, only_summaries);
	}
}
