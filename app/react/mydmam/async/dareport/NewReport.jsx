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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/

dareport.reportnew_link = "dareport";

dareport.NewReport = createReactClass({
	/*getInitialState: function() {
		return {timer: null, show_bar: false};
	},
	componentWillMount: function() {
		this.setState({timer: setTimeout(this.onTimerDone, 300)});
	},*/
	render: function() {
		return (<span>TODO...</span>);
	}
});

mydmam.routes.push("dareport-new", dareport.reportnew_link, dareport.NewReport, [{name: "dareport", verb: "reportnew"}]);
