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

async.DemoColorTemplate = createReactClass({
	getInitialState: function() {
		return {
			color_luma: 0,
			color_hue: 0,
		};
	},
	onClickNothing: function(e) {
		e.preventDefault();
	},
	onMoveSlider: function(e) {
		e.preventDefault();
		var value_luma = parseInt(ReactDOM.findDOMNode(this.refs.color_luma).value, 10);
		var value_hue = parseInt(ReactDOM.findDOMNode(this.refs.color_hue).value, 10);
		this.setState({
			color_luma: value_luma,
			color_hue: value_hue,
		});
	},
	componentDidMount: function() {
		/*var canvas = ReactDOM.findDOMNode(this.refs.canvas);
		var ctx = canvas.getContext("2d");
		for (var i = 0; i < 360; i++) {
			ctx.beginPath();
			ctx.moveTo(i, 0);
			ctx.lineTo(i, 30);
			ctx.lineWidth = 1;
			ctx.strokeStyle = cssHSL(i, 80, 60);
			ctx.stroke();
		}*/
	},
	render: function() {
		var bkg_color = (this.state.color_luma * 0.35) + 14;  /* +/- 13>31 */
		var frt_color = (this.state.color_luma * 0.35) + 80;

		if (this.state.color_luma > 50) {
			bkg_color = (this.state.color_luma * 0.7) + 30;
			frt_color = (this.state.color_luma * 0.1) + 5;
		}

		var bkg_style = {
			backgroundColor: cssHSL(0, 0, bkg_color),
			color: cssHSL(0, 0, frt_color),
			padding: 12,
		};

		var well_bkg_color = bkg_color + 10;
		var well_bkg_sat = 30;
		var well_shadow = cssHSL(0, 0, well_bkg_color - 12, 0.8);
		if (this.state.color_luma > 50) {
			well_bkg_color = bkg_color - 8;
			well_bkg_sat = 30;
			well_shadow = cssHSL(0, 0, well_bkg_color - 5, 0.8);
		}

		var well_style = {
			backgroundColor: cssHSL(this.state.color_hue, well_bkg_sat, well_bkg_color),
			color: cssHSL(0, 0, frt_color),
			display: "block",
			paddingLeft: "8px",
			paddingRight: "8px",
			paddingTop: "4px",
			paddingBottom: "4px",
			textShadow: "0px -1px 0 " + well_shadow,
			borderRadius: "3px",
			marginTop: "12px",
			border: "1px solid " + cssHSL(this.state.color_hue, well_bkg_sat, well_bkg_color - 8),
		};

		var btn_border = 70;
		if (this.state.color_luma > 50) {
			btn_border = 40;
		}
		var btn_style = {
			color: cssHSL(0, 0, frt_color),
			backgroundColor: cssHSL(0, 0, bkg_color),
			paddingLeft: "8px",
			paddingRight: "8px",
			paddingTop: "5px",
			paddingBottom: "4px",
			marginLeft: "6px",
			/*textShadow: "0px -1px 0 " + cssHSL(0, 0, 50, 0.2),*/
			border: "1px solid " + cssHSL(this.state.color_hue, 60, btn_border, 0.5),
			borderRadius: "5px",
			textDecorationLine: "none",
		};

		var btn_hover_style = Object.assign({}, btn_style);
		btn_hover_style.border = "1px solid " + cssHSL(this.state.color_hue, 50, btn_border - 10, 0.5);
		btn_hover_style.backgroundColor = cssHSL(this.state.color_hue, well_bkg_sat, well_bkg_color);

		var btn_active_style = Object.assign({}, btn_hover_style);
		btn_active_style.background = cssGrad("to bottom", [
			{c: cssHSL(this.state.color_hue, well_bkg_sat, well_bkg_color - 15), step: 0},
			{c: cssHSL(this.state.color_hue, well_bkg_sat, well_bkg_color), step: 30},
			{c: cssHSL(this.state.color_hue, well_bkg_sat, well_bkg_color), step: 100},
		]);
		btn_active_style.border = "1px solid " + cssHSL(this.state.color_hue, 30, btn_border - 20, 0.5);
		if (this.state.color_luma > 40) {
			btn_active_style.background = cssGrad("to bottom", [
				{c: cssHSL(this.state.color_hue, well_bkg_sat - 10, well_bkg_color - 20), step: 0},
				{c: cssHSL(this.state.color_hue, well_bkg_sat, well_bkg_color - 10), step: 15},
				{c: cssHSL(this.state.color_hue, well_bkg_sat, well_bkg_color - 5), step: 100},
			]);
			btn_active_style.border = "1px solid " + cssHSL(this.state.color_hue, 40, btn_border - 20, 0.5);
		}

		var link_color = cssHSL(this.state.color_hue + 10, 50, 60);
		if (this.state.color_luma < 50 && this.state.color_hue > 195 && this.state.color_hue < 280) {
			/** dark & blue */
			link_color = cssHSL(this.state.color_hue + 10, 40, 70);
		}
		if (this.state.color_luma > 50) {
			/** mid */
			if (this.state.color_hue > 20 && this.state.color_hue < 200) {
				/* orange -> blue */
				link_color = cssHSL(this.state.color_hue + 10, 70, 20);
			} else {
				/* red -> orange / blue -> violet */
				link_color = cssHSL(this.state.color_hue + 10, 80, 15);
			}
		}
		if (this.state.color_luma > 75) {
			/** high */
			link_color = cssHSL(this.state.color_hue + 10, 100, 30);
		}

		var link_style = {
			color: link_color,
			textDecorationColor: link_color,
			textDecorationStyle: "dotted",
			textDecorationLine: "underline",
		};

		var hover_link_style = {
			color: link_color,
			textDecorationColor: link_color,
			textDecorationStyle: "solid",
			textDecorationLine: "underline",
		};

		var block_color = bkg_color + 5;
		if (this.state.color_luma > 50) {
			block_color = bkg_color - 5;
			if (this.state.color_luma > 90) {
				block_color = bkg_color - 8;
			}
		}

		var block_style = {
			backgroundColor: cssHSL(0, 0, block_color),
			padding: 12,
			marginTop: "12px",
			borderRadius: "3px",
		};

		var icon_style = Object.assign({}, btn_style);
		icon_style.display = "inline-block";
		icon_style.paddingTop = "3px";
		icon_style.paddingBottom = "3px";
		icon_style.cursor = "pointer";
		icon_style.borderRadius = "5px";

		var icon_class = classNames("icon-off", {
			"icon-white": this.state.color_luma < 50,
		});

		var btn_style_well = Object.assign({}, btn_style);
		btn_style_well.backgroundColor = well_style.backgroundColor;
		var btn_hover_style_well = Object.assign({}, btn_hover_style);
		btn_hover_style_well.backgroundColor = cssHSL(this.state.color_hue, well_bkg_sat, well_bkg_color - 3);

		var btn_style_block = Object.assign({}, btn_style);
		btn_style_block.backgroundColor = block_style.backgroundColor;

		// TODO navbar-inverse + body background
		
		//<canvas ref="canvas" width="360" height="30"></canvas>
		return (<div>
			<div style={bkg_style}>
				
				This is a text.
				<a style={link_style} href="#" onClick={this.onClickNothing}>This is a link</a> <a style={hover_link_style} href="#" onClick={this.onClickNothing}>Hover link</a>
				<a style={btn_style} href="#" onClick={this.onClickNothing}>This is an important button</a>
				<a style={btn_hover_style} href="#" onClick={this.onClickNothing}>Hover button</a>
				<a style={btn_active_style} href="#" onClick={this.onClickNothing}>Active button</a>
				<div style={icon_style}><i className={icon_class}></i></div>

				<br />
				<div style={well_style}>
					This is a well <a style={link_style} href="#" onClick={this.onClickNothing}>This is a link in a well</a> <a style={hover_link_style} href="#" onClick={this.onClickNothing}>Hover link</a>
					<a style={btn_style_well} href="#" onClick={this.onClickNothing}>This is an important button</a>
					<a style={btn_hover_style_well} href="#" onClick={this.onClickNothing}>Hover button</a>
					<a style={btn_active_style} href="#" onClick={this.onClickNothing}>Active button</a>
					<div style={icon_style}><i className={icon_class}></i></div>
				</div>
				<div style={block_style}>
					This is a block <a style={link_style} href="#" onClick={this.onClickNothing}>This is a link in a block</a> <a style={hover_link_style} href="#" onClick={this.onClickNothing}>Hover link</a>
					<a style={btn_style_block} href="#" onClick={this.onClickNothing}>This is an important button</a>
					<a style={btn_hover_style} href="#" onClick={this.onClickNothing}>Hover button</a>
					<a style={btn_active_style} href="#" onClick={this.onClickNothing}>Active button</a>
					<div style={icon_style}><i className={icon_class}></i></div>
				</div>
			</div>
			<input type="range" ref="color_luma" min="0" max="100" defaultValue={0} onChange={this.onMoveSlider} /> {this.state.color_luma}<br />
			<input type="range" ref="color_hue" min="0" max="359" defaultValue={0} onChange={this.onMoveSlider} /> {this.state.color_hue}<br />
		</div>);

		//var defs = [];
		/*defs.push({ name: "assbk", content: ["7ec499", "000000", "ffffff", "148b3e", "f4f4f4", "", "", "", "", ""], });*/
		/*defs.push({ name: "avd1", content: ["", "969696", "181818", "7e9dbf", "1e1e1e", "515c67", "353535", "7a7a7a", "", ""], });
		defs.push({ name: "avd2", content: ["", "b3b3b3", "272727", "7e9dbf", "2e2e2e", "495b70", "4b4b4b", "939393", "", ""], });
		defs.push({ name: "avd3", content: ["", "cccccc", "373737", "7e9dbf", "414141", "445569", "6c6c6c", "ababab", "", ""], });
		defs.push({ name: "avd4", content: ["", "1a1a1a", "535353", "000000", "666666", "445569", "959595", "222222", "", ""], });
		defs.push({ name: "avd5", content: ["", "1a1a1a", "6c6c6c", "000000", "818181", "8c9eb1", "b2b2b2", "2A2A2A", "", ""], });
		defs.push({ name: "avd6", content: ["", "1a1a1a", "838383", "000000", "959595", "a0b0c1", "d1d1d1", "323232", "", ""], });*/
		/*defs.push({ name: "cntmo1", content: ["", "000000", "f0f0f0", "2f4fcc", "f7f7f7", "", "", "4d4d4d", "d4d4d4", ""], });
		defs.push({ name: "cntmo2", content: ["", "e8e8e8", "292929", "3686bf", "24242a", "", "", "4d4d4d", "242529", ""], });
		defs.push({ name: "frmio", content: ["", "e6ecf8", "2f3440", "bdc1d1", "3c4054", "1e212a", "252835", "838ba1", "ffffff", "524afb"], });
		defs.push({ name: "kyfw", content: ["", "c6c4c3", "1d1d1d", "e8e8e8", "232323", "3ca8f4", "8d8d8d", "3197fb", "262626", ""], });
		defs.push({ name: "ppro2", content: ["", "a2a2a2", "202020", "2176ce", "1b1b1b", "3c4144", "", "a7a7a7", "", ""], });
		defs.push({ name: "ppro1", content: ["5b5b5b", "787878", "3c3c3c", "529ad3", "3a3a3a", "134c7a", "494949", "b7b7b7", "", ""], });*/

		/*defs.push({
			name: "btn-avd",
			content: ["5f7792", "587f7e", "558656", "875b95", "a9963f", "be8e1c", ],
		});*/

		/*var blocks = [];
		for (var pos in defs) {
			var sub_block = [];
			// style={{width: "50px", height: "50px", color: f_color, backgroundColor: b_color}}
			var def = defs[pos];

			for (var pos2 in def.content) {
				var color = "#" + def.content[pos2];
				sub_block.push(<td key={pos2} style={{backgroundColor: color, margin: 0, padding: 0}}>
					<span style={{margin: 0, paddingLeft: "10px", paddingRight: "10px"}}>&nbsp;</span>
				</td>);
			}

			blocks.push(<tr key={pos}>
				<td key={-1}><span style={{marginLeft: "5px", marginRight: "5px"}}>{def.name}</span></td>
				{sub_block}
			</tr>);
		}*/
		//return (<table style={{border: 0}}><tbody>{blocks}</tbody></table>);

		/*
		var b_color = mydmam.lookandfeel.get("bgnd2");
		var f_color = mydmam.lookandfeel.get("frt2");

		return (<div>
			<div style={{width: "100px", height: "100px", color: f_color, backgroundColor: b_color}}>
				Text
			</div>
			<br />
			<button onClick={this.onClickChange} className="btn">Change</button>
		</div>);*/
	}
});


