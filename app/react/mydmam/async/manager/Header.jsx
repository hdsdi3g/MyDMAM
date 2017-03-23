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

manager.Header = React.createClass({
	getInitialState: function() {
		return {
			summaries: {},
			interval: null,
		};
	},
	componentWillMount: function() {
		this.refresh();
		mydmam.async.request("instances", "appversion", null, function(version) {
			mydmam.async.appversion = version;
		}.bind(this));
	},
	refresh: function() {
		mydmam.async.request("instances", "allsummaries", null, function(summaries) {
			this.setState({summaries: summaries});
		}.bind(this));
	},
	componentDidMount: function(){
		this.setState({interval: setInterval(this.refresh, 10000)});
	},
	componentWillUnmount: function() {
		if (this.state.interval) {
			clearInterval(this.state.interval);
		}
	},
	truncateDb: function(e) {
		e.preventDefault();
		mydmam.async.request("instances", "truncate", null, function(){
			window.location.reload();
		}.bind(this));
	},
	render: function(){
		if (this.state.summaries == null) {
			show_this = (<mydmam.async.PageLoadingProgressBar />);
		}

		var show_this = null;
		if (location.hash.indexOf("#manager/summary") == 0) {
			show_this = (<manager.Summaries summaries={this.state.summaries} />);
		} else if (location.hash.indexOf("#manager/classpath") == 0) {
			show_this = (<manager.Classpaths summaries={this.state.summaries} />);
		} else if (location.hash.indexOf("#manager/threads") == 0) {
			show_this = (<manager.Threads summaries={this.state.summaries} />);
		} else if (location.hash.indexOf("#manager/items") == 0) {
			show_this = (<manager.Items summaries={this.state.summaries} />);
		} else if (location.hash.indexOf("#manager/perfstats") == 0) {
			show_this = (<manager.Perfstats summaries={this.state.summaries} />);
		} else if (location.hash.indexOf("#manager/lastjobs") == 0) {
			show_this = (<manager.Lastjobs />);
		} else if (location.hash.indexOf("#manager/pendingactions") == 0) {
			show_this = (<manager.PendingActions />);
		} else if (location.hash.indexOf("#manager/clusterstatus") == 0) {
			show_this = (<manager.ClusterStatus />);
		} else if (location.hash.indexOf("#manager/playserver") == 0) {
			show_this = (<manager.PlayServer />);
		} 

		return (
			<mydmam.async.PageHeaderTitle title={i18n("manager.pagename")} fluid="true">
				<ul className="nav nav-tabs">
					<mydmam.async.HeaderTab href="#manager/summary"   i18nlabel="manager.summaries" />
					<mydmam.async.HeaderTab href="#manager/items" 	 i18nlabel="manager.items" />
					<mydmam.async.HeaderTab href="#manager/pendingactions"  i18nlabel="manager.pendingactions" />
					<mydmam.async.HeaderTab href="#manager/lastjobs"  i18nlabel="manager.lastjobs" />
					<mydmam.async.HeaderTab href="#manager/threads" 	 i18nlabel="manager.threads" />
					<mydmam.async.HeaderTab href="#manager/perfstats" i18nlabel="manager.perfstats" />
					<mydmam.async.HeaderTab href="#manager/classpath" i18nlabel="manager.classpath" />
					<mydmam.async.HeaderTab href="#manager/playserver" i18nlabel="manager.playserver" />
					<mydmam.async.HeaderTab href="#manager/clusterstatus" i18nlabel="manager.clusterstatus" />
					<li className="pull-right">
						<a href={location.hash} onClick={this.truncateDb}>{i18n("manager.truncate")}</a>
					</li>
				</ul>
				{show_this}
			</mydmam.async.PageHeaderTitle>
		);
	},
});

mydmam.routes.push("manager-Summaries", "manager/summary",		manager.Header, [{name: "instances", verb: "allsummaries"}]);	
mydmam.routes.push("manager-Classpath", "manager/classpath", 	manager.Header, [{name: "instances", verb: "allclasspaths"}, {name: "instances", verb: "allsummaries"}]);	
mydmam.routes.push("manager-Lastjobs", 	"manager/lastjobs", 	manager.Header, [{name: "instances", verb: "alldonejobs"}, {name: "instances", verb: "allsummaries"}]);	
mydmam.routes.push("manager-Threads", 	"manager/threads", 		manager.Header, [{name: "instances", verb: "allthreads"}, {name: "instances", verb: "allsummaries"}]);	
mydmam.routes.push("manager-Items", 	"manager/items", 		manager.Header, [{name: "instances", verb: "allitems"}, {name: "instances", verb: "allsummaries"}]);	
mydmam.routes.push("manager-Perfstats", "manager/perfstats", 	manager.Header, [{name: "instances", verb: "allperfstats"}]);	
mydmam.routes.push("manager-PendingActions", "manager/pendingactions", 	manager.Header, [{name: "instances", verb: "allpendingactions"}]);	
mydmam.routes.push("manager-ClusterStatus", "manager/clusterstatus", 	manager.Header, [{name: "instances", verb: "esclusterstatus"}]);	
mydmam.routes.push("manager-PlayServer", "manager/playserver", 	manager.Header, [{name: "instances", verb: "playserver"}]);	

manager.InstancesNavListElement = React.createClass({
	onClick: function(e) {
		e.preventDefault();
		this.props.onSelect(this.props.reference);
		$(ReactDOM.findDOMNode(this.refs.tab)).blur();
	},
	render: function() {
		return (<a href={location.href} ref="tab" onClick={this.onClick}>
			{this.props.children}
		</a>);
	},
});

manager.canCreateInstanceAction = function() {
	return mydmam.async.isAvaliable("instances", "instanceaction");
};

manager.createInstanceAction = function(target_class_name, target_reference_key, json_order, callback) {
	if (manager.canCreateInstanceAction() == false) {
		console.err("Can't create instance action, missing controller instanceaction", target_class_name, target_reference_key, json_order);
		return;
	}

	var action = {
		target_class_name: target_class_name,
		target_reference_key: target_reference_key,
		json_order: json_order,
	};

	mydmam.async.request("instances", "instanceaction", action, callback);
};
