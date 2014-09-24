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
 * create()
 * public use
 * @return polyvalent object 
 */
(function(creator, availabilities) {
	creator.currentmodal = null;
	
	creator.create = function() {
		creator.currentmodal = {
			/*test: 0,*/
			show : function(classname, items) {
				$("#uacreationmodal").remove();
				var content = '';
				content = content + '<div id="uacreationmodal" class="modal hide" tabindex="-1" role="dialog" aria-labelledby="uacreationmodalLabel" aria-hidden="true">';
				content = content + '<div class="modal-header">';
				content = content + '<button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>';
				content = content + '<h3 id="uacreationmodalLabel">' + i18n("useractions.newaction") + '</h3>';
				content = content + '</div>';
				content = content + '<div class="modal-body">';

				if ($.isArray(items)) {
					//TODO
				}
				var item = items;//TODO
				//this.test++;
				
				content = content + '<p>One fine body…</p>';
				content = content + '</div>';
				content = content + '<div class="modal-footer">';
				content = content + '<button class="btn" data-dismiss="modal" aria-hidden="true">Close</button>';
				content = content + '<button class="btn btn-primary">Save changes</button>';
				content = content + '</div>';
				content = content + '</div>';
				$("body").append(content);

				/*classname: classname,
				item.key,
				item.directory: is_directory,
				item.storagename: item_storagename,
				item.path: item_path*/
				
				$('#uacreationmodal').modal({});

				$('#uacreationmodal').on('hidden', function () {
					creator.currentmodal = null;
				});
			}
		};
		return creator.currentmodal;
	};
})(mydmam.useraction.creator, mydmam.useraction.availabilities);



//mydmam.useraction.url.create

