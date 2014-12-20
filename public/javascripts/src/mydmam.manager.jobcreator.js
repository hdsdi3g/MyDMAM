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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
/*jshint eqnull:true, loopfunc:true, shadow:true, jquery:true */


/**
 * contextToString(instances)
 */
(function(jobcreator) {
	jobcreator.contextToString = function(context, show_json_content) {
		var content = '';
		var class_name = context.classname.substring(context.classname.lastIndexOf('.') + 1, context.classname.length);
		content = content + '<abbr title="' + context.classname + '">' + class_name + '.class</abbr>';
		if (show_json_content) {
			content = content + ' &bull; <code>' + JSON.stringify(context.content) + '</code>';
		}
		if (context.neededstorages) {
			content = content + '<br />' + i18n('manager.jobcreator.context.neededindexedstoragesnames') + ' ';
			for (var pos_nstr = 0; pos_nstr < context.neededstorages.length; pos_nstr++) {
				content = content + context.neededstorages[pos_nstr];
				if (pos_nstr + 1 < context.neededstorages.length) {
					content = content + ', ';
				}
			}
		}
		return content;
	};
})(window.mydmam.manager.jobcreator);

/**
 * prepareGenericTable(instances)
 */
(function(jobcreator) {
	jobcreator.prepareGenericTable = function(instances, declared_key_name, specific_cols_headers, specific_row) {
		var prepareCreatorRow = function(creator) {
			var content = '';
			content = content + '<td>';
			if (creator.enabled === false) {
				content = content + '<span class="badge badge-important">' + i18n('manager.jobcreator.disabled') + '</span> ';
			}
			content = content + '<abbr title="' + i18n('manager.jobcreator.createdby') + ' ' + creator.creator + '">' + creator.long_name + '</abbr><br />';
			content = content + '<small>' + creator.vendor_name + '</small>';
			content = content + '</td>';
			
			if (specific_row) {
				content = content + specific_row(creator);
			}
			
			content = content + '<td><small>';
			for (var pos_d = 0; pos_d < creator.declarations.length; pos_d++) {
				var declaration = creator.declarations[pos_d];
				content = content + '<ul style="margin-left: 0px; margin-bottom: 0px;">';
				content = content + '<li>' + declaration.job_name + '</li>';

				content = content + '<li><ul>';
				for (var pos_ctx = 0; pos_ctx < declaration.contexts.length; pos_ctx++) {
					content = content + '<li>' + jobcreator.contextToString(declaration.contexts[pos_ctx], true) + '</li>';
				}
				content = content + '</ul></li>';
				
				content = content + '</ul>';
			}
			content = content + '</small></td>';
			return content;
		};

		var prepareInstance = function(instance) {
			var creators = instance[declared_key_name];
			if (!creators) {
				return null;
			}
			if (creators.length === 0) {
				return null;
			}
			var content = '';
			
			for (var pos_crt = 0; pos_crt < creators.length; pos_crt++) {
				content = content + '<tr>';

				content = content + '<td>';
				content = content + '<strong>' +instance.app_name + '</strong>&nbsp;&bull; ';
				content = content + instance.instance_name + '<br>';
				content = content + '<small>' + instance.host_name + '</small>';
				content = content + '</td>';

				content = content + prepareCreatorRow(creators[pos_crt]);
				content = content + '</tr>';
			}
			return content;
		};
		
		var content = '';
		var is_empty = true;
		content = content + '<table class="table table-striped table-bordered table-hover table-condensed setdatatableAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA">';
		content = content + '<thead>';
		content = content + '<th>' + i18n('manager.jobcreator.th.instance') + '</th>';
		content = content + '<th>' + i18n('manager.jobcreator.th.name') + '</th>';
		
		if (specific_cols_headers) {
			for (var pos_sch = 0; pos_sch < specific_cols_headers.length; pos_sch++) {
				content = content + '<th>' + specific_cols_headers[pos_sch] + '</th>';
			}
		}
		
		content = content + '<th>' + i18n('manager.jobcreator.th.declarations') + '</th>';
		content = content + '</thead>';
		content = content + '<tbody>';
		for (var pos_i = 0; pos_i < instances.length; pos_i++) {
			var instance = instances[pos_i];
			var instance_content = prepareInstance(instance); 
			if (instance_content != null) {
				content = content + instance_content;
				is_empty = false;
			}
		}
		content = content + '</tbody>';
		content = content + '</table>';
		
		if (is_empty) {
			return '<div class="alert alert-info"><strong>' + i18n('manager.workers.empty') + '</strong></div>';
		} else {
			return content;
		}
	};
})(window.mydmam.manager.jobcreator);


/**
 * prepareCyclic(instances)
 */
(function(jobcreator) {
	jobcreator.prepareCyclic = function(instances) {
		var specific_row = function(creator) {
			var content = '<td>';
			content = content + i18n('manager.jobcreator.cyclic.periodvalue', (creator.period / 1000)) + '<br />';
			
			content = content + '<span class="label">';
			var next_date = mydmam.format.fulldate(creator.next_date_to_create_jobs);
			if (next_date) {
				content = content + i18n('manager.jobcreator.cyclic.nextdate') + ' ' + next_date;
			} else {
				content = content + i18n('manager.jobcreator.cyclic.nonextdate');
			}
			content = content + '</span>';
			
			content = content + '</td>';
			return content;
		};
		
		return jobcreator.prepareGenericTable(instances, "declared_cyclics", 
				[i18n('manager.jobcreator.cyclic.period')], specific_row);
	};
})(window.mydmam.manager.jobcreator);

/**
 * prepareTriggers(instances)
 */
(function(jobcreator) {
	jobcreator.prepareTriggers = function(instances) {
		var specific_row = function(creator) {
			var content = '<td>';
			content = content + jobcreator.contextToString(creator.context_hook, false);
			content = content + '</td>';
			return content;
		};
		
		return jobcreator.prepareGenericTable(instances, "declared_triggers", 
				[i18n('manager.jobcreator.trigger.hook')], specific_row);
	};
})(window.mydmam.manager.jobcreator);
