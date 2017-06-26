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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/

/*
 * ============ Summaries =============
 */
manager.Summaries = createReactClass({
	render: function() {
		var items = [];
		for (var instance_ref in this.props.summaries) {
			items.push(<manager.InstanceSummary key={instance_ref} instance={this.props.summaries[instance_ref]} />);
		}
		return (
			<table className="table table-bordered table-striped table-condensed">
				<thead>
					<tr>
						<th>{i18n("manager.instance.host")}</th>
						<th>{i18n("manager.instance.manager")}</th>
						<th>{i18n("manager.instance.version")}</th>
						<th>{i18n("manager.instance.uptime")}</th>
						<th>{i18n("manager.instance.jvm")}</th>
						<th>{i18n("manager.instance.addr")}</th>
					</tr>
				</thead>
				<tbody>
					{items}
				</tbody>
			</table>
		);
	},
});

manager.InstanceSummary = createReactClass({
	render: function() {

		var addr = [];
		for (var pos in this.props.instance.host_addresses) {
			addr.push(<span key={pos}>&bull; {this.props.instance.host_addresses[pos]}<br /></span>);
		}

		return (<tr>
			<td>{this.props.instance.host_name}<br />
				<small className="muted">PID: {this.props.instance.pid}</small>
			</td>
			<td>
				{this.props.instance.instance_name}<br />
				<em>{this.props.instance.app_name}</em>
			</td>
			<td>
				{this.props.instance.app_version}
			</td>
			<td>
				<mydmam.async.pathindex.reactSinceDate i18nlabel="manager.instance.uptime" date={this.props.instance.starttime} />
			</td>
			<td>
				{this.props.instance.java_version}<br />
				<small className="muted">
					{this.props.instance.java_vendor}
				</small>
			</td>
			<td>{addr}</td>
		</tr>);
	},
});

/*
 * ============ Threads =============
 */

manager.ThreadsInstancesNavList = createReactClass({
	getInitialState: function() {
		return {
			instance_selected: null,
		};
	},
	onSelectInstance: function(ref) {
		this.props.onSelectInstance(ref);
		this.setState({instance_selected: ref});
	},
	onSelectItem: function(ref) {
		this.props.onSelectItem(ref);
	},
	render: function() {
		var instances = [];
		for (var key in this.props.summaries) {
			var summary = this.props.summaries[key];
			var li_class = classNames({
				active: this.state.instance_selected == key,
			});
			instances.push(<li key={key} className={li_class}>
				<manager.InstancesNavListElement reference={key} onSelect={this.onSelectInstance}>
					{summary.instance_name} <small>({summary.app_name})</small>
				</manager.InstancesNavListElement>
			</li>);
		}

		var item_list_i18n_title = null;
		if (this.props.item_list_i18n_title) {
			item_list_i18n_title = (<li className="nav-header">{i18n(this.props.item_list_i18n_title)}</li>);			
		}

		var items = [];
		for (var pos in this.props.items) {
			var item = this.props.items[pos];
			items.push(<li key={pos}>
				<manager.InstancesNavListElement reference={pos} onSelect={this.onSelectItem}>
					{item}
				</manager.InstancesNavListElement>
			</li>);
		}

		return (<div className="row-fluid">
			<div className="span4">
			 	<div className="well" style={{padding: "8px 0"}}>
				    <ul className="nav nav-list">
				    	<li className="nav-header">{i18n("manager.instancepane")}</li>
					    {instances}
				    	{item_list_i18n_title}
				    	{items}
				    </ul>
			    </div>
			</div>
			<div className="span8">
				{this.props.children}
			</div>
		</div>);
	},
});

