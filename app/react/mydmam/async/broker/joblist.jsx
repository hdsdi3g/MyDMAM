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

broker.Jobs = React.createClass({
	getInitialState: function() {
		return {
			selected_tab: 0,
			joblist: {},
			since: 0,
			interval: null,
		};
	},
	onTabChange: function(new_tab, selected_status) {
		this.setState({selected_tab: new_tab});
	},
	componentWillMount: function() {
		this.updateJobList();
	},
	componentDidMount: function(){
		this.setState({interval: setInterval(this.updateJobList, 3000)});// SET OFF FOR DISABLE
	},
	componentWillUnmount: function() {
		if (this.state.interval) {
			clearInterval(this.state.interval);// SET OFF FOR DISABLE
		}
	},
	updateJobList: function() {
		mydmam.async.request("broker", "list", {since: this.state.since}, function(data) {
			if (this.state.since == 0) {
				this.setState({joblist: data, since: Date.now()});
			} else {
				if (Object.keys(data).length == 0){
					return;
				}
				this.setState({
					joblist: jQuery.extend({}, this.state.joblist, data),
					since: Date.now()
				});
			}
		}.bind(this));
	},
	render: function(){
		return (
			<mydmam.async.PageHeaderTitle title={i18n("Job list")} fluid="true">
				<broker.NavTabs selected_tab={this.state.selected_tab} onTabChange={this.onTabChange} joblist={this.state.joblist} />
				<broker.JobListCartridges joblist={this.state.joblist} selected_tab={this.state.selected_tab} />
			</mydmam.async.PageHeaderTitle>
		);
	},
});

mydmam.routes.push("broker-Jobs", "broker", broker.Jobs, [{name: "broker", verb: "list"}]);	

broker.status = {
	WAITING: {
		tab: 0,	i18n_tab_name: "manager.jobs.status.WAITING",			badge_class: null,				btns: ["hipriority", "cancel", "postponed", "noexpiration", "delete"],	},
	PREPARING: {
		tab: 1,	i18n_tab_name: "manager.jobs.status.PREPARING",			badge_class: "badge-warning",	btns: [],	},
	PROCESSING: {
		tab: 2,	i18n_tab_name: "manager.jobs.status.PROCESSING",		badge_class: "badge-warning",	btns: ["stop"],	},
	DONE: {
		tab: 3,	i18n_tab_name: "manager.jobs.status.DONE",				badge_class: "badge-success",	btns: ["setinwait", "postponed", "noexpiration", "delete"],	},
	ERROR: {
		tab: 4,	i18n_tab_name: "manager.jobs.status.ERROR",				badge_class: "badge-important",	btns: ["cancel", "setinwait", "postponed", "noexpiration", "delete"],	},
	TOO_OLD: {
		tab: 5,	i18n_tab_name: "manager.jobs.status.TOO_OLD",			badge_class: "badge-info",		btns: ["cancel", "setinwait", "postponed", "noexpiration", "delete"],	},
	TOO_LONG_DURATION: {
		tab: 6,	i18n_tab_name: "manager.jobs.status.TOO_LONG_DURATION",	badge_class: "badge-info",		btns: ["cancel", "setinwait", "postponed", "noexpiration", "delete"],	},
	STOPPED: {
		tab: 7,	i18n_tab_name: "manager.jobs.status.STOPPED",			badge_class: "badge-important",	btns: ["cancel", "setinwait", "postponed", "noexpiration", "delete"],	},
	CANCELED: {
		tab: 8,	i18n_tab_name: "manager.jobs.status.CANCELED",			badge_class: null,				btns: ["setinwait", "postponed", "noexpiration", "delete"],	},
	POSTPONED: {
		tab: 9,	i18n_tab_name: "manager.jobs.status.POSTPONED",			badge_class: null,				btns: ["setinwait", "noexpiration", "delete"],	},
};

/*		
		var btndelete = createBtn('', 'icon-trash', 'manager.jobs.btn.delete', 'delete');
		var btnstop = createBtn('', 'icon-stop ', 'manager.jobs.btn.stop', 'stop');
		var btnsetinwait = createBtn('', 'icon-inbox', 'manager.jobs.btn.setinwait', 'setinwait');
		var btncancel = createBtn('', 'icon-off', 'manager.jobs.btn.cancel', 'cancel');
		var btnhipriority = createBtn('', 'icon-warning-sign', 'manager.jobs.btn.hipriority', 'hipriority');
		var btnpostponed = createBtn('', 'icon-step-forward', 'manager.jobs.btn.postponed', 'postponed');
		
		var btnnoexpiration = '';
		if ((job.expiration_date - job.create_date) < mydmam.manager.jobs.default_max_expiration_time) {
			btnnoexpiration = createBtn('', 'icon-calendar', 'manager.jobs.btn.noexpiration', 'noexpiration');
		}
*/

