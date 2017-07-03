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

/*
Suite : https://developer.mozilla.org/fr/docs/Web/CSS/CSS_Grid_Layout
*/

var DemoListitem = createReactClass({
	render: function() {
		var style = {
			height: "100px",
		};

		return (<article style={style}>
			{this.props.text}
		</article>);
	}
});

async.DemoPanelLists = createReactClass({
	render: function() {
		var article_list = [];
		for (var i = 0; i < 20; i++) {
			article_list.push(<DemoListitem key={i} text={"article" + i} />);
		}
		
		return (<section className="demopanellists">
			<header className="display-table">
				<div className="display-table-row">
					<nav className="display-table-cell">Breadcrumb</nav>
				</div>
			</header>
			<section className="display-table">
				<div className="display-table-row">
					<aside style={{width: "150px"}} className="display-table-cell">Left panel</aside>
					<div className="display-table-cell">
						<header>Option's menu</header>
						<section className="article-list">{article_list}</section>
						<footer>Pagination</footer>
					</div>
					<aside style={{width: "200px"}} className="display-table-cell">Right panel</aside>
				</div>
			</section>
			<footer>
				Status bar
			</footer>
		</section>);
	}
});

/**
 * Overlay a text on an image
 */
async.DemoTextOverlay = createReactClass({
	render: function() {
		var w1 = 352/4;
		var h1 = 288/4;

		var w2 = 352;
		var h2 = 288;

		return (<div style={{display: "inline"}}>
			<async.TextOverlay overlay="MonTexteSuperLong" width={w1} height={h1} style={{display: "inline"}}>
				<div style={{
					background: "#f5f5f5 url('/public/img/background-v1.jpg') no-repeat center top",
					backgroundSize: "cover",
					width: w1 + "px",
					height: h1 + "px",
				}}></div>
			</async.TextOverlay>

			<async.TextOverlay overlay="MonText2sSuperLong" width={w2} height={h2} style={{display: "inline"}}>
				<div style={{
					background: "#f5f5f5 url('/public/img/background-v1.jpg') no-repeat center top",
					backgroundSize: "cover",
					width: w2 + "px",
					height: h2 + "px",
				}}></div>
			</async.TextOverlay>
		</div>);
	}
});

async.TextOverlay = createReactClass({
	propTypes: {
		overlay: PropTypes.string.isRequired,
		width: PropTypes.number.isRequired,
		height: PropTypes.number.isRequired,
		children: PropTypes.node.isRequired,
		style: PropTypes.object,
	},
	render: function() {
		var w = this.props.width;
		var h = this.props.height;

		var font_size = h/6;
		var margin_top = font_size * 4;
		var padding_left = w/20;

		var style_front = {
			width: (w - padding_left) + "px",
			marginTop: margin_top + "px",
			paddingLeft: padding_left + "px",
			fontSize: font_size + "pt",
		};

		var style_bgk = {
			zIndex: -1,
			width: w + "px",
			height: h + "px",
		};

		return (<div style={this.props.style}>
			<div style={style_front} className="img-text-overlay">{this.props.overlay}</div>
			<div style={style_bgk}>{this.props.children}</div>
		</div>);
	}
});

/**
 * Experiment popup
 */
async.DemoFrontPanel = createReactClass({
	getInitialState: function() {
		return {
			display_front_panel: false,
		};
	},
	btnShowOnClick: function(e) {
		e.preventDefault();
		ReactDOM.findDOMNode(this.refs.btnhideshow).blur();
		this.setState({display_front_panel: !this.state.display_front_panel});
	},
	onDocumentClick: function(e) {
		if (this.state.display_front_panel) {
			var dom_panel = ReactDOM.findDOMNode(this.refs.panel);
			if (dom_panel == null) {
				return;
			}
			var val = dom_panel.compareDocumentPosition(e.target);
			if (val != 20 & val != 0) {
				this.setState({display_front_panel: false});
			}
		}
	},
	componentDidMount: function() {
		if (this.state.display_front_panel) {
			document.addEventListener("click", this.onDocumentClick);
		}
	},
	componentDidUpdate: function() {
		if (this.state.display_front_panel) {
			document.addEventListener("click", this.onDocumentClick);
		} else {
			document.removeEventListener("click", this.onDocumentClick);
		}
	},
	componentWillUnmount: function() {
		if (this.state.display_front_panel) {
			document.removeEventListener("click", this.onDocumentClick);
		}
	},
	render: function() {
		var front_panel = null;
		if (this.state.display_front_panel) {
			var style = {
				position: "absolute",
				display: "block",
				/*top: "100%",*/
				/*left: 0,*/
				zIndex: 1000,
				/*float: "left",*/
				/*minWidth: "160px",*/
				/*maxWidth: "300px",*/
				padding: "5px",
				margin: "2px 0 0",
				backgroundColor: "#ffffff",
				border: "1px solid rgba(0, 0, 0, 0.2)",
				borderRadius: "6px",
				boxShadow: "0 5px 10px rgba(0, 0, 0, 0.2)",
				backgroundClip: "padding-box",
			};

			front_panel = (<div style={style} ref="panel">
				<div style={{width: 300}}>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas dictum lorem nec efficitur dapibus. Phasellus risus nisl, venenatis eu quam vitae, placerat placerat libero. Integer auctor lectus metus, vel malesuada dui facilisis non. Proin sagittis arcu eu fringilla rutrum. Phasellus faucibus est sed enim molestie vulputate. Nullam magna nulla, laoreet a lorem in, tempus vehicula magna. Phasellus et egestas eros, id vestibulum massa. Duis accumsan massa in nulla mollis placerat. Maecenas mollis lectus in risus facilisis, vel viverra lacus vulputate.</div>
			</div>);
			// {this.props.children}
		}

		return (<div>
			<button className="btn" onClick={this.btnShowOnClick} ref="btnhideshow">Show</button>
			{front_panel}
		</div>);
	}
});

