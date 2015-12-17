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
		//this.setState({interval: setInterval(this.updateJobList, 10000)});
	},
	componentWillUnmount: function() {
		if (this.state.interval) {
			//clearInterval(this.state.interval);
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

broker.map_navtabs_status = {TOO_OLD: 3, CANCELED: 5, POSTPONED: 5, WAITING: 0, DONE: 2, PROCESSING: 1, STOPPED: 4, ERROR: 6, PREPARING: 1, TOO_LONG_DURATION: 4};

broker.NavTabs = React.createClass({
	render: function(){
		var tabs_count = {0:0,1:0,2:0,3:0,4:0,5:0,6:0};

		for (var job_key in this.props.joblist){
			var job = this.props.joblist[job_key];
			tabs_count[broker.map_navtabs_status[job.status]]++;
		}

		return (<ul className="nav nav-tabs">
			<broker.Tab i18n_tab_name="manager.jobs.status.WAITING"
				tab_index={0} selected_tab={this.props.selected_tab} badge_class={null} onTabChange={this.props.onTabChange} tabs_count={tabs_count} />
			<broker.Tab i18n_tab_name="manager.jobs.status.PREPARINGPROCESSING"
				tab_index={1} selected_tab={this.props.selected_tab} badge_class="badge-warning" onTabChange={this.props.onTabChange} tabs_count={tabs_count} />
			<broker.Tab i18n_tab_name="manager.jobs.status.DONE"
				tab_index={2} selected_tab={this.props.selected_tab} badge_class="badge-success" onTabChange={this.props.onTabChange} tabs_count={tabs_count} />
			<broker.Tab i18n_tab_name="manager.jobs.status.TOO_OLD"
				tab_index={3} selected_tab={this.props.selected_tab} badge_class="badge-info" onTabChange={this.props.onTabChange} tabs_count={tabs_count} />
			<broker.Tab i18n_tab_name="manager.jobs.status.STOPPEDTOO_LONG_DURATION"
				tab_index={4} selected_tab={this.props.selected_tab} badge_class="badge-info" onTabChange={this.props.onTabChange} tabs_count={tabs_count} />
			<broker.Tab i18n_tab_name="manager.jobs.status.CANCELEDPOSTPONED"
				tab_index={5} selected_tab={this.props.selected_tab} badge_class={null} onTabChange={this.props.onTabChange} tabs_count={tabs_count} />
			<broker.Tab i18n_tab_name="manager.jobs.status.ERROR"
				tab_index={6} selected_tab={this.props.selected_tab} badge_class="badge-important" onTabChange={this.props.onTabChange} tabs_count={tabs_count} />
		</ul>);
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
			if (broker.map_navtabs_status[job.status] != selected_tab) {
				continue;
			}
			selected_jobs.push(job);
		}

		selected_jobs = selected_jobs.sort(function (a, b) {
			return a.update_date - b.update_date;
		});

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

broker.getStatusLabel = function(job) {
	var i18n_status = i18n('manager.jobs.status.' + job.status);
	if (job.status === 'WAITING') {
		return (<span className="label">{i18n_status}</span>);
	} else if (job.status === 'PREPARING') {
		return (<span className="label label-warning">{i18n_status}</span>);
	} else if (job.status === 'PROCESSING') {
		return (<span className="label label-warning">{i18n_status}</span>);
	} else if (job.status === 'DONE') {
		return (<span className="label">{i18n_status}</span>);
	} else if (job.status === 'TOO_OLD') {
		return (<span className="label label-info">{i18n_status}</span>);
	} else if (job.status === 'STOPPED') {
		return (<span className="label label-info">{i18n_status}</span>);
	} else if (job.status === 'TOO_LONG_DURATION') {
		return (<span className="label label-info">{i18n_status}</span>);
	} else if (job.status === 'CANCELED') {
		return (<span className="label label-info">{i18n_status}</span>);
	} else if (job.status === 'POSTPONED') {
		return (<span className="label label-info">{i18n_status}</span>);
	} else if (job.status === 'ERROR') {
		return (<span className="label badge-important">{i18n_status}</span>);
	} else {
		return (<span className="label label-inverse">{i18n_status}</span>);
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
				width = (Math.round(job.progression.progress / job.progression.progress_size) * 100) + "%";
			}

			progression_bar = (
				<div className="progress" style={{height: "12px", marginBottom: 0}}>
					<div className="bar" style={{width: width}} />
				</div>
			);

			if (job.progression.last_message) {
				last_message = (<em><i className="icon-comment"/> {job.progression.last_message}</em>);
			}

			if ((job.progression.step > 0) & (job.progression.step_count > 0)) {
				step = (<strong className="pull-right">
						{job.progression.step}
						<i className="icon-arrow-right" />
						{job.progression.step_count}
					</strong>
				);
			}
		}

		return (<span>
			{step}
			{progression_bar}
			{last_message}
		</span>);
	}
});

broker.JobCartridge = React.createClass({
	render: function() {
		var job = this.props.job;
		var required_jobs = this.props.required_jobs;

		// job.context
		// required_jobs
		var urgent_job = null;
		if (job.urgent) {
			urgent_job = (<span className="badge badge-important">{i18n("manager.jobs.urgent")}</span>);
		}

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
			priority = (<span className="badge badge-important">
				{i18n('manager.jobs.priority', job.priority)}
			</span>);
		}

		var processing_error = null;
		if (job.processing_error) {
			processing_error = (<code className="json">{broker.getStacktrace(job.processing_error)}</code>);
		}

		return (<div className="row-fluid">
			<div className="span12">
				<strong>{job.name}</strong><br />
				{broker.getStatusLabel(job)}<br />

				<mydmam.async.JavaClassNameLink javaclass={job.creator} version={this.props.version} />
				<mydmam.async.JavaClassNameLink javaclass={job.worker_class} version={this.props.version} />
				<mydmam.async.pathindex.reactDate i18nlabel="expiration_date" date={job.expiration_date} />
				<mydmam.async.pathindex.reactDate i18nlabel="create_date" date={job.create_date} />
				<mydmam.async.pathindex.reactDate i18nlabel="update_date" date={job.update_date} />
				<mydmam.async.pathindex.reactDate i18nlabel="start_date" date={job.start_date} />
				<mydmam.async.pathindex.reactDate i18nlabel="end_date" date={job.end_date} />
				{urgent_job}
				{delete_after_completed}
				{max_execution_time}
				{priority}
				<span className="label label-info"><i className="icon-cog icon-white"></i> Created by {job.instance_status_creator_key}</span><br />
				<span>{broker.displayKey(job.worker_reference, true)}</span><br />
				<span className="label label-info"><i className="icon-cog icon-white"></i> Processed by {job.instance_status_executor_key}</span><br />
				{processing_error}
				<broker.JobProgression job={job} />
			</div>
		</div>);
	},	
});