broker.NavTabs = React.createClass({
	render: function(){
		var tabs_count = {0:0,1:0,2:0,3:0,4:0,5:0,6:0,7:0,8:0,9:0};

		for (var job_key in this.props.joblist){
			var job = this.props.joblist[job_key];
			tabs_count[broker.status[job.status].tab]++;
		}

		var tabs = [];
		for (var status_name in broker.status) { 
			var status = broker.status[status_name];
			tabs.push(<broker.Tab
				i18n_tab_name={status.i18n_tab_name}
				tab_index={status.tab}
				selected_tab={this.props.selected_tab}
				badge_class={status.badge_class}
				onTabChange={this.props.onTabChange}
				tabs_count={tabs_count}
				key={status_name} />
			);
		}

		return (<ul className="nav nav-tabs">{tabs}</ul>);
	},
});

broker.Tab = React.createClass({
	onClick: function(e) {
		e.preventDefault();
		$(React.findDOMNode(this.refs.tab)).blur();
		this.props.onTabChange(this.props.tab_index);
	},
	render: function() {
		var span = null;
		var badge_count = this.props.tabs_count[this.props.tab_index];
		if (badge_count > 0) {
			var span_class = classNames("badge", this.props.badge_class);
			span = (<span className={span_class} style={{marginLeft: 5}}>
				{badge_count}
			</span>);
		}
		var li_class = null;
		if (this.props.tab_index == this.props.selected_tab) {
			li_class = "active";
		}

		return (<li className={li_class}>
			<a href={location.hash} onClick={this.onClick} ref="tab">
				{i18n(this.props.i18n_tab_name)}
				{span}
			</a>
		</li>);
	},
});

broker.JobListCartridges = React.createClass({
	getInitialState: function() {
		return {version: null, }
	},
	componentWillMount: function() {
		mydmam.async.request("broker", "appversion", {}, function(data) {
			this.setState({version: data});
		}.bind(this));
	},
	render: function() {
		var joblist = this.props.joblist;
		var selected_tab = this.props.selected_tab;

		var selected_jobs = [];
		for (var job_key in this.props.joblist){
			var job = this.props.joblist[job_key];
			if (broker.status[job.status].tab != selected_tab) {
				continue;
			}
			selected_jobs.push(job);
		}

		if (selected_tab == broker.status.WAITING.tab) {
			selected_jobs = selected_jobs.sort(function (a, b) {
				return a.create_date - b.create_date;
			});
		} else {
			selected_jobs = selected_jobs.sort(function (a, b) {
				return b.update_date - a.update_date;
			});
		}

		var cartridges = [];
		for (var pos in selected_jobs) {
			var job = selected_jobs[pos];
			var required_jobs = [];
			if (job.required_keys) {
				for (var pos_rk in job.required_keys) {
					var required_key = job.required_keys[pos_rk];
					if (joblist[required_key]) {
						required_jobs.push(joblist[required_key]);
					}
				}
			}
			cartridges.push(<broker.JobCartridge key={job.key} job={job} version={this.state.version} required_jobs={required_jobs} />);
		}

		return (<div>
			{cartridges}
		</div>);
	},	
});

broker.displayKey = function(key, react_return) {
	if (!key) {
		return null;
	}
	var short_value = key.substring(key.lastIndexOf(":") + 1, key.lastIndexOf(":") + 9) + '.';
	if (react_return) {
		return (<abbr title={key}>
			<code>
				<i className="icon-barcode"></i>&nbsp;{short_value}
			</code>
		</abbr>);
	} else {
		return short_value;
	}
};

broker.getStacktrace = function(processing_error) {
	var content = '';
	content = content + processing_error["class"] + ': ' + processing_error.message + "\n";
	for (var pos = 0; pos < processing_error.stacktrace.length; pos++) {
		var trace = processing_error.stacktrace[pos];
		content = content + String.fromCharCode(160) + 'at' + String.fromCharCode(160) + trace["class"] + '.' + trace.method;
		if (trace.line === -2) {
			content = content + '(Native Method)';
		} else if (trace.file) {
			if (trace.line >= 0) {
				content = content + '(' + trace.file + ':' + trace.line + ')';
			} else {
				content = content + '(' + trace.file + ')';
			}
		} else {
			content = content + '(Unknown Source)';
		}
		content = content + "\n";
	}
	
	if (processing_error.cause) {
		content = content + 'Caused by: ';
		content = content + broker.getStacktrace(processing_error.cause);
	}
	return content;
};

