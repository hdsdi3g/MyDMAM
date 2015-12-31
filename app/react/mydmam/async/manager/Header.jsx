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
	render: function(){
		var show_this = null;
		if (this.props.params) {
			if (this.props.params.tab) {
				if (this.props.params.tab == "summary") {
					show_this = (<manager.Summaries />);
				} else if (this.props.params.tab == "classpath") {
					show_this = (<manager.Classpaths />);
				}
			}
		}

		//allitems
		//allthreads

		if (show_this == null) {
			show_this = (<manager.InstancesSummaries />);
		}

		return (
			<mydmam.async.PageHeaderTitle title={i18n("manager.pagename")} fluid="true">
				<ul className="nav nav-tabs">
					<manager.HeaderTab href="#manager/summary" i18nlabel="manager.summaries" />
					<manager.HeaderTab href="#manager/classpath" i18nlabel="manager.classpath" />
				</ul>
				{show_this}
			</mydmam.async.PageHeaderTitle>
		);
	},
});

mydmam.routes.push("manager", "manager/:tab", manager.Header, [{name: "instances", verb: "allsummaries"}]);	

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
