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

/**
 * Wait 300 ms before show it.
 */
async.PageLoadingProgressBar = React.createClass({
	getInitialState: function() {
		return {timer: null, show_bar: false};
	},
	componentWillMount: function() {
		this.setState({timer: setTimeout(this.onTimerDone, 300)});
	},
	onTimerDone: function() {
		this.setState({show_bar: true, timer: null});
	},
	componentWillUnmount: function() {
		if (this.state.timer) {
			clearInterval(this.state.timer);
		}
	},
	render: function() {
		if (this.state.show_bar) {
			return (<div className="progress progress-striped progress-info active"><div className="bar" style={{width: "100%"}}></div></div>);
		} else {
			return (<span />);
		}
	}
});

async.AlertBox = React.createClass({
	render: function() {
		var title = null;
		if (this.props.title) {
			title = (<div><h4>{this.props.title}</h4></div>);
		}
		var classes = classNames("alert");
		if (this.props.color) {
			classes = classNames("alert", this.props.color);
		}
		return (<div className={classes}>{title}{this.props.children}</div>);
	}
});

async.AlertInfoBox = React.createClass({
	render: function() {
		return (<async.AlertBox color="alert-info" {...this.props} />);
	}
});

async.AlertErrorBox = React.createClass({
	render: function() {
		return (<async.AlertBox color="alert-error" {...this.props} />);
	}
});

async.FormControlGroup = React.createClass({
	render: function() {
		var label = null;
		if (this.props.label) {
			label = (<label className="control-label">{this.props.label}</label>);
		}

		return (<div className="control-group">
			{label}
			<div className="controls">
				{this.props.children}
			</div>
		</div>);
	}
});

async.BtnEnableDisable = React.createClass({
	propTypes: {
		simplelabel: React.PropTypes.bool.isRequired,
		enabled: React.PropTypes.bool.isRequired,
		labelenabled: React.PropTypes.string.isRequired,
		labeldisabled: React.PropTypes.string.isRequired,
		onEnable: React.PropTypes.func,
		onDisable: React.PropTypes.func,
		reference: React.PropTypes.string,
		iconcircle: React.PropTypes.bool,
	},
	getInitialState: function() {
		return {pending_changes: false};
	},
	onClickSetEnable: function() {
		if (this.state.pending_changes) {
			return;
		}
		this.setState({pending_changes: true});
		this.props.onEnable(this.props.reference);
	},
	onClickSetDisable: function() {
		if (this.state.pending_changes) {
			return;
		}
		this.setState({pending_changes: true});
		this.props.onDisable(this.props.reference);
	},
	componentWillReceiveProps: function() {
		this.setState({pending_changes: false});
	},
	render: function() {
		var class_name_icon = classNames("icon-white", {
			"icon-stop":         this.props.enabled & !this.props.iconcircle,
			"icon-play":        !this.props.enabled & !this.props.iconcircle,
			"icon-ok-circle":    this.props.enabled &  this.props.iconcircle,
			"icon-ban-circle":  !this.props.enabled &  this.props.iconcircle,
		});

		if (this.props.simplelabel) {
			if (this.props.enabled) {
				return (<span><strong>{this.props.labelenabled}</strong></span>);
			} else {
				return (<span className="muted"><strong>{this.props.labeldisabled}</strong></span>);
			}
		}
		 
		if (this.props.enabled) {
			var btn_classes = classNames("btn", "btn-mini", {
				disabled: this.state.pending_changes,
				"btn-danger": !this.props.iconcircle,
				"btn-success": this.props.iconcircle,
			});
			return (<button className={btn_classes} onClick={this.onClickSetDisable}><i className={class_name_icon}></i> {this.props.labelenabled}</button>);
		} else {
			var btn_classes = classNames("btn", "btn-mini", {
				disabled: this.state.pending_changes,
				"btn-success": !this.props.iconcircle,
			});
			return (<button className={btn_classes} onClick={this.onClickSetEnable}><i className={class_name_icon}></i> {this.props.labeldisabled}</button>);
		}
	},
});

async.BtnDelete = React.createClass({
	propTypes: {
		label: React.PropTypes.string,
		enabled: React.PropTypes.bool.isRequired,
		onClickDelete: React.PropTypes.func.isRequired,
		reference: React.PropTypes.string,
	},
	getInitialState: function() {
		return {pending_changes: false};
	},
	onClickDelete: function() {
		if (this.state.pending_changes) {
			return;
		}
		if (!this.props.enabled) {
			return;
		}
		this.setState({pending_changes: true});
		this.props.onClickDelete(this.props.reference);
	},
	componentWillReceiveProps: function() {
		this.setState({pending_changes: false});
	},
	render: function() {
		var btn_classes = classNames("btn", "btn-mini", "btn-danger", {
			disabled: this.state.pending_changes | !this.props.enabled,
		});
		return (<button className={btn_classes} onClick={this.onClickDelete}><i className="icon-trash icon-white"></i> {this.props.label}</button>);
	},
});

