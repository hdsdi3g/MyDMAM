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
 * Pagination function
 */

/**
 * Prepare consts and vars.
 */
(function(mydmam) {
	if(!mydmam){mydmam = {};}
	mydmam.pagination = {};
})(window.mydmam);

/**
 * create
 * @param currentpage (int) Current page to display
 * @param pagecount (int) Page count
 * @param href (function(currentpage, pagecount), return String) href to display
 * @param name (String) for identify buttons
 * @return html (String)
 */
(function(pagination) {
	pagination.create = function(currentpage, pagecount, href, name) {
		if (pagecount < 2) {
			return "";
		}
		var html_btn_class = 'class="pagination-' + name + '"';
		
		var content = "";
		content = content + '<div class="pagination pagination-centered pagination-large">';
		content = content + '<ul>';
		
		if ((currentpage > 0) & (pagecount > 2)) {
			content = content + '<li><a href="' + href(currentpage - 1, pagecount) + '" ' + html_btn_class + ' data-currentpage="' + (currentpage - 1) + '"><span>&laquo;</span></a></li>';
		}
		if (pagecount < 9) {
			for (var pos = 0; pos < pagecount; pos++) {
				if (currentpage == pos) {
					content = content + '<li class="active"><span>' + (pos + 1) + '</span></li>';
				} else {
					content = content + '<li><a href="' + href(pos, pagecount) + '" ' + html_btn_class + ' data-currentpage="' + pos + '">' + (pos + 1) + '</a></li>';
				}
			}
		} else {
			if ((currentpage - 3) > -1) {
				if ((currentpage + 3) < pagecount) {
					for (var pos = (currentpage - 3); pos < currentpage; pos++) {
						content = content + '<li><a href="' + href(pos, pagecount) + '" ' + html_btn_class + ' data-currentpage="' + pos + '">' + (pos + 1) + '</a></li>';
					}
					content = content + '<li class="active"><span>' + (pos + 1) + '</span></li>';
					for (var pos = (currentpage + 1); pos <= (currentpage + 3); pos++) {
						content = content + '<li><a href="' + href(pos, pagecount) + '" ' + html_btn_class + ' data-currentpage="' + pos + '">' + (pos + 1) + '</a></li>';
					}
				} else {
					for (var pos = (currentpage - 3); pos < pagecount; pos++) {
						if (currentpage == pos) {
							content = content + '<li class="active"><span>' + (pos + 1) + '</span></li>';
						} else {
							content = content + '<li><a href="' + href(pos, pagecount) + '" ' + html_btn_class + ' data-currentpage="' + pos + '">' + (pos + 1) + '</a></li>';
						}
					}
				}
			} else {
				for (var pos = 0; pos <= (currentpage + 3); pos++) {
					if (currentpage == pos) {
						content = content + '<li class="active"><span>' + (pos + 1) + '</span></li>';
					} else {
						content = content + '<li><a href="' + href(pos, pagecount) + '" ' + html_btn_class + ' data-currentpage="' + pos + '">' + (pos + 1) + '</a></li>';
					}
				}
			}
		}
		if (((currentpage+1) < pagecount) & (pagecount > 2)) {
			content = content + '<li><a href="' + href(currentpage + 1, pagecount) + '" ' + html_btn_class + ' data-currentpage="' + (currentpage + 1) + '"><span>&raquo;</span></a></li>';
		}

		content = content + '</ul>';
		content = content + '</div>';
		return content;
	};
})(window.mydmam.pagination);

/**
 * addevents
 * @param name (String) previously identify in create()
 * @param onclickevent (function(currentpage), return JS event) add events for all buttons in all paginations in webpage.
 * @return null
 */
(function(pagination) {
	pagination.addevents = function(onclickevent, name) {
		//console.log('a.pagination-' + name);
		$('a.pagination-' + name).each(function() {
			$(this).click(onclickevent($(this).data("currentpage")));
		});
		return null;
	};
})(window.mydmam.pagination);