/*
Panels and item list
https://developer.mozilla.org/fr/docs/Web/CSS/CSS_Grid_Layout
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
					<async.ResizablePane mode="width" initial_width="150px" className="display-table-cell">
						<aside>Left panel</aside>
					</async.ResizablePane>
					<div className="display-table-cell">
						<header>Option's menu</header>
						<section className="article-list">{article_list}</section>
						<footer>Pagination</footer>
					</div>
					<async.ResizablePane mode="width" drag_handle="left" initial_width="200px" className="display-table-cell">
						<aside>Right panel</aside>
					</async.ResizablePane>
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
	propTypes: {
		mode: PropTypes.string.isRequired,
		children: PropTypes.node.isRequired,
		//className: PropTypes
		initial_width: PropTypes.oneOfType([
			PropTypes.string,
			PropTypes.number,
		]),
		initial_height: PropTypes.oneOfType([
			PropTypes.string,
			PropTypes.number,
		]),
		drag_handle: PropTypes.string,
	},
	getDefaultProps: function() {
		return {
			drag_handle: "right",
		};
	},
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
		var actual_w = this.state.start_width;
		var actual_h = this.state.start_height;
		var new_w = actual_w + new_x - this.state.start_x;
		if (this.props.drag_handle == "left") {
			new_w = actual_w + this.state.start_x - new_x;
		}
		var new_h = this.state.start_height + new_y - this.state.start_y;

		if (this.props.mode == "width") {
			this.setState({
				width: new_w,
				height: actual_h,
			});
		} else if (this.props.mode == "height") {
			this.setState({
				width: actual_w,
				height: new_h,
			});
		} else {
			if (this.state.behavior == "width") {
				this.setState({
					width: new_w,
					height: actual_h,
				});
			} else if (this.state.behavior == "height") {
				this.setState({
					width: actual_w,
					height: new_h,
				});
			} else {
				this.setState({
					width: new_w,
					height: new_h,
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
		} else if (this.state.width == null && this.state.height == null){
			if (this.props.initial_width != null) {
				style.width = this.props.initial_width;
			}
			if (this.props.initial_height != null) {
				style.height = this.props.initial_height;
			}
		}

		var border_debug = null; //"1px solid #888";

		var handles = [];
		var div_style_children = null;
		if (mode == "width") {
			var hdl_style = {
				width: "20px",
				height: "100%",
				position: "absolute",
				border: border_debug,
				top: 0,
				cursor: "ew-resize",
			};

			if (this.props.drag_handle == "left") {
				hdl_style["left"] = 0;
				div_style_children = {marginLeft: "15px"};
			} else {
				hdl_style["right"] = 0;
			}

			handles.push(<ResizerHandle style={hdl_style} key="w" onStartResize={this.onStartResize} behavior="width" />);
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
			var hdl_style = {
				width: "20px",
				height: "100%",
				position: "absolute",
				border: border_debug,
				top: 0,
				cursor: "ew-resize",
			};

			if (this.props.drag_handle == "left") {
				hdl_style["left"] = 0;
				div_style_children = {marginLeft: "15px"};
			} else {
				hdl_style["right"] = 0;
			}

			handles.push(<ResizerHandle style={hdl_style} key="w" onStartResize={this.onStartResize} behavior="width" />);
			handles.push(<ResizerHandle style={{
				width: "100%",
				height: "20px",
				position: "absolute",
				border: border_debug,
				left: 0,
				bottom: 0,
				cursor: "ns-resize",
			}} key="h" onStartResize={this.onStartResize} behavior="height" />);

			hdl_style = {
				width: "20px",
				height: "20px",
				position: "absolute",
				border: border_debug,
				bottom: 0,
				cursor: "nwse-resize",
			};

			if (this.props.drag_handle == "left") {
				hdl_style["left"] = 0;
			} else {
				hdl_style["right"] = 0;
			}

			handles.push(<ResizerHandle style={hdl_style} key="c" onStartResize={this.onStartResize} behavior="corner" />);
		}

		return (<div ref="resizeable" style={style} className={this.props.className}>
			<div style={div_style_children}>{this.props.children}</div>
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