broker.JobProgression = React.createClass({
	render: function() {
		var job = this.props.job;
		var progression_bar = null;
		var last_message = null;
		var step = null;

		if (job.progression) {
			var width = 0;
			if (job.progression.progress_size > 0) {
				width = ((job.progression.progress * 100) / job.progression.progress_size);
				if (width > 98) {
					width = "100%";
				} else {
					width = width + "%";
				}

				var progress_class_names = classNames("progress", {
					"progress-striped": job.status === 'PREPARING'
						| job.status === 'PROCESSING'
						| job.status === 'STOPPED'
						| job.status === 'TOO_LONG_DURATION'
						| job.status === 'ERROR',
					"active": job.status === 'PROCESSING',
					"progress-warning": job.status === 'CANCELED',
					"progress-danger": job.status === 'STOPPED'
						| job.status === 'TOO_LONG_DURATION'
						| job.status === 'ERROR'
						| job.status === 'TOO_OLD',
					"progress-success": job.status === 'DONE',
					"progress-info": job.status === 'WAITING'
						| job.status === 'PREPARING'
						| job.status === 'PROCESSING'
						| job.status === 'POSTPONED',
				});

				progression_bar = (
					<div className={progress_class_names} style={{height: "12px", marginBottom: 0}}>
						<div className="bar" style={{width: width}} />
					</div>
				);
			}

			if (job.progression.last_message) {
				last_message = (<em><i className="icon-comment"/> {job.progression.last_message}</em>);
			}

			if (job.progression.step_count > 0) {
				step = (<strong>
					{job.progression.step}
					<i className="icon-arrow-right" />
					{job.progression.step_count}
				</strong>);
			}
		}

		return (<span>
			<div>
				<div className="pull-left" style={{marginRight: 5, marginTop: -4}}>
					{step}
				</div>
				<div>
					{progression_bar}
				</div>
			</div>
			{last_message}
		</span>);
	}
});

broker.JobCartridgeActionButtons = React.createClass({
	render: function() {
		var status = this.props.status;
		var stacked = this.props.stacked;
		var button_names_for_status = broker.status[status].btns;

		var buttons = [];
		for (var pos in button_names_for_status) {
			var name = button_names_for_status[pos];

			var label = null;
			var btn_style = null;
			if (stacked) {
				label = " " + i18n("manager.jobs.btn." + name);
				btn_style = {marginBottom: 7};
			}

			var btn_class = classNames("btn", "btn-mini", {
				"btn-warning": 	name == "hipriority",
				"btn-success": 	name == "postponed",
				"btn-info": 	name == "cancel" | name == "noexpiration",
				"btn-danger": 	name == "delete" | name == "stop",
				"btn-primary": 	name == "setinwait",
				"btn-block": 	stacked,
			});

			if (name == "hipriority") {		buttons.push(<button key={name} className={btn_class} style={btn_style}>	<i className="icon-warning-sign icon-white"></i>{label}</button>); }
			if (name == "postponed") {		buttons.push(<button key={name} className={btn_class} style={btn_style}>	<i className="icon-step-forward icon-white"></i>{label}</button>); }
			if (name == "cancel") {			buttons.push(<button key={name} className={btn_class} style={btn_style}>	<i className="icon-off icon-white"></i>{label}</button>); }
			if (name == "noexpiration") {	buttons.push(<button key={name} className={btn_class} style={btn_style}>	<i className="icon-calendar icon-white"></i>{label}</button>); }
			if (name == "delete") {			buttons.push(<button key={name} className={btn_class} style={btn_style}>	<i className="icon-trash icon-white"></i>{label}</button>); }
			if (name == "stop") {			buttons.push(<button key={name} className={btn_class} style={btn_style}>	<i className="icon-stop icon-white"></i>{label}</button>); }
			if (name == "setinwait") {		buttons.push(<button key={name} className={btn_class} style={btn_style}>	<i className="icon-inbox icon-white"></i>{label}</button>); }
		}

		if (buttons.length == 0) {
			return (<div />);
		} else if (buttons.length == 1) {
			if (stacked) {
				return (<div className="pull-right span4">{buttons[0]}</div>);
			} else {
				return (<div className="pull-right">{buttons[0]}</div>);
			}
		}

		if (stacked) {
			return (<div className="pull-right span4">{buttons}</div>);
		}

		return (<div className="btn-toolbar pull-right"><div className="btn-group">{buttons}</div></div>);
	},	
});

