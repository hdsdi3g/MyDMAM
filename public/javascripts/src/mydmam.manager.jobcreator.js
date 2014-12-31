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
			if (jQuery.isEmptyObject(context.content) === false) {
				content = content + ' &bull; <code class="json"><i class="icon-indent-left"></i>'; 
				content = content + JSON.stringify(context.content, null, "\t").nl2br(); 
				content = content + '</code>'; 
			} else {
				content = content + '<br>'; 
			}
		} else {
			content = content + '<br>'; 
		}
		if (context.neededstorages) {
			content = content + i18n('manager.jobcreator.context.neededindexedstoragesnames') + ' ';
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
	jobcreator.prepareGenericTable = function(instances, declared_key_name, specific_cols_headers, specific_row, target_class_name) {
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
			
			if (mydmam.manager.hasInstanceAction()) {
				content = content + '<td>';
				content = content + '<div class="btn-group">';
				if (creator.enabled) {
					content = content + '<button class="btn btn-mini btnmgraction btn-danger" ';
					content = content + 'data-target_class_name="' + target_class_name + '" ';
					content = content + 'data-order_key="activity" ';
					content = content + 'data-order_value="disable" ';
					content = content + 'data-target_reference_key="' + creator.reference_key + '" ';
					content = content + '><i class="icon-stop icon-white"></i> ' + i18n("manager.jobcreator.action.disable") ;
					content = content + '</button>';
				} else {
					content = content + '<button class="btn btn-mini btnmgraction btn-success" ';
					content = content + 'data-target_class_name="' + target_class_name + '" ';
					content = content + 'data-order_key="activity" ';
					content = content + 'data-order_value="enable" ';
					content = content + 'data-target_reference_key="' + creator.reference_key + '" ';
					content = content + '><i class="icon-play icon-white"></i> ' + i18n("manager.jobcreator.action.enable") ;
					content = content + '</button>';
				}
				content = content + '<button class="btn btn-mini btnmgraction btn-primary" ';
				content = content + 'data-target_class_name="' + target_class_name + '" ';
				content = content + 'data-order_key="activity" ';
				content = content + 'data-order_value="createjobs" ';
				content = content + 'data-target_reference_key="' + creator.reference_key + '" ';
				content = content + '><i class="icon-play-circle icon-white"></i> ' + i18n("manager.jobcreator.action.createjobs") ;
				content = content + '</button>';
				
				content = content + '</div>'; //btn-group
				content = content + '</td>';
			}
			
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
				var creator = creators[pos_crt];
				content = content + '<tr>';

				content = content + '<td>';
				content = content + '<strong>' +instance.app_name + '</strong>&nbsp;&bull; ';
				content = content + instance.instance_name + '<br>';
				content = content + '<small>' + instance.host_name + '</small>';
				content = content + '<span class="pull-right">' + mydmam.manager.jobs.view.displayKey(creator.reference_key, true) + '</span>';
				content = content + '</td>';

				content = content + prepareCreatorRow(creator);
				content = content + '</tr>';
			}
			return content;
		};
		
		var content = '';
		var is_empty = true;
		content = content + '<table class="table table-striped table-bordered table-hover table-condensed setdatatable">';
		content = content + '<thead>';
		content = content + '<th>' + i18n('manager.jobcreator.th.instance') + '</th>';
		content = content + '<th>' + i18n('manager.jobcreator.th.name') + '</th>';
		
		if (specific_cols_headers) {
			for (var pos_sch = 0; pos_sch < specific_cols_headers.length; pos_sch++) {
				content = content + '<th>' + specific_cols_headers[pos_sch] + '</th>';
			}
		}
		
		content = content + '<th>' + i18n('manager.jobcreator.th.declarations') + '</th>';
		if (mydmam.manager.hasInstanceAction()) {
			content = content + '<th>' + i18n('manager.jobcreator.th.actions') + '</th>';
		}
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
		var target_class_name = 'CyclicJobCreator';
		var specific_row = function(creator) {
			var content = '<td>';
			if (creator.enabled) {
				content = content + '<span class="label">';
				content = content + mydmam.format.timeAgo(creator.period + new Date().getTime(), 'manager.jobcreator.cyclic.periodvalue', 'manager.jobcreator.cyclic.periodvalue');
				content = content + '</span>';

				content = content + '<span class="pull-right">';
				content = content + '<div class="btn-group">';
				var addButton = function(order_key, order_value, icon1, icon2, label) {
					content = content + '<button class="btn btn-mini btnmgraction" ';
					content = content + 'data-target_class_name="' + target_class_name + '" ';
					content = content + 'data-order_key="' + order_key + '" ';
					content = content + 'data-order_value="' + order_value + '" ';
					content = content + 'data-target_reference_key="' + creator.reference_key + '" ';
					content = content + '><i class="' + icon1 + '"></i> ';
					if (icon2) {
						content = content + '<i class="' + icon2 + '"></i> ';
					}
					if (label) {
						content = content + i18n(label);
					}
					
					content = content + '</button>';
				};
				
				addButton("setperiod", Math.round(creator.period / 2), 'icon-repeat', null, '/2');
				addButton("setperiod", creator.period * 2, 'icon-repeat', null, 'x2');
				addButton("setnextdate", creator.next_date_to_create_jobs + creator.period, 'icon-time', 'icon-fast-forward', null);
				content = content + '</div>';
				content = content + '</span>';
				
				content = content + '<br><span class="label">';
				if (creator.next_date_to_create_jobs) {
					content = content + mydmam.format.timeAgo(creator.next_date_to_create_jobs, 'manager.jobcreator.cyclic.nextdate', 'manager.jobcreator.cyclic.nextdate');
				} else {
					content = content + i18n('manager.jobcreator.cyclic.nonextdate');
				}
				content = content + '</span>';
			}
			
			content = content + '</td>';
			return content;
		};
		
		return jobcreator.prepareGenericTable(instances, "declared_cyclics", 
				[i18n('manager.jobcreator.cyclic.period')], specific_row, target_class_name);
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
				[i18n('manager.jobcreator.trigger.hook')], specific_row, 'TriggerJobCreator');
	};
})(window.mydmam.manager.jobcreator);

