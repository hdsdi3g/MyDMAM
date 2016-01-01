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

manager.PageSummaries = React.createClass({
	render: function(){
		return (
			<manager.Header page="summary" />
		);
	},
});
manager.PageClasspath = React.createClass({
	render: function(){
		return (
			<manager.Header page="classpath" />
		);
	},
});
manager.PageThreads = React.createClass({
	render: function() {
		return (
			<manager.Header page="threads" />
		);
	},
});
manager.PageItems = React.createClass({
	render: function() {
		return (
			<manager.Header page="items" />
		);
	},
});

mydmam.routes.push("manager-PageSummaries", "manager/summary", 	manager.PageSummaries, [{name: "instances", verb: "allsummaries"}]);	
mydmam.routes.push("manager-PageClasspath", "manager/classpath", manager.PageClasspath, [{name: "instances", verb: "allclasspaths"}, {name: "instances", verb: "byrefs"}]);	
mydmam.routes.push("manager-PageThreads", 	"manager/threads", 	manager.PageThreads, [{name: "instances", verb: "allthreads"}, {name: "instances", verb: "allsummaries"}]);	
mydmam.routes.push("manager-PageItems", 	"manager/items", 	manager.PageItems, [{name: "instances", verb: "allitems"}, {name: "instances", verb: "allsummaries"}]);	

manager.Header = React.createClass({
	render: function(){
		var show_this = null;
		if (this.props.page == "summary") {
			show_this = (<manager.Summaries />);
		} else if (this.props.page == "classpath") {
			show_this = (<manager.Classpaths />);
		} else if (this.props.page == "threads") {
			show_this = (<manager.Threads />);
		} else if (this.props.page == "items") {
			show_this = (<manager.Items />);
		}

		return (
			<mydmam.async.PageHeaderTitle title={i18n("manager.pagename")} fluid="true">
				<ul className="nav nav-tabs">
					<manager.HeaderTab href="#manager/summary" i18nlabel="manager.summaries" />
					<manager.HeaderTab href="#manager/items" 	i18nlabel="manager.items" />
					<manager.HeaderTab href="#manager/threads" 	i18nlabel="manager.threads" />
					<manager.HeaderTab href="#manager/classpath" i18nlabel="manager.classpath" />	
				</ul>
				{show_this}
			</mydmam.async.PageHeaderTitle>
		);
	},
});

manager.HeaderTab = React.createClass({
	onClick: function(e) {
		//e.preventDefault();
		//this.props.onActiveChange(this.props.pos);
		$(React.findDOMNode(this.refs.tab)).blur();
	},
	render: function(){
		var li_class = classNames({
			"active": this.props.href == location.hash
		});

		return (<li className={li_class}>
			<a href={this.props.href}>{i18n(this.props.i18nlabel)}</a>
		</li>);
	},
});

manager.InstancesNavList = React.createClass({
	getInitialState: function() {
		return {
			list: {},
			instance_selected: null,
		};
	},
	componentWillMount: function() {
		mydmam.async.request("instances", "allsummaries", null, function(list) {
			this.setState({list: list});
		}.bind(this));
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
		for (var key in this.state.list) {
			var summary = this.state.list[key];
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

manager.InstancesNavListElement = React.createClass({
	onClick: function(e) {
		e.preventDefault();
		this.props.onSelect(this.props.reference);
		$(React.findDOMNode(this.refs.tab)).blur();
	},
	render: function() {
		return (<a href={location.href} ref="tab" onClick={this.onClick}>
			{this.props.children}
		</a>);
	},
});