/*	
		var btndelete = createBtn('', 'icon-trash', 'manager.jobs.btn.delete', 'delete');
		var btnstop = createBtn('', 'icon-stop ', 'manager.jobs.btn.stop', 'stop');
		var btnsetinwait = createBtn('', 'icon-inbox', 'manager.jobs.btn.setinwait', 'setinwait');
		var btncancel = createBtn('', 'icon-off', 'manager.jobs.btn.cancel', 'cancel');
		var btnhipriority = createBtn('', 'icon-warning-sign', 'manager.jobs.btn.hipriority', 'hipriority');
		var btnpostponed = createBtn('', 'icon-step-forward', 'manager.jobs.btn.postponed', 'postponed');
		
		var btnnoexpiration = '';
		if ((job.expiration_date - job.create_date) < mydmam.manager.jobs.default_max_expiration_time) {
			btnnoexpiration = createBtn('', 'icon-calendar', 'manager.jobs.btn.noexpiration', 'noexpiration');
		}
*/
broker.JobCartridge = React.createClass({
	getInitialState: function() {
		return {
			stacked: false,
		};
	},
	render: function() {
		var job = this.props.job;
		var required_jobs = this.props.required_jobs;

		// TODO job.context
		// TODO required_jobs

		var delete_after_completed = null;
		if (job.delete_after_completed) {
			delete_after_completed = (<span className="label label-inverse">{i18n("manager.jobs.delete_after_completed")}</span>);
		}

		var max_execution_time = null;
		if (job.max_execution_time < (1000 * 3600 * 24)) {
			if (job.max_execution_time > (3600 * 1000)) {
				max_execution_time = (<span className="label">{i18n('manager.jobs.max_execution_time_hrs', Math.round((job.max_execution_time / (3600 * 1000))))}</span>);
			} else {
				max_execution_time = (<span className="label">{i18n('manager.jobs.max_execution_time_sec', (job.max_execution_time / 1000))}</span>);
			}
		}
		
		var priority = null;
		if (job.priority > 0) {
			var urgent = null;
			if (job.urgent) {
				urgent = (<i className="icon-warning-sign icon-white"></i>);
			}
			priority = (<span className="badge badge-important">
				{urgent} {i18n('manager.jobs.priority', job.priority)}
			</span>);
		}

		var processing_error = null;
		if (job.processing_error) {
			processing_error = (<code className="json">{broker.getStacktrace(job.processing_error)}</code>);
		}

		var div_3rd_zone = null;
		if (job.status === 'WAITING' | job.status === 'TOO_OLD') {
			div_3rd_zone = (<mydmam.async.pathindex.reactDate i18nlabel="expiration_date" date={job.expiration_date} />);
		} else if (job.status === 'PROCESSING'
			 | job.status === 'DONE'
			 | job.status === 'STOPPED'
			 | job.status === 'TOO_LONG_DURATION'
			 | job.status === 'CANCELED'
			 | job.status === 'POSTPONED'
			 | job.status === 'ERROR') {
			div_3rd_zone = (<broker.JobProgression job={job} />);
		}

		var instance_status_executor_key = null;
		if (job.instance_status_executor_key) {
			if (this.state.stacked) {
				instance_status_executor_key = (<span className="label label-info"><i className="icon-cog icon-white"></i> by {job.instance_status_executor_key}</span>);
			} else {
				instance_status_executor_key = (<span className="label label-info"><i className="icon-cog icon-white"></i> by {job.instance_status_executor_key.substring(0, job.instance_status_executor_key.indexOf("#"))}</span>);
			}
		}

		return (<div className="row-fluid hover-focus">
			<div className="span3 nomargin">
				<strong>{job.name}</strong>
			</div>
			<div className="span3 nomargin">
				<mydmam.async.pathindex.reactSinceDate i18nlabel="update_date" date={job.update_date} /> {delete_after_completed} {priority}
			</div>
			<div className="span3 nomargin">
				{div_3rd_zone}
			</div>
			<div className="span3 nomargin">
				{instance_status_executor_key}
			    <broker.JobCartridgeActionButtons status={job.status} stacked={this.state.stacked} />
			</div>
		</div>);

		/*
				<hr />
				<mydmam.async.JavaClassNameLink javaclass={job.creator} version={this.props.version} />
				<mydmam.async.JavaClassNameLink javaclass={job.worker_class} version={this.props.version} />
				<mydmam.async.pathindex.reactDate i18nlabel="create_date" date={job.create_date} />
				<mydmam.async.pathindex.reactDate i18nlabel="start_date" date={job.start_date} />
				<mydmam.async.pathindex.reactDate i18nlabel="end_date" date={job.end_date} />
				{max_execution_time}
				<span>{broker.displayKey(job.worker_reference, true)}</span><br />
				<span className="label label-info"><i className="icon-cog icon-white"></i> Created by {job.instance_status_creator_key}</span><br />
				{processing_error}
		*/
	},	
});
