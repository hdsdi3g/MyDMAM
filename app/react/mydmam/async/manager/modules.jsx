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

mydmam.module.register("AppManager", {
	managerInstancesItems: function(item) {
		if (item["class"] != "AppManager") {
			return null;
		}

		var on_toogle = function(toogle) {
			manager.createInstanceAction("AppManager", item.key, {broker: toogle});
		};

		return (<div>
			<mydmam.async.BtnEnableDisable
					simplelabel={!manager.canCreateInstanceAction()}
					enabled={item.content.brokeralive}
					labelenabled={i18n("manager.items.AppManager.broker.on")}
					labeldisabled={i18n("manager.items.AppManager.broker.off")}
					onEnable={on_toogle}
					onDisable={on_toogle}
					reference={item.content.brokeralive ? "stop" : "start"} />
			&nbsp;
			<mydmam.async.LabelBoolean
				label_true={i18n("manager.items.AppManager.inoffhours")}
				label_false={i18n("manager.items.AppManager.innormalhours")}
				value={item.content.is_off_hours} />
			&nbsp;
			<mydmam.async.pathindex.reactDate
				date={item.content.next_updater_refresh_date}
				i18nlabel={i18n("manager.items.AppManager.next_updater_refresh_date")}
				style={{marginLeft: 0}} />
		</div>);
	},
	managerInstancesItemsDescr: function(item) {
		if (item["class"] != "AppManager") {
			return null;
		}
		return i18n("manager.items.AppManager.descr");
	},
});

manager.jobcreatorutils = {};
var jobcreatorutils = manager.jobcreatorutils;

jobcreatorutils.createButtonKitToogleEnableDisable = function(item, java_class) {
	var on_toogle_enable_disable = function(toogle) {
		manager.createInstanceAction(java_class, item.key, {activity: toogle});
	};

	return (<mydmam.async.BtnEnableDisable
		simplelabel={!manager.canCreateInstanceAction()}
		enabled={item.content.enabled}
		labelenabled={i18n("manager.items.jobcreatorutils.enabled")}
		labeldisabled={i18n("manager.items.jobcreatorutils.disabled")}
		onEnable={on_toogle_enable_disable}
		onDisable={on_toogle_enable_disable}
		reference={item.content.enabled ? "disable" : "enable"} />
	);
};

jobcreatorutils.createButtonKitCreateJob = function(item, java_class) {
		var on_do_create = function() {
			manager.createInstanceAction(java_class, item.key, {activity: "createjobs"});
		};

		return (<mydmam.async.SimpleBtn
				enabled={item.content.enabled}
				onClick={on_do_create}
				reference={item.content.enabled ? "disable" : "enable"}
				btncolor="btn-primary">
			<i className="icon-plus icon-white"></i> {i18n("manager.items.jobcreatorutils.createjob")}
		</mydmam.async.SimpleBtn>);
}

jobcreatorutils.getDeclarationList = function(declarations) {
	var declaration_list = [];
	for (var pos in declarations) {
		var declaration = declarations[pos];
		
		var context_list = [];
		for (var pos_ctx in declaration.contexts) {
			var context = declaration.contexts[pos_ctx];
			context_list.push(<div key={pos_ctx} style={{marginLeft: 10}}>
				{mydmam.async.broker.displayContext(context)}
			</div>);
		}
		declaration_list.push(<div key={pos} style={{marginLeft: 12}}>
			<strong>&bull; {declaration.job_name}</strong><br />
			{context_list}
		</div>);
	}
	return declaration_list;
}

mydmam.module.register("jobcreatorutils", {
	managerInstancesItemsDescr: function(item) {
		if (item["class"] != "CyclicJobCreator" &
			item["class"] != "TriggerJobCreator") {
			return null;
		}
		var declaration_list = [];
		for (var pos in item.content.declarations) {
			var declaration = item.content.declarations[pos];
			declaration_list.push(declaration.job_name);
		}
		return declaration_list.join(", ");
	},
});

