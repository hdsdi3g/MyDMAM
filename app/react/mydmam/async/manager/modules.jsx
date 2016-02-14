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

mydmam.module.register("CyclicJobCreator", {
	managerInstancesItems: function(item) {
		if (item["class"] != "CyclicJobCreator") {
			return null;
		}
		var content = item.content;

		var on_toogle_enable_disable = function(toogle) {
			manager.createInstanceAction("CyclicJobCreator", item.key, {activity: toogle});
		};

		var btn_label_enable_disable = (<mydmam.async.BtnEnableDisable
			simplelabel={!manager.canCreateInstanceAction()}
			enabled={content.enabled}
			labelenabled={i18n("manager.items.CyclicJobCreator.enabled")}
			labeldisabled={i18n("manager.items.CyclicJobCreator.disabled")}
			onEnable={on_toogle_enable_disable}
			onDisable={on_toogle_enable_disable}
			reference={content.enabled ? "disable" : "enable"} />);

		var on_do_create = function() {
			manager.createInstanceAction("CyclicJobCreator", item.key, {activity: "createjobs"});
		};

		// setperiod xxxx
		// setnextdate xxxx

		var declaration_list = [];
		for (var pos in content.declarations) {
			var declaration = content.declarations[pos];
			
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

		return (<div>
			<strong>{content.long_name} :: {content.vendor_name}</strong><br />
			{btn_label_enable_disable}&nbsp;
			<mydmam.async.pathindex.reactDate date={content.next_date_to_create_jobs} i18nlabel={i18n("manager.items.CyclicJobCreator.next_date_to_create_jobs")} style={{marginLeft: 0}} />&nbsp;
			<mydmam.async.LabelBoolean label_true={i18n("manager.items.CyclicJobCreator.onlyoff")} label_false={i18n("manager.items.CyclicJobCreator.norestricted")} value={content.only_off_hours} />&nbsp;
			<br />
			{i18n("manager.items.CyclicJobCreator.period", content.period / 1000)}
			<br />
			{i18n("manager.items.CyclicJobCreator.creator")} <mydmam.async.JavaClassNameLink javaclass={content.creator} />
			<div style={{marginTop: 16}}>
				<i className="icon-th-list"></i> {i18n("manager.items.CyclicJobCreator.declarations")}<br />
				{declaration_list}
			</div>
		</div>);
	},
	managerInstancesItemsDescr: function(item) {
		if (item["class"] != "CyclicJobCreator") {
			return null;
		}
		var content = item.content;

		var declaration_list = [];
		for (var pos in content.declarations) {
			var declaration = content.declarations[pos];
			declaration_list.push(declaration.job_name);
		}
		
		return declaration_list.join(", ");
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

		return (<div>
			{i18n("manager.items.watchfolderentry.source")} <span className="badge badge-warning">{content.source_storage}</span>
			{must_contain_div}
			{target_block}<br />

			{i18n("manager.items.watchfolderentry.min_file_size")} <mydmam.async.pathindex.reactFileSize size={content.min_file_size} /><br />
			{i18n("manager.items.watchfolderentry.time_to_sleep_between_scans")} <span className="label">{content.time_to_sleep_between_scans / 1000}</span><br />
			{i18n("manager.items.watchfolderentry.time_to_wait_growing_file")} <span className="label">{content.time_to_wait_growing_file / 1000}</span><br />
			{i18n("manager.items.watchfolderentry.want_to_stop")} {content.want_to_stop}
			{i18n("manager.items.watchfolderentry.isalive")} {content.isalive}
		</div>);
	},
	managerInstancesItemsDescr: function(item) {
		if (item["class"] != "WatchFolderEntry") {
			return null;
		}
		return item.content.name;
	},
});

/*
{
 "executable_name": "convert",
 "executable": "/.../bin/convert",
 "params": "<%$INPUTFILE%>[0] -thumbnail 64x64 -profile <%$ICCPROFILE%> null: ( -brightness-contrast 30x60 -size 128x128 -resize 64x64 pattern:CHECKERBOARD ) -compose Dst_Over -layers composite -strip -density 72x72 -units PixelsPerInch -interlace plane -sampling-factor 4:1:1 -quality 80 <%$OUTPUTFILE%>",
 "extension": "jpg",
 "outputformat": {
  "width": 64,
  "height": 64,
  "faststarted": false
 },
 "current_directory_mode": "none"
}
*/

mydmam.module.register("TranscodeProfile", {
	managerInstancesItems: function(item) {
		if (item["class"] != "TranscodeProfile") {
			return null;
		}
		var content = item.content;

		return (<div>
		</div>);
	},
	managerInstancesItemsDescr: function(item) {
		if (item["class"] != "TranscodeProfile") {
			return null;
		}
		return item.key + " (" + item.content.executable_name + "/" + item.content.extension + ")";
	},
});

