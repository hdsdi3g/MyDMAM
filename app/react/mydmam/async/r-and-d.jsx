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
 * Polyvalent div resizer (mouse/touch).
 * <async.ResizablePane mode="both">...items...</async.ResizablePane>
 * With mode=both|width|height
 */
var ResizerHandle = createReactClass({
	onMouseDown: function(e) {
		this.props.onStartResize(e.clientX, e.clientY, this.props.behavior, false);
	},
	onTouchStart: function(e) {
		e.preventDefault(),
		this.props.onStartResize(e.changedTouches[0].clientX, e.changedTouches[0].clientY, this.props.behavior, true);
	},
	render: function() {
		return (<div style={this.props.style} onMouseDown={this.onMouseDown} onTouchStart={this.onTouchStart}></div>);
	}
});

async.ResizablePane = createReactClass({
	getInitialState: function() {
		return {
			start_x: null,
			start_y: null,
			start_width: null,
			start_height: null,
			width: null,
			height: null,
			behavior: null,
		};
	},
	onStartResize: function(x, y, behavior, is_touch) {
		var resizeable = ReactDOM.findDOMNode(this.refs.resizeable);
		var computed_style = document.defaultView.getComputedStyle(resizeable);

		this.setState({
			start_width: parseInt(computed_style.width, 10),
			start_height: parseInt(computed_style.height, 10),
			start_x: x,
			start_y: y,
			behavior: behavior,
		}, function() {
			if (is_touch) {
				document.documentElement.addEventListener('touchmove', this.doTouchMove, false);
				document.documentElement.addEventListener('touchend', this.doTouchEnd, false);
			} else {
				document.documentElement.addEventListener('mousemove', this.doDrag, false);
				document.documentElement.addEventListener('mouseup', this.stopDrag, false);
			}
		}.bind(this));
	},
	doDrag: function(e) {
		this.doResize(e.clientX, e.clientY);
	},
	doTouchMove: function(e) {
		e.preventDefault();
		this.doResize(e.changedTouches[0].clientX, e.changedTouches[0].clientY);
	},
	doResize: function(new_x, new_y) {
		if (this.props.mode == "width") {
			this.setState({
				width: this.state.start_width + new_x - this.state.start_x,
				height: this.state.start_height,
			});
		} else if (this.props.mode == "height") {
			this.setState({
				width: this.state.start_width,
				height: this.state.start_height + new_y - this.state.start_y,
			});
		} else {
			if (this.state.behavior == "width") {
				this.setState({
					width: this.state.start_width + new_x - this.state.start_x,
					height: this.state.start_height,
				});
			} else if (this.state.behavior == "height") {
				this.setState({
					width: this.state.start_width,
					height: this.state.start_height + new_y - this.state.start_y,
				});
			} else {
				this.setState({
					width: this.state.start_width + new_x - this.state.start_x,
					height: this.state.start_height + new_y - this.state.start_y,
				});
			}
		}
	},
	stopDrag: function() {
	    document.documentElement.removeEventListener('mousemove', this.doDrag, false);
	    document.documentElement.removeEventListener('mouseup', this.stopDrag, false);
	},
	doTouchEnd: function() {
	    document.documentElement.removeEventListener('touchmove', this.doTouchMove, false);
	    document.documentElement.removeEventListener('touchend', this.doTouchEnd, false);
	},
	componentWillUnmount: function() {
		this.stopDrag();
		this.doTouchEnd();
	},
	componentWillUpdate: function() {
		window.getSelection().removeAllRanges();
	},
	render: function() {
		var mode = this.props.mode;
		if (mode == null) {
			mode = "both";
		}

		var style = {
			position: "relative",
		};

		if (this.state.width != null && this.state.height != null) {
			style.width = this.state.width + "px";
			style.height = this.state.height + "px";
		}

		var border_debug = null; //"1px solid #888";

		var handles = [];
		if (mode == "width") {
			handles.push(<ResizerHandle style={{
				width: "20px",
				height: "100%",
				position: "absolute",
				border: border_debug,
				right: 0,
				top: 0,
				cursor: "ew-resize",
			}} key="w" onStartResize={this.onStartResize} behavior="width" />);
		} else if (mode == "height") {
			handles.push(<ResizerHandle style={{
				width: "100%",
				height: "20px",
				position: "absolute",
				border: border_debug,
				left: 0,
				bottom: 0,
				cursor: "ns-resize",
			}} key="h" onStartResize={this.onStartResize} behavior="height" />);
		} else {
			handles.push(<ResizerHandle style={{
				width: "20px",
				height: "100%",
				position: "absolute",
				border: border_debug,
				right: 0,
				top: 0,
				cursor: "ew-resize",
			}} key="w" onStartResize={this.onStartResize} behavior="width" />);
			handles.push(<ResizerHandle style={{
				width: "100%",
				height: "20px",
				position: "absolute",
				border: border_debug,
				left: 0,
				bottom: 0,
				cursor: "ns-resize",
			}} key="h" onStartResize={this.onStartResize} behavior="height" />);
			handles.push(<ResizerHandle style={{
				width: "20px",
				height: "20px",
				position: "absolute",
				border: border_debug,
				right: 0,
				bottom: 0,
				cursor: "nwse-resize",
			}} key="c" onStartResize={this.onStartResize} behavior="corner" />);
		}

		return (<div ref="resizeable" style={style}>
			{this.props.children}
			{handles}
		</div>);
	}
});

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