manager.Threads = createReactClass({
	getInitialState: function() {
		return {
			thread_list: {},
			selected_instance: null,
		};
	},
	componentWillMount: function() {
		mydmam.async.request("instances", "allthreads", null, function(list) {
			this.setState({thread_list: list});
		}.bind(this));
	},
	onSelectInstance: function(ref) {
		this.setState({selected_instance: ref});
		$("html, body").scrollTop(0);
	},
	onSelectThread: function(thread_pos) {
		var absolute = ReactDOM.findDOMNode(this.refs[thread_pos]).getBoundingClientRect().y;
		$("html, body").scrollTop($("html, body").scrollTop() + absolute - 50);
	},
	onGotoTheTop: function(e) {
		e.preventDefault();
		$("html, body").scrollTop(0);
	},
	render: function() {
		if (this.state.selected_instance == null) {
			return (<manager.ThreadsInstancesNavList onSelectInstance={this.onSelectInstance} summaries={this.props.summaries} />);
		}
		var current_threads = this.state.thread_list[this.state.selected_instance];
		if (current_threads == null) {
			return (<manager.ThreadsInstancesNavList onSelectInstance={this.onSelectInstance} summaries={this.props.summaries} />);
		}

		current_threads.sort(function(a, b) {
			return a.id > b.id;
		});

		var items = [];
		var thread_names = [];
		for (var pos in current_threads) {
			var thread = current_threads[pos];
			var daemon = null;
			if (thread.isdaemon) {
				daemon = (<span><span className="badge badge-important">DAEMON</span>&nbsp;</span>);
			}

			var stacktrace = [];
			if (thread.execpoint == "") {
				stacktrace.push(<div key="-1">{thread.classname}</div>);
			} else {
				var execpoints = thread.execpoint.split("\n");
				for (var pos_execpoint in execpoints) {
					stacktrace.push(<div key={pos_execpoint}>{execpoints[pos_execpoint]}</div>);
				}
			}

			items.push(<div key={pos} ref={pos}>
				<h4>
					<a href={location.hash} onClick={this.onGotoTheTop}><i className=" icon-arrow-up" style={{marginRight: 5, marginTop: 5}}></i></a>
					{thread.name}
				</h4>
				<span className="badge badge-inverse">#{thread.id}</span>&nbsp;
				<span className="label label-info">{thread.state}</span>&nbsp;
				{daemon}
				<span className="label">Time: {thread.cpu_time_ms / 1000} sec</span>
				<br />
				<div className="thread-stacktrace">
					{stacktrace}
				</div>
			</div>);

			thread_names.push(<span key={pos}>{thread.id} &bull; {thread.name.substring(0, 50)}</span>);
		}

		return (<manager.ThreadsInstancesNavList
				summaries={this.props.summaries}
				onSelectInstance={this.onSelectInstance}
				onSelectItem={this.onSelectThread}
				items={thread_names}
				item_list_i18n_title="manager.threads">
			{items}
		</manager.ThreadsInstancesNavList>);
	},
});

/*
 * ============ Perfstats =============
 */
manager.Perfstats = createReactClass({
	getInitialState: function() {
		return {
			list: {},
			interval: null,
		};
	},
	componentWillMount: function() {
		this.refresh();
	},
	refresh: function() {
		mydmam.async.request("instances", "allperfstats", null, function(list) {
			this.setState({list: list});
		}.bind(this));
	},
	componentDidMount: function(){
		this.setState({interval: setInterval(this.refresh, 5000)});
	},
	componentWillUnmount: function() {
		if (this.state.interval) {
			clearInterval(this.state.interval);
		}
	},
	render: function() {
		var items = [];
		for (var instance_ref in this.state.list) {
			var summary = null;
			if (this.props.summaries[instance_ref]) {
				summary = this.props.summaries[instance_ref];
			}
			items.push(<manager.PerfstatsInstance key={instance_ref} instance={this.state.list[instance_ref]} summary={summary} />);
		}
		return (
			<div>{items}</div>
		);
	},
});

