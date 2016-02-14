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
			last_full_refresh: 0,
			interval: null,
		};
	},
	onTabChange: function(new_tab, selected_status) {
		this.setState({selected_tab: new_tab});
	},
	onActionTabClick: function(action, status_name) {
		var request = {
			all_status: status_name,
			order: action,
		};
		document.body.style.cursor = 'wait';
		mydmam.async.request("broker", "action", request, this.onActionAlterJoblist);
	},
	onActionButtonClick: function(job, action_name) {
		var request = {
			job_key: job.key,
			order: action_name,
		};
		document.body.style.cursor = 'wait';
		mydmam.async.request("broker", "action", request, this.onActionAlterJoblist);
	},
	onActionAlterJoblist: function (altered_items) {
		var new_joblist = jQuery.extend({}, this.state.joblist);
		for (var key in altered_items) {
			if (altered_items[key] == null) {
				delete new_joblist[key];
			} else {
				new_joblist[key] = altered_items[key];
			}
		}
		this.setState({joblist: new_joblist});
		document.body.style.cursor = 'default';
	},
	componentWillMount: function() {
		mydmam.async.request("instances", "appversion", null, function(version) {
			mydmam.async.appversion = version;
		}.bind(this));
		this.updateJobList();
	},
	componentDidMount: function(){
		this.setState({interval: setInterval(this.updateJobList, 3000)});
	},
	componentWillUnmount: function() {
		if (this.state.interval) {
			clearInterval(this.state.interval);
		}
	},
	updateJobList: function() {
		var since = this.state.since;
		if (Date.now() - this.state.last_full_refresh > (5 * 60 * 1000)) {
			since = 0;
		}
		
		mydmam.async.request("broker", "list", {since: since}, function(data) {
			if (since == 0) {
				this.setState({joblist: data, since: Date.now(), last_full_refresh: Date.now()});
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
		var action_avaliable = mydmam.async.isAvaliable("broker", "action");

		return (
			<mydmam.async.PageHeaderTitle title={i18n("manager.jobs.joblist")} fluid="true">
				<broker.NavTabs selected_tab={this.state.selected_tab} onActionTabClick={this.onActionTabClick} onTabChange={this.onTabChange} joblist={this.state.joblist} action_avaliable={action_avaliable} />
				<broker.JobListCartridges joblist={this.state.joblist} selected_tab={this.state.selected_tab} onActionButtonClick={this.onActionButtonClick} action_avaliable={action_avaliable} />
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

broker.NavTabs = React.createClass({
	getInitialState: function() {
		return {
			display_drop_down_action: false,
		};
	},
	componentWillReceiveProps: function(nextProps){
		if (this.props.action_avaliable == false) {
			return;
		}
		this.updateDisplayDropDownAction(nextProps);
	},
	updateDisplayDropDownAction: function(props) {
		if (this.props.action_avaliable == false) {
			return;
		}
		for (var job_key in props.joblist){
			var job = props.joblist[job_key];
			if (broker.status[job.status].tab == props.selected_tab) {
				this.setState({display_drop_down_action: true})
				return;
			}
		}
		this.setState({display_drop_down_action: false})
	},
	componentWillMount: function() {
		if (this.props.action_avaliable == false) {
			return;
		}
		this.updateDisplayDropDownAction(this.props);
	},
	render: function() {
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

		if (this.state.display_drop_down_action & this.props.action_avaliable) {
			var selected_status_tab = null;
			for (var status_name in broker.status) {
				if (this.props.selected_tab == broker.status[status_name].tab) {
					selected_status_tab = status_name;
					break;
				}
			}
			tabs.push(<broker.NavTabDropDownAction key="action" selected_status_tab={selected_status_tab} onActionTabClick={this.props.onActionTabClick} />);
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

broker.NavTabDropDownAction = React.createClass({
	getInitialState: function() {
		return {
			active_dropdown_action_tab: false,
		};
	},
    hide: function() {
		document.removeEventListener("click", this.hide);
    	this.setState({active_dropdown_action_tab: false});
    },
	componentWillUnmount: function () {
		if (this.state.active_dropdown_action_tab) {
			document.removeEventListener("click", this.hide);
		}
	},
	onDropDownActionClick: function(e) {
		e.preventDefault();
		$(React.findDOMNode(this.refs.tab)).blur();
		if (this.state.active_dropdown_action_tab) {
			document.removeEventListener("click", this.hide);
		} else {
			document.addEventListener("click", this.hide);
		}
		this.setState({active_dropdown_action_tab: ! this.state.active_dropdown_action_tab});
	},
	onMenuClick_postponed: function(e) {	this.props.onActionTabClick("postponed", 	this.props.selected_status_tab);	},
	onMenuClick_cancel: function(e) {		this.props.onActionTabClick("cancel", 		this.props.selected_status_tab);	},
	onMenuClick_noexpiration: function(e) {	this.props.onActionTabClick("noexpiration", this.props.selected_status_tab);	},
	onMenuClick_delete: function(e) {		this.props.onActionTabClick("delete", 		this.props.selected_status_tab);	},
	onMenuClick_stop: function(e) {			this.props.onActionTabClick("stop", 		this.props.selected_status_tab);	},
	onMenuClick_setinwait: function(e) {	this.props.onActionTabClick("setinwait", 	this.props.selected_status_tab);	},
	render: function() {
		var status = this.props.selected_status_tab;
		var button_names_for_status = broker.status[status].btns;

		var buttons = [];
		for (var pos in button_names_for_status) {
			var name = button_names_for_status[pos];
			var label = " " + i18n("manager.jobs.btn." + name);
			if (name == "postponed") {
				buttons.push(<li key={name} onClick={this.onMenuClick_postponed}>	<a href={location.hash}><i className="icon-step-forward"></i>{label}	</a></li>); }
			else if (name == "cancel") {	
				buttons.push(<li key={name} onClick={this.onMenuClick_cancel}>		<a href={location.hash}><i className="icon-off"></i>{label}				</a></li>); }
			else if (name == "noexpiration") {
				buttons.push(<li key={name} onClick={this.onMenuClick_noexpiration}><a href={location.hash}><i className="icon-calendar"></i>{label}		</a></li>); }
			else if (name == "delete") {
				buttons.push(<li key={name} onClick={this.onMenuClick_delete}>		<a href={location.hash}><i className="icon-trash"></i>{label}			</a></li>); }
			else if (name == "stop") {
				buttons.push(<li key={name} onClick={this.onMenuClick_stop}>		<a href={location.hash}><i className="icon-stop"></i>{label}			</a></li>); }
			else if (name == "setinwait") {
				buttons.push(<li key={name} onClick={this.onMenuClick_setinwait}>	<a href={location.hash}><i className="icon-inbox"></i>{label}			</a></li>); }
		}

		if (buttons.length == 0) {
			return null;
		}

		var dropdown_class = classNames("dropdown", "pull-right", {
			"open": this.state.active_dropdown_action_tab,
		});

		return (<li key="action" ref="tab" className={dropdown_class}>
			<a className="dropdown-toggle" href={location.hash} onClick={this.onDropDownActionClick}>{i18n("manager.jobs.foreachbtntable")} <b className="caret"></b></a>
			<ul className="dropdown-menu">
				{buttons}
			</ul>
		</li>);
	},
});

broker.JobListCartridges = React.createClass({
	render: function() {
		var joblist = this.props.joblist;
		var selected_tab = this.props.selected_tab;
		var max_display_jobs_reached = false;

		var selected_jobs = [];
		for (var job_key in this.props.joblist){
			var job = this.props.joblist[job_key];
			if (broker.status[job.status].tab != selected_tab) {
				continue;
			}
			selected_jobs.push(job);

			if (selected_jobs.length > 49) {
				max_display_jobs_reached = true;
				break;
			}
		}

		if (selected_tab == broker.status.WAITING.tab) {
			selected_jobs = selected_jobs.sort(function (a, b) {
				return a.create_date - b.create_date;
			});
		} else if (selected_tab == broker.status.DONE.tab) {
			selected_jobs = selected_jobs.sort(function (a, b) {
				return a.end_date - b.end_date;
			});
		} else if (selected_tab == broker.status.PROCESSING.tab) {
			selected_jobs = selected_jobs.sort(function (a, b) {
				return a.start_date - b.start_date;
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
					} else {
						required_jobs.push({key: required_key});
					}
				}
			}
			cartridges.push(<broker.JobCartridge
				key={job.key}
				job={job}
				required_jobs={required_jobs}
				action_avaliable={this.props.action_avaliable}
				onActionButtonClick={this.props.onActionButtonClick} />);
		}

		var too_many_jobs = null;
		if (max_display_jobs_reached) {
			too_many_jobs = (<mydmam.async.AlertBox title={i18n("manager.jobs.toomanyjobs")}>{i18n("manager.jobs.limitedjoblist", selected_jobs.length)}</mydmam.async.AlertBox>);
		}

		return (<div>
			{too_many_jobs}
			{cartridges}
		</div>);
	},	
});

broker.displayKey = function(key, react_return) {
	if (!key) {
		return null;
	}
	var short_value = key;
	if (key.indexOf(":") > -1) {
		short_value = key.substring(key.lastIndexOf(":") + 1, key.lastIndexOf(":") + 9) + '.';
	} else if (key.indexOf("#") > -1) {
		short_value = key.substring(key.indexOf("#") + 1, key.length);
	}
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

broker.JobProgression = React.createClass({
	render: function() {
		var job = this.props.job;
		var progression_bar = null;
		var last_message = null;
		var step = null;

		if (job.progression) {
			var width = "100%";
			if (job.progression.progress_size > 0) {
				width = ((job.progression.progress * 100) / job.progression.progress_size);
				if (width > 98) {
					width = "100%";
				} else {
					width = width + "%";
				}
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
	onClickButton_hipriority: function(e) {		e.stopPropagation();		this.props.onActionButtonClick("hipriority");	},
	onClickButton_postponed: function(e) {		e.stopPropagation();		this.props.onActionButtonClick("postponed");	},
	onClickButton_cancel: function(e) {			e.stopPropagation();		this.props.onActionButtonClick("cancel");	},
	onClickButton_noexpiration: function(e) {	e.stopPropagation();		this.props.onActionButtonClick("noexpiration");	},
	onClickButton_delete: function(e) {			e.stopPropagation();		this.props.onActionButtonClick("delete");	},
	onClickButton_stop: function(e) {			e.stopPropagation();		this.props.onActionButtonClick("stop");	},
	onClickButton_setinwait: function(e) {		e.stopPropagation();		this.props.onActionButtonClick("setinwait");	},
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
				"btn-block": 	stacked,
			});

			if (name == "hipriority") {
				buttons.push(<button key={name} className={btn_class} style={btn_style} onClick={this.onClickButton_hipriority}>	<i className="icon-warning-sign"></i>{label}</button>); }
			else if (name == "postponed") {
				buttons.push(<button key={name} className={btn_class} style={btn_style} onClick={this.onClickButton_postponed}>	<i className="icon-step-forward"></i>{label}</button>); }
			else if (name == "cancel") {	
				buttons.push(<button key={name} className={btn_class} style={btn_style} onClick={this.onClickButton_cancel}>	<i className="icon-off"></i>{label}</button>); }
			else if (name == "noexpiration") {
				buttons.push(<button key={name} className={btn_class} style={btn_style} onClick={this.onClickButton_noexpiration}>	<i className="icon-calendar"></i>{label}</button>); }
			else if (name == "delete") {
				buttons.push(<button key={name} className={btn_class} style={btn_style} onClick={this.onClickButton_delete}>	<i className="icon-trash"></i>{label}</button>); }
			else if (name == "stop") {
				buttons.push(<button key={name} className={btn_class} style={btn_style} onClick={this.onClickButton_stop}>	<i className="icon-stop"></i>{label}</button>); }
			else if (name == "setinwait") {
				buttons.push(<button key={name} className={btn_class} style={btn_style} onClick={this.onClickButton_setinwait}>	<i className="icon-inbox"></i>{label}</button>); }
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

broker.displayContext = function(context) {
	var context_content = null;
	if (context.content != null) {
		var context_content_json = JSON.stringify(context.content, null, " ");
		if (context_content_json != "{}") {
			context_content = (<code className="json" onClick={this.onClickDoNothing}>
				<i className="icon-indent-left"></i>
				<span className="jsontitle"> {i18n("manager.jobs.context")}</span>
				{context_content_json}
			</code>);	
		}
		context_content = (<div style={{marginTop: 7}}>{context_content}</div>);
	}

	var context_neededstorages = null;
	if (context.neededstorages) {
		var label = "manager.jobs.targetstorage";
		if (context.neededstorages.length > 1) {
			label = "manager.jobs.targetstorages";
		}
		var cs_needs = [];
		for (var pos_ns in context.neededstorages) {
			if (context.neededstorages.length > 3) {
				cs_needs.push(<div key={pos_ns}>
					<span className="badge badge-warning" style={{marginLeft: 8}}><i className="icon-hdd icon-white"></i> {context.neededstorages[pos_ns]}</span>
				</div>);
			} else {
				cs_needs.push(<span key={pos_ns} className="badge badge-warning" style={{marginLeft: 8}}><i className="icon-hdd icon-white"></i> {context.neededstorages[pos_ns]}</span>);
			}
		}
		context_neededstorages = (<span>{i18n(label)}
			{cs_needs}
		</span>);
	}

	var context_hookednames = null;
	if (context.hookednames) {
		var label = "manager.jobs.hookedname";
		if (context.hookednames.length > 1) {
			label = "manager.jobs.hookednames";
		}
		var cx_hooks = [];
		for (var pos_cx in context.hookednames) {
			if (context.hookednames.length > 1) {
				cx_hooks.push(<div key={pos_cx}>
					<span className="badge badge-inverse" style={{marginLeft: 8}}><i className="icon-tags icon-white"></i> {context.hookednames[pos_cx]}</span>
				</div>);
			} else {
				cx_hooks.push(<span key={pos_cx} className="badge badge-inverse" style={{marginLeft: 8}}><i className="icon-tags icon-white"></i> {context.hookednames[pos_cx]}</span>);
			}
		}
		context_hookednames = (<span>{i18n(label)}
			{cx_hooks}
		</span>);
	}

	return (<div style={{marginBottom: 7}}>
		<div style={{marginTop: 5}}>
			{i18n("manager.jobs.contextclass")} <mydmam.async.JavaClassNameLink javaclass={context.classname} />
		</div>
		<div style={{marginTop: 5}}>{context_neededstorages}</div>
		<div style={{marginTop: 5}}>{context_hookednames}</div>
		{context_content}
	</div>);
};

broker.JobCartridge = React.createClass({
	getInitialState: function() {
		return {
			stacked: false,
		};
	},
	onToogleCartridgeSize: function(e) {
		e.preventDefault();
		this.setState({stacked: !this.state.stacked});
	},
	onActionButtonClick: function(action_name) {
		this.props.onActionButtonClick(this.props.job, action_name);
	},
	onClickDoNothing: function(e) {
		e.stopPropagation();
	},
	render: function() {
		var job = this.props.job;
		var required_jobs = this.props.required_jobs;

		var creator = null;
		var require = null;
		var dates_start_end = null;
		var processing_error = null;
		var worker_ref = null;
		var context = null;

		if (this.state.stacked) {

			/**
			 * Display require
			 */
			if (required_jobs.length > 0) {
				var require_list = [];
				for (var pos in required_jobs) {
					if (required_jobs[pos].status) {
						var status_broker = broker.status[required_jobs[pos].status];
						var status_broker_class = classNames("badge", status_broker.badge_class);

						require_list.push(<div key={pos}>
							<i className="icon-hand-right" style={{marginLeft: 5}}></i>
							<strong style={{marginLeft: 8, marginRight: 8}}>
								{required_jobs[pos].name}
							</strong>
							<span className={status_broker_class} style={{marginRight: 8}}>
								{i18n(status_broker.i18n_tab_name)}
							</span>
							{broker.displayKey(required_jobs[pos].key, true)}
						</div>);
					} else {
						require_list.push(<div key={pos}>
							<i className="icon-trash" style={{marginLeft: 5}}></i>
							<strong style={{marginLeft: 8, marginRight: 8}}>
								{i18n("manager.jobs.deletedjob")}
							</strong>
						</div>);
					}
				};
				require = (<div className="well well-small">
					<i className="icon-tasks"></i>&nbsp;
					{i18n("manager.jobs.require")}
					<div style={{marginLeft: 8}}>
						{require_list}
					</div>
				</div>);
			}

			var max_execution_time = null;
			if (job.max_execution_time < (1000 * 3600 * 24)) {
				if (job.max_execution_time > (3600 * 1000)) {
					max_execution_time = (<span className="label">{i18n('manager.jobs.max_execution_time_hrs', Math.round((job.max_execution_time / (3600 * 1000))))}</span>);
				} else {
					max_execution_time = (<span className="label">{i18n('manager.jobs.max_execution_time_sec', (job.max_execution_time / 1000))}</span>);
				}
			}

			/**
			 * Display dates
			 */
			creator = (<div>
				<div style={{marginTop: 5}}>
					<mydmam.async.pathindex.reactDate i18nlabel="manager.jobs.createdate" date={job.create_date} style={{marginLeft: 0}} />
				</div>
				<div style={{marginTop: 4}}>
					<span className="label label-info"><i className="icon-cog icon-white"></i> {i18n("manager.jobs.createdbysimple", job.instance_status_creator_key)}</span>
				</div>
				<div style={{marginTop: 4}}>
					{i18n("manager.jobs.classcreator")} <mydmam.async.JavaClassNameLink javaclass={job.creator} />
				</div>
				<div style={{marginTop: 4, marginBottom: 8}}>
					{max_execution_time}
				</div>
			</div>);

			var dates_start_end_content = [];
			if (job.start_date > 0) {
				dates_start_end_content.push(<div style={{marginTop: 5}} key="sdate">
					<mydmam.async.pathindex.reactDate i18nlabel="manager.jobs.start_date" date={job.start_date} />
				</div>);				
				if (job.end_date > 0) {
					dates_start_end_content.push(<div style={{marginTop: 5}} key="edate">
						<mydmam.async.pathindex.reactDate i18nlabel="manager.jobs.end_date" date={job.end_date} />
					</div>);		
					dates_start_end_content.push(<div style={{marginTop: 5}} key="deltadate">
						<span className="label" style={{marginLeft: 5}}>
							<i className="icon-time icon-white"></i>&nbsp;{i18n("manager.jobs.duration", Math.round((job.end_date - job.start_date) / 1000))}
						</span>
					</div>);
					dates_start_end_content.push(<div style={{marginTop: 5}} key="sinceedate">
						<mydmam.async.pathindex.reactSinceDate i18nlabel="manager.jobs.end_date_for" date={job.end_date} />
					</div>);
				}
			}
			dates_start_end = (<div>{dates_start_end_content}</div>);

			/**
			 * Display Stacktrace
			 */
			processing_error = (<mydmam.async.JavaStackTrace processing_error={job.processing_error} />);

			context = broker.displayContext(job.context);

			/**
			 * Display references
			 */
			var job_ref = (<div style={{marginTop: 5}}>
				{i18n("manager.jobs.jref")} <span>{broker.displayKey(job.key, true)}</span>
			</div>);

			if (job.worker_reference != null & job.worker_class != null) {
				worker_ref = (<div className="span7">
					<div style={{marginTop: 5}}>
						{i18n("manager.jobs.wref")} <span>{broker.displayKey(job.worker_reference, true)}</span>
					</div>
					<div style={{marginTop: 5}}>
						{i18n("manager.jobs.classexec")} <mydmam.async.JavaClassNameLink javaclass={job.worker_class} />
					</div>
					{job_ref}
				</div>);
			} else {
				worker_ref = (<div className="span7">
					{job_ref}
				</div>);
			}
		}

		var delete_after_completed = null;
		if (job.delete_after_completed) {
			delete_after_completed = (<span className="label label-inverse">{i18n("manager.jobs.delete_after_completed")}</span>);
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

		var div_3rd_zone = null;
		if (job.status === 'WAITING' | job.status === 'TOO_OLD') {
			div_3rd_zone = (<mydmam.async.pathindex.reactDate i18nlabel="manager.jobs.expiration_date" date={job.expiration_date} />);
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

		var main_div_classname = classNames("row-fluid", "hover-focus", {
			"stacked": this.state.stacked,
		});

		var action_buttons = null;
		if (this.props.action_avaliable) {
			action_buttons = (<broker.JobCartridgeActionButtons status={job.status} stacked={this.state.stacked} onActionButtonClick={this.onActionButtonClick} />);
		}

		return (<div className={main_div_classname} onClick={this.onToogleCartridgeSize}>
			<div className="span3 nomargin">
				<strong>{job.name}</strong>
				{creator}
				{require}
			</div>
			<div className="span3 nomargin">
				<mydmam.async.pathindex.reactSinceDate i18nlabel="manager.jobs.update_date" date={job.update_date} /> {delete_after_completed} {priority}
				{dates_start_end}
			</div>
			<div className="span3 nomargin">
				{div_3rd_zone}
				{context}
				{processing_error}
			</div>
			<div className="span3 nomargin">
				{instance_status_executor_key}
				{worker_ref}
			    {action_buttons}
			</div>
		</div>);

	},	
});