mydmam.module.register("CyclicJobCreator", {
	managerInstancesItems: function(item) {
		if (item["class"] != "CyclicJobCreator") {
			return null;
		}
		var content = item.content;

		// setperiod xxxx
		// setnextdate xxxx

		return (<div>
			<strong>{content.long_name} :: {content.vendor_name}</strong><br />
			{jobcreatorutils.createButtonKitToogleEnableDisable(item, "CyclicJobCreator")} {jobcreatorutils.createButtonKitCreateJob(item, "CyclicJobCreator")}&nbsp;
			<mydmam.async.pathindex.reactDate date={content.next_date_to_create_jobs} i18nlabel={i18n("manager.items.CyclicJobCreator.next_date_to_create_jobs")} style={{marginLeft: 0}} />&nbsp;
			<mydmam.async.LabelBoolean label_true={i18n("manager.items.CyclicJobCreator.onlyoff")} label_false={i18n("manager.items.CyclicJobCreator.norestricted")} value={content.only_off_hours} />&nbsp;
			<br />
			{i18n("manager.items.CyclicJobCreator.period", content.period / 1000)}
			<br />
			{i18n("manager.items.jobcreatorutils.creator")} <mydmam.async.JavaClassNameLink javaclass={content.creator} />
			<div style={{marginTop: 16}}>
				<i className="icon-th-list"></i> {i18n("manager.items.jobcreatorutils.declarations")}<br />
				{jobcreatorutils.getDeclarationList(content.declarations)}
			</div>
		</div>);
	},
});

mydmam.module.register("TriggerJobCreator", {
	managerInstancesItems: function(item) {
		if (item["class"] != "TriggerJobCreator") {
			return null;
		}
		var content = item.content;

		return (<div>
			<strong>{content.long_name} :: {content.vendor_name}</strong><br />
			
			{jobcreatorutils.createButtonKitToogleEnableDisable(item, "TriggerJobCreator")} {jobcreatorutils.createButtonKitCreateJob(item, "TriggerJobCreator")}<br />
			
			{i18n("manager.items.jobcreatorutils.creator")} <mydmam.async.JavaClassNameLink javaclass={content.creator} />

			<div style={{marginTop: 16}}>
				<i className="icon-th-list"></i> {i18n("manager.items.TriggerJobCreator.context_hook")}<br />
				{mydmam.async.broker.displayContext(content.context_hook)}
				{i18n("manager.items.TriggerJobCreator.context_hook_trigger_key")} {mydmam.async.broker.displayKey(content.context_hook_trigger_key, true)}<br />
			</div>

			<div style={{marginTop: 16}}>
				<i className="icon-th-list"></i> {i18n("manager.items.jobcreatorutils.declarations")}<br />
				{jobcreatorutils.getDeclarationList(content.declarations)}
			</div>
		</div>);
	},
});


mydmam.module.register("WorkerNG", {
	managerInstancesItems: function(item) {
		if (item["class"] != "WorkerNG") {
			return null;
		}
		var content = item.content;

		var current_job_key = null;
		if (content.current_job_key != null) {
			current_job_key = mydmam.async.broker.displayKey(content.current_job_key, true);
		}

		var capablities_list = [];
		for (var pos in content.capablities) {
			var declaration = content.capablities[pos];
			capablities_list.push(<div key={pos} style={{marginLeft: 16}}>
				{mydmam.async.broker.displayContext(declaration)}
			</div>);
		}

		var specific = null;
		if (content.specific != null) {
			specific = (<div style={{marginTop: 10}}>
				<mydmam.async.JsonCode i18nlabel="manager.items.worker.specific" json={content.specific} />
			</div>);
		}

		return (<div>
			<strong>{content.long_name} :: {content.vendor}</strong> ({content.category}) <mydmam.async.JavaClassNameLink javaclass={content.worker_class} /><br />
			<span className="badge badge-info">{content.state}</span> {current_job_key}<br />
			<div style={{marginTop: 16}}>
				<i className="icon-th-list"></i> {i18n("manager.items.worker.capablities")}<br />
				{capablities_list}
			</div>
			{specific}
		</div>);

	},
	managerInstancesItemsDescr: function(item) {
		if (item["class"] != "WorkerNG") {
			return null;
		}
		return item.content.long_name;
	},
});