manager.PerfstatsInstance = createReactClass({
	render: function() {
		var instance = this.props.instance;

		var showMBsize= function(val) {
			if (val > 1000 * 1000 * 10) {
				return mydmam.format.number(Math.round(val / (1000 * 1000))) + " MB";
			} else {
				return mydmam.format.number(val) + " bytes";
			}
		}

		var cartridge_summary = null;
		var tr_uptime = null;
		if (this.props.summary != null) {
			var summary = this.props.summary;
			cartridge_summary = (<table className="table table-bordered table-striped table-condensed table-hover pull-right" style={{width: "inherit"}}>
				<tbody>
					<tr>
						<th>App name</th>
						<td>{summary.app_name}</td>
					</tr>
					<tr>
						<th>Host</th>
						<td>{summary.os_name} {summary.os_version} ({summary.os_arch})</td>
					</tr>
					<tr>
						<th>CPU Count</th>
						<td>{summary.cpucount}</td>
					</tr>
					<tr>
						<th>User</th>
						<td>{summary.user_name} {summary.user_language}_{summary.user_country} {summary.user_timezone}
						</td>
					</tr>
					<tr>
						<th>JVM uptime</th>
						<td>
							<mydmam.async.pathindex.reactSinceDate i18nlabel="manager.instance.uptime" date={summary.starttime} style={{marginLeft: 0}} />
						</td>
					</tr>
				</tbody>
			</table>);
				
				}

		var update_since = null;
		if (instance.now + 2000 < Date.now()) {
			update_since = (<mydmam.async.pathindex.reactSinceDate i18nlabel="manager.perfstats.since" date={instance.now} />);
		}

		var percent_free = (instance.freeMemory / instance.maxMemory) * 100;
		var percent_total = ((instance.totalMemory / instance.maxMemory) * 100) - percent_free;

		var heap_used = ((instance.heapUsed / instance.maxMemory) * 100);
		var non_heap_used = ((instance.nonHeapUsed / instance.maxMemory) * 100);

		var gc_table = [];
		for (var pos in instance.gc) {
			var gc = instance.gc[pos];
			gc_table.push(<tr key={pos}>
				<th>{gc.name}</th>
				<td>{gc.time / 1000} sec</td>
				<td>{gc.count} items</td>
			</tr>);
		}

		var os_table = null;
		if (instance.os) {
			var os = instance.os;
			os_table = (<table className="table table-bordered table-striped table-condensed table-hover" style={{width: "inherit"}}><tbody>
				<tr>
					<th>CPU load</th>
					<td>JVM process: {Math.round(os.getProcessCpuLoad * 100) / 100}</td>
					<td colSpan="2">System: {Math.round(os.getSystemCpuLoad * 100) / 100}</td>
				</tr>
				<tr>
					<th>JVM CPU time</th>
					<td colSpan="3">{Math.round(os.getProcessCpuTime / (1000 * 1000 * 100)) / 100} sec</td>
				</tr>
				<tr>
					<th>Physical memory</th>
					<td>Free: {showMBsize(os.getFreePhysicalMemorySize)}</td>
					<td>Total: {showMBsize(os.getTotalPhysicalMemorySize)}</td>
					<td>Used: {Math.floor(((os.getTotalPhysicalMemorySize - os.getFreePhysicalMemorySize) / os.getTotalPhysicalMemorySize) * 100)}%</td>
				</tr>
				<tr>
					<th>Swap</th>
					<td>Free: {showMBsize(os.getFreeSwapSpaceSize)}</td>
					<td>Total: {showMBsize(os.getTotalSwapSpaceSize)}</td>
					<td>Used: {Math.floor(((os.getTotalSwapSpaceSize - os.getFreeSwapSpaceSize) / os.getTotalSwapSpaceSize) * 100)}%</td>
				</tr>
				<tr>
					<td colSpan="4">Committed virtual memory size: {showMBsize(os.getCommittedVirtualMemorySize)}</td>
				</tr>
			</tbody></table>);
		}

		return (<div>
			<h4>
				{instance.instance_name}&nbsp;
				<small className="muted">{instance.pid}@{instance.host_name}</small>&nbsp;
				<span className="badge badge-important">
					Load {Math.round(instance.getSystemLoadAverage * 100)/100}
				</span>&nbsp;
				{update_since}
			</h4>
			
			{cartridge_summary}

			<div style={{marginLeft: "15px"}}>
				Memory free: {showMBsize(instance.freeMemory)}, total: {showMBsize(instance.totalMemory)}, max: {showMBsize(instance.maxMemory)}.<br />
			    <div className="progress" style={{width: "40%"}}>
					<div className="bar bar-warning" style={{width: percent_total + "%"}}></div>
					<div className="bar bar-success" style={{width: percent_free + "%"}}></div>
				</div>

				<p>
					Classes count unloaded: {instance.getUnloadedClassCount}, loaded: {instance.getLoadedClassCount}, total loaded: {instance.getTotalLoadedClassCount}, object pending finalization: {instance.getObjectPendingFinalizationCount}
				</p>
				
				Heap memory: <span className="text-info">{showMBsize(instance.heapUsed)}</span>, non heap: <span className="text-error">{showMBsize(instance.nonHeapUsed)}</span><br />

				<div className="progress" style={{width: "40%"}}>
					<div className="bar bar-info" style={{width: heap_used + "%"}}></div>
					<div className="bar bar-danger" style={{width: non_heap_used + "%"}}></div>
				</div>

				{os_table}
				
				<table className="table table-bordered table-striped table-condensed table-hover" style={{width: "inherit"}}>
					<tbody>
						{gc_table}
					</tbody>
				</table>
			</div>
			<hr />			
		</div>);
	},
});

