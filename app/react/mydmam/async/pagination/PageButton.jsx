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

pagination.PageButton = React.createClass({
	handleClick: function(e) {
		if (!this.props.onClickButton) {
			return;
		}
		e.preventDefault();
		var alt = false;
		if (e.altKey) {
			alt = true;
		}
		this.props.onClickButton(this.props.num, alt);
	},
	render: function() {
		if (this.props.currentpage === this.props.num) {
		    return (<li className="active"><span>{this.props.num}</span></li>);
		}

	    return (<li>
		    	<a href={this.props.linkHref} onClick={this.handleClick}>{this.props.num}</a>
			</li>);
	}
});