async.ButtonSort = React.createClass({
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

async.LabelBoolean = React.createClass({
	propTypes: {
		label_true: 	React.PropTypes.string.isRequired,
		label_false: 	React.PropTypes.string.isRequired,
		value: 			React.PropTypes.bool.isRequired,
		inverse: 		React.PropTypes.bool,
	},
	render: function() {
		var value = this.props.value;
		var inverse = false;
		if (this.props.inverse) {
			inverse = true;
		}

		var label = i18n(this.props.label_false);
		if (value) {
			label = i18n(this.props.label_true);
		}

		var span_class = classNames("badge", {
			"badge-success": value == inverse,
			"badge-important": value != inverse,
		});
		var icon = classNames("icon-white", {
			"icon-remove-circle": ! value,
			"icon-ok-circle": value,
		});

		return (<span className={span_class}><i className={icon}></i> {label}</span>)
	},
});

async.JsonCode = React.createClass({
	propTypes: {
		i18nlabel:		React.PropTypes.string.isRequired,
		json: 			React.PropTypes.string.isRequired,
	},
	render: function() {
		var i18nlabel = (<span className="jsontitle"> {i18n(this.props.i18nlabel)} </span>);

		return (<div>
			<code className="json" style={{marginTop: 10}}>
				<i className="icon-indent-left"></i>
				{i18nlabel}
				{JSON.stringify(this.props.json, null, " ")}
			</code>
		</div>);
	},
});

async.appversion = "master master";

async.makeGitHubLink = function(javaclass) {
	if (javaclass == null) {
		return null;
	}
	if ((javaclass.indexOf("hd3gtv.") != 0) & (javaclass.indexOf("controllers.") != 0)) {
		return null;
	}
	var dollar_pos = javaclass.indexOf("$");
	if (dollar_pos > -1) {
		javaclass = javaclass.substring(0, dollar_pos);
	}

	return "https://github.com/hdsdi3g/MyDMAM/blob/" + async.appversion.substring(async.appversion.lastIndexOf(" ") + 1) + "/app/" + javaclass.replace(/\./g, "/") + ".java";
}

async.JavaClassNameLink = React.createClass({
	onClickLink: function(e) {
		e.stopPropagation();
	},
	render: function() {
		var javaclass = this.props.javaclass;
		if (javaclass == null) {
			return (<strong><em className="muted">Void</em></strong>);
		}

		var href = async.makeGitHubLink(javaclass);

		if (href == null) {
			return (<span>{javaclass}</span>);
		}

		var icon_style = {
			height: 14,
			lineHeight: 14,
			marginTop: 1,
			verticalAlign: "text-top",
			width: 14,
		};

		return (<span>
			<a href={href} target="_blank" onClick={this.onClickLink}>
				<img src={mydmam.urlimgs.github_favicon} style={icon_style} />
				&nbsp;
				<abbr title={javaclass}>
					{javaclass.substring(javaclass.lastIndexOf(".") + 1)}
				</abbr>
			</a>
		</span>);
	},
});

async.JavaStackTrace = React.createClass({
	onClickLink: function(e) {
		e.stopPropagation();
	},
	getStacktrace: function(processing_error, i) {
		var lines = [];
		lines.push(<div key={i++}>{processing_error["class"]}: {processing_error.message}</div>);
		for (var pos = 0; pos < processing_error.stacktrace.length; pos++) {
			var trace = processing_error.stacktrace[pos];

			if (trace.line === -2) {
				lines.push(<div key={i++}>
					&nbsp;at&nbsp;{trace["class"]}.{trace.method}(Native Method)
				</div>);
			} else if (trace.file) {
				var url = async.makeGitHubLink(trace["class"]);

				if (trace.line >= 0) {
					if (url) {
						lines.push(<div key={i++}>
							&nbsp;at&nbsp;{trace["class"]}.{trace.method}(<a href={url + "#L" + trace.line} target="_blank">{trace.file}:{trace.line}</a>)
						</div>);
					} else {
						lines.push(<div key={i++}>
							&nbsp;at&nbsp;{trace["class"]}.{trace.method}({trace.file}:{trace.line})
						</div>);
					}
				} else {
					if (url) {
						lines.push(<div key={i++}>
							&nbsp;at&nbsp;{trace["class"]}.{trace.method}(<a href={url} target="_blank">{trace.file}</a>)
						</div>);
					} else {
						lines.push(<div key={i++}>
							&nbsp;at&nbsp;{trace["class"]}.{trace.method}({trace.file})
						</div>);
					}
				}
			} else {
				lines.push(<div key={i++}>
					&nbsp;at&nbsp;{trace["class"]}.{trace.method}(Unknown Source)
				</div>);
			}
		}
		
		if (processing_error.cause) {
			lines.push(<div key={i++}>Caused by:</div>);
			return lines.concat(broker.getStacktrace(processing_error.cause, i));
		}
		return lines;
	},
	render: function() {
		if (this.props.processing_error == null) {
			return null;
		}

		return (<div className="stacktrace" onClick={this.onClickLink}>{this.getStacktrace(this.props.processing_error, 0)}</div>);
	},
});

async.SearchInputBox = React.createClass({
	getInitialState: function() {
		return {timer: null};
	},
	onTimerDone: function() {
		this.setState({timer: null});
		this.props.onKeyPress(React.findDOMNode(this.refs.inputbox).value);
	},
	onKeyPress: function() {
		if (this.state.timer) {
			clearInterval(this.state.timer);
		}
		this.setState({timer: setTimeout(this.onTimerDone, 500)});
	},
	componentWillUnmount: function() {
		if (this.state.timer) {
			clearInterval(this.state.timer);
		}
	},
	render: function() {
		return (<span style={{"fontSize" : 13}}><input
			type="text"
			ref="inputbox"
			placeholder={i18n("maingrid.search")}
			className="input-medium search-query"
			style={this.props.style}
			onKeyUp={this.onKeyPress} />
		</span>);
	}
});

async.NavTabsLink = React.createClass({
	onClick: function(e) {
		e.preventDefault();
		this.props.onActiveChange(this.props.pos);
		$(React.findDOMNode(this.refs.tab)).blur();
	},
	render: function() {
		var icon = null;
		if (this.props.icon) {
			icon = (<span><i className={this.props.icon}></i>&nbsp;</span>);
		}
		var label = null;
		if (this.props.i18nlabel) {
			label = i18n(this.props.i18nlabel);
		}
		return (<a href={location.hash} onClick={this.onClick} ref="tab">{icon}{label}</a>);
	},
});

async.NavTabs = React.createClass({
	onActiveChange: function(new_pos) {
		if (new_pos < 0) {
			return;
		}
		if (new_pos > this.props.content.length) {
			return;
		}
		this.props.onChange(new_pos);
	},
	render: function() {
		if (this.props.content == null) {
			return (<span />);
		}
		if (this.props.content.length === 0) {
			return (<span />);
		}

		var content = [];
		for (var pos in this.props.content) {
			var item = this.props.content[pos];
			var li_class = classNames({
				"active": 		(this.props.active == pos),
				"pull-right": 	item.pullright,
			});
			content.push(<li className={li_class} key={pos}>
				<async.NavTabsLink pos={pos} i18nlabel={item.i18nlabel} icon={item.icon} onActiveChange={this.onActiveChange} />
			</li>);
		}

		return (<ul className="nav nav-tabs">
			{content}
		</ul>);
	},
});

async.PageHeaderTitle = React.createClass({
	getInitialState: function() {
		return {active_tab: 0};
	},
	componentDidMount: function() {
		this.setDocumentTitle();
	},
	componentDidUpdate: function(dd) {
		this.setDocumentTitle();
	},
	setDocumentTitle: function() {
		var tab_name = "";

		if (this.props.tabs) {
			tab_name = this.props.tabs[this.state.active_tab].i18nlabel;
			if (tab_name) {
				tab_name = i18n(tab_name) + " :: ";
			}
		}

		var new_title = null;
		if (this.props.title) {
			new_title = tab_name + this.props.title + " :: " + i18n("site.name");
		} else {
			new_title = tab_name + i18n("site.name");
		}
		if (document.title != new_title) {
			document.title = new_title
		}
	},
	componentWillUnmount: function() {
		document.title = i18n("site.name");
	},
	onChangeTab: function(new_pos){
		this.setState({active_tab: new_pos});
	},
	render: function() {
		var p_lead = null;
		if (this.props.title) {
			p_lead = (<p className="lead">{this.props.title}</p>);
		}

		var main_class_name = classNames("container");
		if (this.props.fluid) {
			main_class_name = classNames("container-fluid");
		}

		var tabs_content = null;
		var selected_content = null;
		if (this.props.tabs) {
			var navtabscontent = this.props.tabs;
			tabs_content = (<mydmam.async.NavTabs content={navtabscontent} onChange={this.onChangeTab} active={this.state.active_tab} />);
			selected_content = this.props.tabs[this.state.active_tab].content;
		}

		return (<div className={main_class_name}>
			{p_lead}
			{tabs_content}
			<div>{selected_content}</div>
			<div>{this.props.children}</div>
		</div>);
	}
});