/*
 * ============ Classpaths =============
 */
manager.Classpaths = createReactClass({
	getInitialState: function() {
		return {
			list: {},
		};
	},
	componentWillMount: function() {
		mydmam.async.request("instances", "allclasspaths", null, function(list) {
			this.setState({list: list});
		}.bind(this));
	},
	render: function() {
		var items = [];
		var declared_classpath = [];

		/**
		 * Mergue all CP for create a reference list.
		 */
		var current_classpath;
		for (var instance_ref in this.state.list) {
			current_classpath = this.state.list[instance_ref];
			for (var pos in current_classpath) {
				if (declared_classpath.indexOf(current_classpath[pos]) == -1) {
					declared_classpath.push(current_classpath[pos]);
				}
			}
		}

		for (pos in declared_classpath) {
			for (var instance_ref in this.state.list) {
				current_classpath = this.state.list[instance_ref];
				if (current_classpath.indexOf(declared_classpath[pos]) == -1) {
					var instance_info = instance_ref;
					if (this.props.summaries[instance_ref]) {
						if (this.props.summaries[instance_ref] !== "nope") {
							var summary = this.props.summaries[instance_ref];
							instance_info = summary.instance_name + " (" + summary.app_name + ") " + summary.host_name;
						} else {
							instance_info = i18n("manager.classpath.notfound") + " :: " + instance_ref;
						}
					} else {
						instance_info = i18n("manager.classpath.notfound") + " :: " + instance_ref;
					}

					items.push(<tr key={md5(declared_classpath[pos] + instance_ref)}>
						<td>{declared_classpath[pos]}</td>
						<td>{instance_info}</td>
					</tr>);
				}
			}
		}
		
		return (
			<table className="table table-bordered table-striped table-condensed">
				<thead>
					<tr>
						<th>{i18n("manager.classpath.missing")}</th>
						<th>{i18n("manager.classpath.missingin")}</th>
					</tr>
				</thead>
				<tbody>
					{items}
				</tbody>
			</table>
		);
	},
});

/*
 * ============ Lastjobs =============
 */
manager.Lastjobs = createReactClass({
	getInitialState: function() {
		return {
			list: {},
		};
	},
	componentWillMount: function() {
		mydmam.async.request("instances", "alldonejobs", null, function(list) {
			list.sort(function (a, b) {
				return a.update_date < b.update_date;
			});
			this.setState({list: list});
		}.bind(this));
	},
	render: function() {
		var broker = mydmam.async.broker;

		var joblist = [];
		for (var pos in this.state.list) {
			var job = this.state.list[pos];

			joblist.push(<div key={job.key}>
				<div className="donejoblistitem">
					<mydmam.async.JavaClassNameLink javaclass={job.context.classname} />
					<broker.JobCartridge job={job} required_jobs={[]} action_avaliable={null} onActionButtonClick={null} />
				</div>
			</div>);
		}

		return (<div>
			<p>
				<em>{i18n("manager.lastjobs.descr")}</em>
			</p>
			{joblist}
		</div>);
	},
}); 

/*
 * ============ Pending actions =============
 */
