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
 * Copyright (C) hdsdi3g for hd3g.tv 2015-2016
 * 
*/

metadatas.getFileURL = function(file_hash, file_type, file_name) {
	var url = mydmam.routes.reverse("metadatafile");
	if (!url) {
		return "";
	}
	return url.replace("filehashparam1", file_hash).replace("typeparam2", file_type).replace("fileparam3", file_name);
};

