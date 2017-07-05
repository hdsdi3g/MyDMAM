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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
 */

/**
 * Look and feel API: get colors (and other stuffs) exposed in themes.
 */
(function(mydmam) {
	if(!mydmam.lookandfeel){mydmam.lookandfeel = {};}
})(window.mydmam);

(function(lookandfeel) {

	/**
	 * Declaration zone
	 */
	lookandfeel.declared_themes = {};

	var addTheme = function(theme_name, content) {
		lookandfeel.declared_themes[theme_name] = content;
	};

	addTheme("base", {
		bgnd1: "#FFF",
		bgnd2: "#F5F5F5",
		frt1: "#000",
		frt2: "#FFF",
	});

	/**
	 * API Zone
	 */
	var current_theme = {};

	/**
	 * @return null if visual_ref is null
	 */
	lookandfeel.get = function(visual_ref) {
		if (!visual_ref) {
			return null;
		}
		return current_theme[visual_ref];
	};

	lookandfeel.switchTheme = function(theme_name) {
		current_theme = lookandfeel.declared_themes[theme_name];
	};

	lookandfeel.switchTheme("base");

	//TODO list themes + pvw
})(window.mydmam.lookandfeel);