manager.PendingActions = createReactClass({
	getInitialState: function() {
		return {
			list: {},
			interval: null,
		};
	},
	componentWillMount: function() {
		this.refresh();
	},
	refresh: function() {
		mydmam.async.request("instances", "allpendingactions", null, function(list) {
			list.sort(function (a, b) {
				return a.created_at < b.created_at;
			});
			this.setState({list: list});
		}.bind(this));
	},
	componentDidMount: function(){
		this.setState({interval: setInterval(this.refresh, 5000)});
	},
	componentWillUnmount: function() {
		if (this.state.interval) {
			clearInterval(this.state.interval);
		}
	},
	render: function() {
		if (this.state.list.length == 0) {
			return (<p>
				<em>{i18n("manager.pendingactions.nothing")}</em>
			</p>);
		}
		
		var actionlist = [];
		for (var pos in this.state.list) {
			var action = this.state.list[pos];
			actionlist.push(<div key={action.key}>
				{mydmam.async.broker.displayKey(action.key, true)}
				<mydmam.async.pathindex.reactDate
					date={action.created_at}
					i18nlabel={i18n("manager.pendingactions.at")} />
				<span className="label label-inverse" style={{marginLeft: 10}}>
					{i18n("manager.pendingactions.by", action.caller)}
				</span>
				<div style={{marginTop: 10}}>
					<span className="label label-info">
						{i18n("manager.pendingactions.for", action.target_reference_key)}
					</span>
				</div>
				<h5>{i18n("manager.pendingactions.order")}</h5>
				<div>
					<code className="json" style={{marginTop: 10}}>
						<i className="icon-indent-left"></i>
						<span className="jsontitle"> {action.target_class_name} </span>
						{JSON.stringify(action.order, null, " ")}
					</code>
				</div>
				<hr />
			</div>);
		}

		return (<div>
			<h4>
				{i18n("manager.pendingactions.descr")}
			</h4>
			{actionlist}
		</div>);
	},
}); 

var ClusterStatusTable = createReactClass({
	render: function() {
		var report = this.props.report;
		var datas = report.datas;
		var colums_names = report.colums_names;

		var headers = [];
		//headers.push(<th key="_empty"></th>);
		for (colname in colums_names) {
			headers.push(<th key={colname}>{colums_names[colname]}</th>);
		}

		var lines = [];
		for (line in datas) {
			var data = datas[line];
			var cols = [];

			for (col in data.content) {
				cols.push(<td key={col}>
					{data.content[col]}
				</td>);
			}

			lines.push(<tr key={line + "_head"}>
				<th key="head">{data.name}</th>
				{cols}
			</tr>);

		}

		return (<table style={{width: "auto"}} className="table table-striped table-hover table-condensed table-bordered">
			<thead><tr>
				{headers}
			</tr></thead>
			<tbody>{lines}</tbody>
		</table>);
	},
}); 


manager.ClusterStatus = createReactClass({
	getInitialState: function() {
		return {
			status: {},
		};
	},
	componentDidMount: function() {
		mydmam.async.request("instances", "esclusterstatus", null, function(status) {
			/*list.sort(function (a, b) {
				return a.update_date < b.update_date;
			});*/
			this.setState({status: status});
		}.bind(this));
	},
	render: function() {
		var status = this.state.status;

		var result = [];
		if (status.last_status_reports) {
			for (var report in status.last_status_reports) {
				var report_name = status.last_status_reports[report].report_name;
				result.push(<div key={report}>
					<h3>{report_name}</h3>
					<ClusterStatusTable report={status.last_status_reports[report]} />
				</div>);
			}
		}

		return (<div>
			{result}
		</div>);
	},
}); 