mydmam.module.register("WatchFolderEntry", {
	managerInstancesItems: function(item) {
		if (item["class"] != "WatchFolderEntry") {
			return null;
		}
		var content = item.content;

		var must_contain_div = null;
		if (content.must_contain.length > 0) {
			var must_contain = [];
			for (pos_mc in content.must_contain) {
				var mc = content.must_contain[pos_mc];
				must_contain.push(
					<span key={pos_mc + "_mc"} className="badge badge-important" style={{marginRight: 5}}>
						{i18n("manager.items.watchfolderentry.mustcontain." + mc)}
					</span>
				);
			}
			must_contain_div = (<div style={{marginTop: 8}}>
				{i18n("manager.items.watchfolderentry.mustcontain")} {must_contain}
			</div>);
		}

		var targets = [];
		for (pos_tg in content.targets) {
			var tg = content.targets[pos_tg];
			targets.push(<div key={pos_tg}>
				{i18n("manager.items.watchfolderentry.target.storage")} <span className="badge badge-warning">{tg.storage}</span><br />
				{i18n("manager.items.watchfolderentry.target.profile")} <span className="label label-inverse">{tg.profile}</span><br />
				{i18n("manager.items.watchfolderentry.target.prefixsuffix", tg.dest_file_prefix, tg.dest_file_suffix)}<br />
				<mydmam.async.LabelBoolean value={tg.keep_input_dir_to_dest} label_true={i18n("manager.items.watchfolderentry.keep_input_dir_to_dest.true")} label_false={i18n("manager.items.watchfolderentry.keep_input_dir_to_dest.false")} />
			</div>);
		}

		var target_block = null;
		if (content.targets.length === 1) {
			target_block = (<div style={{marginTop: 8}}>
				{targets}
			</div>);
		} else {
			target_block = (<div style={{marginTop: 16}}>
				<i className="icon-th-list"></i> {i18n("manager.items.watchfolderentry.targets")}<br />
				{targets}
			</div>);
		}

		var want_to_stop = null;
		if (content.want_to_stop) {
			want_to_stop = (<p>
				<mydmam.async.LabelBoolean
					value={true}
					label_true={i18n("manager.items.watchfolderentry.want_to_stop")}
					label_false={i18n("manager.items.watchfolderentry.want_to_stop")} />
			</p>);
		}

		var on_toogle_enable_disable = function(want_to_paused){
			manager.createInstanceAction("WatchFolderEntry", item.key, {paused: want_to_paused == "true"});
		};

		var btn_label_paused = i18n("manager.items.watchfolderentry.disabled");
		if (content.isalive) {
			btn_label_paused = (<mydmam.async.BtnEnableDisable
				simplelabel={!manager.canCreateInstanceAction()}
				enabled={!content.paused}
				labelenabled={i18n("manager.items.watchfolderentry.notpaused")}
				labeldisabled={i18n("manager.items.watchfolderentry.paused")}
				onEnable={on_toogle_enable_disable}
				onDisable={on_toogle_enable_disable}
				reference={content.paused ? "false" : "true"} />);
		}

		return (<div>
			<p>
				{btn_label_paused}
			</p>
			{i18n("manager.items.watchfolderentry.source")} <span className="badge badge-warning">{content.source_storage}</span>
			{want_to_stop}
			{must_contain_div}
			{target_block}<br />

			{i18n("manager.items.watchfolderentry.min_file_size")} <mydmam.async.pathindex.reactFileSize size={content.min_file_size} /><br />
			<span className="label">{i18n("manager.items.watchfolderentry.time_to_sleep_between_scans", content.time_to_sleep_between_scans / 1000)}</span><br />
			<span className="label">{i18n("manager.items.watchfolderentry.time_to_wait_growing_file", content.time_to_wait_growing_file / 1000)}</span><br />
		</div>);
	},
	managerInstancesItemsDescr: function(item) {
		if (item["class"] != "WatchFolderEntry") {
			return null;
		}
		return item.content.name;
	},
});

