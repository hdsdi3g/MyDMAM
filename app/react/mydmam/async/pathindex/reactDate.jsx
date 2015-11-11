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

pathindex.reactDate = React.createClass({
	render: function() {
		if (!this.props.date) {
			return null;
		}
		var label = null;
		if (this.props.i18nlabel) {
			label = i18n(this.props.i18nlabel) + " ";
		}
		var style = {marginLeft: 5};
		if (this.props.style) {
			style = this.props.style;
		}
		return (<span className="label" style={style}>{label}{mydmam.format.fulldate(this.props.date)}</span>);
	},
});