manager.PlayServer = createReactClass({
	getInitialState: function() {
		return {
			report: {
				is_js_dev_mode: false,
				pluginstatus: "Loading...",
				ajs_process_time_log: null,
				jsressource_process_time_log: null,
			},
		};
	},
	componentWillMount: function() {
		mydmam.async.request("instances", "playserver", null, function(report) {
			this.setState({report: report});
		}.bind(this));
	},
	onClickBtnPurgeplaycache: function(e) {
		e.preventDefault();
		var request = {
			purgeplaycache: true, refreshlogconf: false, switchjsdevmode: false, purgejs: false, reset_process_time_log: false, toogle_process_time_log: false,
		};
		mydmam.async.request("instances", "playserverupdate", request, function(report) {
			this.setState({report: report});
		}.bind(this));
	},
	onClickBtnRefreshlogconf: function(e) {
		e.preventDefault();
		var request = {
			purgeplaycache: false, refreshlogconf: true, switchjsdevmode: false, purgejs: false, reset_process_time_log: false, toogle_process_time_log: false,
		};
		mydmam.async.request("instances", "playserverupdate", request, function(report) {
			this.setState({report: report});
		}.bind(this));
	},
	onClickBtnSwitchjsdevmode: function(e) {
		e.preventDefault();
		var request = {
			purgeplaycache: false, refreshlogconf: false, switchjsdevmode: true, purgejs: false, reset_process_time_log: false, toogle_process_time_log: false,
		};
		mydmam.async.request("instances", "playserverupdate", request, function(report) {
			this.setState({report: report});
		}.bind(this));
	},
	onClickBtnPurgejs: function(e) {
		e.preventDefault();
		var request = {
			purgeplaycache: false, refreshlogconf: false, switchjsdevmode: false, purgejs: true, reset_process_time_log: false, toogle_process_time_log: false,
		};
		mydmam.async.request("instances", "playserverupdate", request, function(report) {
			this.setState({report: report});
		}.bind(this));
	},
	onClickBtnResetProcessTimeLog: function(e) {
		e.preventDefault();
		var request = {
			purgeplaycache: false, refreshlogconf: false, switchjsdevmode: false, purgejs: false, reset_process_time_log: true, toogle_process_time_log: false,
		};
		mydmam.async.request("instances", "playserverupdate", request, function(report) {
			this.setState({report: report});
		}.bind(this));
	},
	onClickBtnToogleProcessTimeLog: function(e) {
		e.preventDefault();
		var request = {
			purgeplaycache: false, refreshlogconf: false, switchjsdevmode: false, purgejs: false, reset_process_time_log: false, toogle_process_time_log: true,
		};
		mydmam.async.request("instances", "playserverupdate", request, function(report) {
			this.setState({report: report});
		}.bind(this));
	},
	render: function() {
		var js_dev_mode = (<span className="text-success">{i18n("manager.playserver.in_js_prod")}</span>);
		var btn_label_js_dev_mode = i18n("manager.playserver.switchjsdevmode");

		if (this.state.report.is_js_dev_mode) {
			js_dev_mode = (<span className="text-error">{i18n("manager.playserver.in_js_dev")}</span>);
			btn_label_js_dev_mode = i18n("manager.playserver.switchjsprodmode")
		}

		var ajs_process_time_log = null;
		if (this.state.report.ajs_process_time_log) {
			ajs_process_time_log = (<div>
				<strong>{i18n("manager.playserver.ajs_process_time_log")}</strong><br />
				<pre>{this.state.report.ajs_process_time_log}</pre>
			</div>);
		}

		var jsressource_process_time_log = null;
		if (this.state.report.jsressource_process_time_log) {
			jsressource_process_time_log = (<div>
				<strong>{i18n("manager.playserver.jsressource_process_time_log")}</strong><br />
				<pre>{this.state.report.jsressource_process_time_log}</pre>
			</div>);
		}

		var style_p = {marginBottom: "1em"};

		return (<div>
			<div>
				<div className="btn-group" style={{marginBottom: "10px", marginLeft: 0, marginRight: "10px", }}>
					<button onClick={this.onClickBtnPurgeplaycache} className="btn">
						<i className="icon-fire"></i> {i18n("manager.playserver.purgeplaycache")}
					</button>
					<button onClick={this.onClickBtnRefreshlogconf} className="btn">
						<i className="icon-refresh"></i> {i18n("manager.playserver.refreshlogconf")}
					</button>
				</div>
				<div className="btn-group" style={{marginBottom: "10px", marginLeft: 0, marginRight: "10px", }}>
					<button onClick={this.onClickBtnSwitchjsdevmode} className="btn">
						<i className="icon-plane"></i> {btn_label_js_dev_mode}
					</button>
					<button onClick={this.onClickBtnPurgejs} className="btn">
						<i className="icon-fire"></i> {i18n("manager.playserver.purgejs")}
					</button>
				</div>
				<div className="btn-group" style={{marginBottom: "10px", marginLeft: 0, marginRight: "10px", }}>
					<button onClick={this.onClickBtnResetProcessTimeLog} className="btn">
						<i className="icon-trash"></i> {i18n("manager.playserver.ResetProcessTimeLog")}
					</button>
					<button onClick={this.onClickBtnToogleProcessTimeLog} className="btn">
						<i className="icon-off"></i> {i18n("manager.playserver.ToogleProcessTimeLog")}
					</button>
				</div>
			</div>
			<div style={style_p}>
				{js_dev_mode}
			</div>
			<div style={style_p}>
				<i className="icon-list-alt"></i> <a href="#debugpage">{i18n("manager.playserver.ajsdebugpage")}</a>
			</div>
			<pre>{this.state.report.pluginstatus}</pre>
			{ajs_process_time_log}
			{jsressource_process_time_log}
		</div>);
	},
}); 