mydmam.module.register("TranscodeProfile", {
	managerInstancesItems: function(item) {
		if (item["class"] != "TranscodeProfile") {
			return null;
		}
		var content = item.content;

		var params = content.params;
		var cmd_line = [];
		var nb_car = params.length;
		var pos = 0;
		while (pos < params.length) {
			var first_index = params.indexOf("<%$", pos);
			var last_index = params.indexOf("%>", pos);
			if (pos != first_index) {
				cmd_line.push(params.substring(pos, first_index));
			}
			cmd_line.push(params.substring(first_index, last_index + 2));
			pos = last_index + 2;
		}
		var react_cmd_line = [];
		for (var pos in cmd_line) {
			var param = cmd_line[pos];
			var span_class = classNames({
				"cmd_var": param.indexOf("<%$") == 0,
			});
			react_cmd_line.push(<span key={pos} className={span_class}>
				{param}
			</span>);
		}

		var cdm = null;
		if (content.current_directory_mode != "none") {
			cdm = (<span className="label label-warning">{i18n("manager.items.TranscodeProfile.current_directory_mode." + content.current_directory_mode)}</span>);
		}

		var outputformat = null;
		if (content.outputformat != null) {
			outputformat = (<div style={{marginTop: 10}}>
				<mydmam.async.JsonCode i18nlabel="manager.items.TranscodeProfile.outputformat" json={content.outputformat} />
			</div>);
		}

		return (<div>
			<p>
				{i18n("manager.items.TranscodeProfile.executable_name")} <span className="label label-info">{content.executable_name}</span>
				<span style={{marginLeft: 5, marginRight: 3}}>&bull;</span>
				{i18n("manager.items.TranscodeProfile.extension")} <span className="label label-success">.{content.extension}</span><br />
				{cdm}
			</p>
			<code className="commandline"><span className="executable">{content.executable}</span> {react_cmd_line}</code>
			{outputformat}
		</div>);
	},
	managerInstancesItemsDescr: function(item) {
		if (item["class"] != "TranscodeProfile") {
			return null;
		}
		return item.key + " (" + item.content.executable_name + "/" + item.content.extension + ")";
	},
});

mydmam.module.register("FTPGroup", {
	managerInstancesItems: function(item) {
		if (item["class"] != "FTPGroup") {
			return null;
		}
		var content = item.content;

		var disabled = null;
		if (content.disabled_group) {
			disabled = (<p><span className="label label-error">{i18n("manager.items.FTPGroup.disabled_group")}</span></p>);
		}

		var freespace = null;
		if (content.last_free_space) {
			freespace = (<p>
				{i18n("manager.items.FTPGroup.last_free_space")} <mydmam.async.pathindex.reactFileSize size={content.last_free_space} />
				<br />
				{i18n("manager.items.FTPGroup.min_disk_space_before_warn")} <mydmam.async.pathindex.reactFileSize size={content.min_disk_space_before_warn} />
				<br />
				{i18n("manager.items.FTPGroup.min_disk_space_before_stop")} <mydmam.async.pathindex.reactFileSize size={content.min_disk_space_before_stop} />
			</p>);
		}

		var expirations = null;
		if (content.account_expiration_trash_duration > 0 | content.account_expiration_purge_duration > 0) {
			expirations = (<p>
				<span className="label">{i18n("manager.items.FTPGroup.account_expiration_trash_duration", content.account_expiration_trash_duration / 1000)}</span><br />
				<span className="label">{i18n("manager.items.FTPGroup.account_expiration_purge_duration", content.account_expiration_purge_duration / 1000)}</span><br />
				<span className="label">{i18n("manager.items.FTPGroup.account_expiration_based_on_last_activity." + content.account_expiration_based_on_last_activity)}</span>
			</p>);
		}
		var domain_isolation = null;
		if (content.domain_isolation) {
			domain_isolation = (<em>{i18n("manager.items.FTPGroup.domain_isolation")}</em>);
		}
		var short_activity_log = null;
		if (content.short_activity_log == false) {
			short_activity_log = (<div><span className="badge badge-important">{i18n("manager.items.FTPGroup.long_activity_log")}</span></div>);
		}

		var users_no_activity_log = null;
		if (content.users_no_activity_log.length > 0) {
			var users = [];
			for (var pos in content.users_no_activity_log) {
				var user = content.users_no_activity_log[pos];
				users.push(<span className="label label-inverse" style={{marginRight: 5}} key={pos}>{user}</span>);
			}
			users_no_activity_log = (<div>{i18n("manager.items.FTPGroup.users_no_activity_log")} {users}</div>);
		}

		return (<div>
			{disabled}
			<strong>{i18n("manager.items.FTPGroup.base_working_dir", content.base_working_dir)}</strong>{domain_isolation}<br />
			{i18n("manager.items.FTPGroup.pathindex_storagename")} <span className="label label-warning">{content.pathindex_storagename}</span><br />
			{freespace}
			{expirations}
			{short_activity_log}
			{i18n("manager.items.FTPGroup.trash_directory", content.trash_directory)}
			{users_no_activity_log}<br />
		</div>);
	},
	managerInstancesItemsDescr: function(item) {
		if (item["class"] != "FTPGroup") {
			return null;
		}
		return i18n("manager.items.FTPGroup.key", item.key);
	},
});