/**
 * Simple and static TreeView
 * TODO dynamic branch loading (and/or) load dir content after open dir
*/
async.DemoTreeView = createReactClass({
	render: function() {
		var content = [
			"Item 1",
			{name: "Item 2, Sub list", content: [
				{name: "Another Item 1", content: [
					"Another Sub Item 1",
					"Another Sub Item 2",
				], default_opened: true},
				"Another Item 2",
			], default_opened: true},
			"Item 3",
		];
		var processor = function(actual_name, external_ref) {
			return <span><i className="icon-folder-close"></i> {actual_name}</span>;
		}

		return <async.TreeView content={content} processor={processor} />
	}
});

var TreeViewItem = createReactClass({
	propTypes: {
		item: PropTypes.oneOfType([
			PropTypes.string,
			PropTypes.object,
		]).isRequired,
		processor: PropTypes.func.isRequired,
		is_root: PropTypes.bool,
		is_first_root: PropTypes.bool,
	},
	getInitialState: function() {
		return {
			is_open: null,
		};
	},
	onToogleOpenClose: function(e) {
		e.preventDefault();
		e.stopPropagation();
		if (this.state.is_open == null) {
			if (this.props.item.default_opened) {
				this.setState({is_open: false});
			} else {
				this.setState({is_open: true});
			}
		} else {
			this.setState({is_open: !this.state.is_open});
		}
	},
	render: function() {
		var item = this.props.item;

		var btn_open_close = null;
		var display = null;
		var sub_items = [];

		if (typeof item != "string") {
			var btn = function(icon_chevron_class) {
				return (<span style={{
						cursor: "pointer",
						marginRight: 6,
					}}
					onClick={this.onToogleOpenClose}>
						<i className={icon_chevron_class}></i>
				</span>);
			}.bind(this);

			if (item.content != null) {
				if (item.content.length > 0) {
					if (this.props.is_root) {
						for (var pos in item.content) {
							sub_items.push(<TreeViewItem key={pos} item={item.content[pos]} processor={this.props.processor} is_first_root={true} />);
						}
					} else {
						display = this.props.processor(item.name, item.external_ref);

						var is_really_open = item.default_opened;
						if (this.state.is_open != null) {
							is_really_open = this.state.is_open;
						}
						if (is_really_open) {
							btn_open_close = btn("icon-chevron-down");
							for (var pos in item.content) {
								sub_items.push(<TreeViewItem key={pos} item={item.content[pos]} processor={this.props.processor} />);
							}
						} else {
							btn_open_close = btn("icon-chevron-right");
						}
					}
				}
			}
		}

		if (display == null && !this.props.is_root) {
			display = <span style={{marginLeft: 20,}}>{this.props.processor(item, null)}</span>;
		}

		var node_style = null;
		var sub_nodes_style = null;

		/*border: "1px solid #000",*/
		if (!this.props.is_root) {
			node_style = {
				marginTop: 3,
				marginBottom: 3,
			};

			if (this.props.is_first_root) {
				node_style.marginLeft = 5;
			} else {
				node_style.marginLeft = 10;
			}

			sub_nodes_style = {
				marginLeft: 10,
			};
		}

		return (<div style={node_style}>
			{btn_open_close}
			{display}
			<div style={sub_nodes_style}>{sub_items}</div>
		</div>);
	}
});

async.TreeView = createReactClass({
	propTypes: {
		/**
		 * Is like [item1, item2, item3]
		 * Each item is like: {name: "", external_ref: "external_object_reference", content: [sub content], default_opened: false}
		 * With all is not mandatory, a simple string can be used.
		 */
		content: PropTypes.array.isRequired,
		 /**
		  * callback(actual_name, external_ref) return <ToDisplayInTreeViewNode>
		  */
		processor: PropTypes.func,
	},
	render: function() {
		var content = this.props.content;
		var processor = this.props.processor;

		if (processor == null) {
			processor = function(actual_name, external_ref) {
				return actual_name;
			}
		}
		var sub_item = {
			name: "",
			content: content,
			default_opened: true,
		};

		return (<div style={{border: "1px solid #888", }}>
			<TreeViewItem item={sub_item} processor={processor} is_root={true} />
		</div>);
	}
});

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

