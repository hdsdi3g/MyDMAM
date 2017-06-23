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
 * Research and development for some new tools
 */

/**
 * getI18n() + formatBytes() Create correct file size i18n with unit selection.
 */
var bytes_unit_sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];

/**
 * @See https://stackoverflow.com/questions/15900485/correct-way-to-convert-size-in-bytes-to-kb-mb-gb-in-javascript
 */
async.formatBytes = function(bytes, decimals) {
	if (bytes == 0) {
		return i18n("sizeunit.Bytes", 0);
	}
	var k = 1000;
	var dm = decimals || 2;
	var i = Math.floor(Math.log(bytes) / Math.log(k));
	return i18n("sizeunit." + bytes_unit_sizes[i], (bytes / Math.pow(k, i)).getI18n(dm));
}

/**
 * @return, for fr-FR "123 octets - 123,46 Ko - 132,47 Go - 54,8 To"
 */
async.DemoNumbersSize = function() {
	var num0 = 123;
	var num1 = 123456;
	var num2 = 132465789123;
	var num3 = 54796524796435;

	return async.formatBytes(num0) + " - " + async.formatBytes(num1) + " - " + async.formatBytes(num2) + " - " + async.formatBytes(num3);
};

/**
 * DemoMultipleSelect: get onclick steal for a child item
*/
var SelectableItem = createReactClass({
	onClick: function(e) {
		e.preventDefault();
		this.props.onSelect(this.props.reference);
	},
	render: function() {
		var is_selected = this.props.selected;

		var color = "transparent";
		if (is_selected) {
			color = "#DDD";
		}
		var style = {
			border: "1px solid black",
			backgroundColor: color,
			cursor: "pointer",
		};

		return (<div style={style} onClick={this.onClick}>
			{this.props.children}
		</div>);
	}
});

async.DemoMultipleSelect = createReactClass({
	getInitialState: function() {
		return {selected: null, };
	},
	onSelect: function(reference) {
		this.setState({selected: reference});
	},
	onClickButton(e) {
		e.preventDefault();
		e.stopPropagation();
		console.log("CLICK !");
	},
	render: function() {
		var items = [];

		for (var i = 0; i < 3; i++) {
			items.push(<SelectableItem key={i} selected={this.state.selected === i} reference={i} onSelect={this.onSelect}>
				Text <button onClick={this.onClickButton} className="btn">Do click</button>
			</SelectableItem>);
		}
		

		return (<div>
			{items}
		</div>);
	}
});

/* 
 * Bound in a div one-line items, with a strict width
 * Usage : <DivWidth width="5em" dotted={true}>This is a too long text</DivWidth>
 *         <DivWidth width="2em" ellipsis={true}>Short text</DivWidth>
 */
async.DivWidth = createReactClass({
	propTypes: {
		/** css width */
		width: PropTypes.oneOfType([
			PropTypes.string,
			PropTypes.number,
		]),
		/** Add "..." before the end */ 
		ellipsis: PropTypes.bool,
		/** Add 1px dotted border, for debugging */
		dotted: PropTypes.bool,
		/** Items to add in the div */
		children: PropTypes.node.isRequired,
	},
	render: function() {
		var width = this.props.width;
		var ellipsis = this.props.ellipsis;
		var dotted = this.props.dotted;

		var text_overflow = "clip";
		if (ellipsis) {
			text_overflow = "ellipsis";
		}

		var border = null;
		if (dotted) {
			border = "1px dotted black";
		}	
			
		var style = {
			overflow: "hidden",
			border: border,
			whiteSpace: "nowrap",
			textOverflow: text_overflow,
			width: width,
		};

		return (<div style={style}>
			{this.props.children}
		</div>);
	}
});