manager.executorStatus = function(exec) {
	var status = i18n("manager.items.ExecutorStatus.status.run");
	if (exec.shutdown) {
		status = i18n("manager.items.ExecutorStatus.status.shutdown");
	} else if (exec.terminating) {
		status = i18n("manager.items.ExecutorStatus.status.terminating");
	} else if (exec.terminated) {
		status = i18n("manager.items.ExecutorStatus.status.terminated");
	} 

	return (<div style={{maxWidth: "300px"}}>
		<p>
			<strong>{i18n("manager.items.ExecutorStatus")}</strong>
		</p>

		<table className="table table-bordered table-striped table-condensed table-hover">
		<tbody>
			<tr>
				<th>{i18n("manager.items.ExecutorStatus.status")}</th>
				<td>{status}</td>
			</tr>
			<tr>
				<th>{i18n("manager.items.ExecutorStatus.active")}</th>
				<td>{exec.active}</td>
			</tr>
			<tr>
				<th>{i18n("manager.items.ExecutorStatus.max_capacity")}</th>
				<td>{exec.max_capacity}</td>
			</tr>
			<tr>
				<th>{i18n("manager.items.ExecutorStatus.completed")}</th>
				<td>{exec.completed}</td>
			</tr>
			<tr>
				<th>{i18n("manager.items.ExecutorStatus.core_pool")}</th>
				<td>{exec.core_pool}</td>
			</tr>
			<tr>
				<th>{i18n("manager.items.ExecutorStatus.pool")}</th>
				<td>{exec.pool}</td>
			</tr>
			<tr>
				<th>{i18n("manager.items.ExecutorStatus.largest_pool")}</th>
				<td>{exec.largest_pool}</td>
			</tr>
			<tr>
				<th>{i18n("manager.items.ExecutorStatus.maximum_pool")}</th>
				<td>{exec.maximum_pool}</td>
			</tr>
			</tbody>
		</table>
	</div>);
};

mydmam.module.register("ClockProgrammedTasks", {
	managerInstancesItems: function(item) {
		if (item["class"] != "ClockProgrammedTasks") {
			return null;
		}
		var content = item.content;
		var tasks = [];

		var cpTask = function(task) {
			var retry_after = task.retry_after;
			if (retry_after > 0) {
				retry_after = <span>Retry: {mydmam.format.msecToHMSms(task.retry_after, false, false)} &bull;</span>;
			} else {
				retry_after = null;
			}

			var last_execute_date = task.last_execute_date;
			var last_execute_duration = null;
			if (last_execute_date > 0) {
				last_execute_date = <mydmam.async.pathindex.reactDate date={last_execute_date} i18nlabel={i18n("manager.items.ClockProgrammedTasks.previous_start_date")} />;
				last_execute_duration = <span>{i18n("manager.items.ClockProgrammedTasks.previous_duration", mydmam.format.msecToHMSms(task.last_execute_duration, false, false))}</span>
			} else {
				last_execute_date = null;
			}

			return (<div style={{marginBottom: "15px", marginLeft: "15px"}}>
				<strong>{task.name}</strong> &bull; {i18n("manager.items.ClockProgrammedTasks.start_at", mydmam.format.msecToHMSms(task.start_time_after_midnight, false, true))}
				<span style={{marginLeft: "6px"}}>
					{mydmam.async.broker.displayKey(task.key, true)} <mydmam.async.JavaClassNameLink javaclass={task.task_class} />
				</span>
				<br />
				{retry_after} <mydmam.async.LabelBoolean value={task.unschedule_if_error} inverse={false} label_true={i18n("manager.items.ClockProgrammedTasks.unschedule_if_error")} label_false={i18n("manager.items.ClockProgrammedTasks.not-unschedule_if_error")} />
				<br />
				<mydmam.async.pathindex.reactDate date={task.next_scheduled} i18nlabel={i18n("manager.items.ClockProgrammedTasks.next_scheduled")} />
				<br />
				{last_execute_date} {last_execute_duration}
			</div>);
		};

		for (var key in content.tasks) {
			tasks.push(<div key={key}>{cpTask(content.tasks[key])}</div>);
		}

		return (<div>
			<strong style={{marginBottom: "15px"}}>{i18n("manager.items.ClockProgrammedTasks.list")}</strong>
			{tasks}
			{manager.executorStatus(content.executor)}
		</div>);
	},
});
