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

/**
 * Transform "application/x-dummy" to "application-dummy", and translate it.
 */
pathindex.mtdTypeofElement = function(mtd_element) {
	if (!mtd_element) {
		return "";
	}
	if (!mtd_element.mimetype) {
		return "";
	}
	var element = mtd_element.mimetype;
	var element_type = element.substr(0, element.indexOf('/'));
	var element_subtype = element.substr(element.indexOf('/') + 1);

	if (element_subtype.startsWith("x-")) {
		element_subtype = element_subtype.substr(2);
	}
	element = element_type + "-" + element_subtype;

	var translated_element = i18n("mime." + element);
	if (translated_element.startsWith("mime.")) {
		translated_element = translated_element.substr(5);
	}
	return translated_element;
};