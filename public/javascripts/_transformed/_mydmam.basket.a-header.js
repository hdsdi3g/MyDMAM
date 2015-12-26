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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
 */
/*jshint eqnull:true, loopfunc:true, shadow:true, jquery:true */

/**
 * Prepare consts and vars.
 */
(function(mydmam) {
	if(!mydmam.basket){mydmam.basket = {};}
	if(!mydmam.basket.content){mydmam.basket.content = {};}
	if(!mydmam.basket.content.backend ){mydmam.basket.content.backend  = {};}
	if(!mydmam.basket.allusers){mydmam.basket.allusers = {};}
	if(!mydmam.basket.url){mydmam.basket.url = {};}

	mydmam.basket.currentname = "";
	mydmam.basket.LOCALSTORAGE_CONTENT_KEYNAME = "basket-content";
	mydmam.basket.LOCALSTORAGE_LASTUPDATE_KEYNAME = "basket-lastupdate";
	mydmam.basket.CACHE_DURATION = 60;
	/** in sec */
})(window.mydmam);
