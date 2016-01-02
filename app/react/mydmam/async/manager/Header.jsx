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
		}

		return (
			<mydmam.async.PageHeaderTitle title={i18n("manager.pagename")} fluid="true">
				<ul className="nav nav-tabs">
					<manager.HeaderTab href="#manager/summary" i18nlabel="manager.summaries" />
					<manager.HeaderTab href="#manager/items" 	i18nlabel="manager.items" />
					<manager.HeaderTab href="#manager/threads" 	i18nlabel="manager.threads" />
					<manager.HeaderTab href="#manager/perfstats" i18nlabel="manager.perfstats" />
					<manager.HeaderTab href="#manager/classpath" i18nlabel="manager.classpath" />
					<li className="pull-right">
						<a href={location.hash} onClick={this.truncateDb}>{i18n("manager.truncate")}</a>
					</li>
				</ul>
				{show_this}
			</mydmam.async.PageHeaderTitle>
		);
	},
});

mydmam.routes.push("manager-PageSummaries", "manager/summary", manager.Header, [{name: "instances", verb: "allsummaries"}]);	
mydmam.routes.push("manager-PageClasspath", "manager/classpath", manager.Header, [{name: "instances", verb: "allclasspaths"}, {name: "instances", verb: "allsummaries"}]);	
mydmam.routes.push("manager-PageThreads", 	"manager/threads", manager.Header, [{name: "instances", verb: "allthreads"}, {name: "instances", verb: "allsummaries"}]);	
mydmam.routes.push("manager-PageItems", 	"manager/items", manager.Header, [{name: "instances", verb: "allitems"}, {name: "instances", verb: "allsummaries"}]);	
mydmam.routes.push("manager-PagePerfstats", "manager/perfstats", manager.Header, [{name: "instances", verb: "allperfstats"}]);	

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
			<a href={this.props.href} onClick={this.onClick} ref="tab">{i18n(this.props.i18nlabel)}</a>
		</li>);
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
