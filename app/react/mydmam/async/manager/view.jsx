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

manager.View = React.createClass({
	getInitialState: function() {
		return {
		};
	},
	componentWillMount: function() {
	},
	componentDidMount: function(){
	},
	componentWillUnmount: function() {
	},
	render: function(){
		return (
			<mydmam.async.PageHeaderTitle title={i18n("manager.instances")} fluid="true">
			</mydmam.async.PageHeaderTitle>
		);
	},
});

//mydmam.routes.push("broker-Jobs", "broker", manager.View, [{name: "broker", verb: "list"}]);	
