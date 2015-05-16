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

navigate.ButtonSort = React.createClass({
	handleClick: function(e) {
		e.preventDefault();
		this.props.onChangeState(this.props.colname, this.props.order);
	},
	render: function() {
		var is_up = false;
		var is_down = false;
		var btn_active = false;

		if (this.props.order != null) {
			is_up = (this.props.order === 'asc');
			is_down = (this.props.order === 'desc');
			btn_active = true;
		}

		var btn_classes = classNames({
		    'btn': true, 'btn-mini': true, 'pull-right': true,
	    	'active': btn_active,
		});
		var icon_classes = classNames({
			'pull-right': true,
		    'icon-chevron-up': is_up,
		    'icon-chevron-down': is_down,
		    'icon-minus': ((is_up === false) & (is_down === false)),
		});

		return (
			<button className={btn_classes} onClick={this.handleClick}>
				<i className={icon_classes}></i>
			</button>
		);
	}
});