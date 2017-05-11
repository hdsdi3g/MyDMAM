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
async.PageLoadingProgressBar = createReactClass({
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

async.AlertBox = createReactClass({
	onBtnCloseClick: function(e) {
		e.preventDefault();
		this.props.onClose();
	},
	render: function() {
		var title = null;
		if (this.props.title) {
			title = (<h4>{this.props.title}</h4>);
		}
		
		var classes = classNames("alert", "alert-block", this.props.color);

		var btn_close = null;
		if (this.props.onClose) {
			btn_close = (<button type="button" className="close" onClick={this.onBtnCloseClick}>&times;</button>);
		}
		
		return (<div className={classes}>
			{btn_close}
			{title}
			{this.props.children}
		</div>);
	}
});

async.AlertInfoBox = createReactClass({
	render: function() {
		return (<async.AlertBox color="alert-info" {...this.props} />);
	}
});

async.AlertErrorBox = createReactClass({
	render: function() {
		return (<async.AlertBox color="alert-error" {...this.props} />);
	}
});

async.FormControlGroup = createReactClass({
	render: function() {
		var label = null;

		var classname_label = classNames("control-label", {
			"text-error": this.props.stronglabel,
		});

		if (this.props.label) {
			label = (<label className={classname_label}>{this.props.label}</label>);
		}

		return (<div className="control-group">
			{label}
			<div className="controls">
				{this.props.children}
			</div>
		</div>);
	}
});

async.BtnEnableDisable = createReactClass({
	propTypes: {
		simplelabel: PropTypes.bool.isRequired,
		enabled: PropTypes.bool.isRequired,
		labelenabled: PropTypes.string.isRequired,
		labeldisabled: PropTypes.string.isRequired,
		onEnable: PropTypes.func,
		onDisable: PropTypes.func,
		reference: PropTypes.string,
		iconcircle: PropTypes.bool,
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
		var class_name_icon = classNames({
			"icon-white":       !(!this.props.enabled &  this.props.iconcircle),
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

async.CheckboxItem = createReactClass({
	propTypes: {
		reference: PropTypes.string.isRequired,
		checked: PropTypes.bool.isRequired,
		onChangeCheck: PropTypes.func.isRequired,
	},
	onClickCB: function(e) {
   		$(ReactDOM.findDOMNode(this.refs.cb)).blur();
   		this.props.onChangeCheck(this.props.reference, !this.props.checked);
	},
	render: function() {
		return (
			<label className="checkbox">
	        	<input type="checkbox" ref="cb" defaultChecked={this.props.checked} onChange={this.onClickCB} /> {this.props.children}
			</label>
		);
	}
});

async.BtnDelete = createReactClass({
	propTypes: {
		label: PropTypes.string,
		enabled: PropTypes.bool.isRequired,
		onClickDelete: PropTypes.func.isRequired,
		reference: PropTypes.string,
		hide_for_disable: PropTypes.bool,
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
		if (this.props.hide_for_disable && !this.props.enabled) {
			return (<span />);
		}

		var btn_classes = classNames("btn", "btn-mini", "btn-danger", {
			disabled: this.state.pending_changes | !this.props.enabled,
		});
		return (<button className={btn_classes} onClick={this.onClickDelete}><i className="icon-trash icon-white"></i> {this.props.label}</button>);
	},
});

async.SimpleBtn = createReactClass({
	propTypes: {
		enabled: PropTypes.bool.isRequired,
		onClick: PropTypes.func.isRequired,
		reference: PropTypes.string,
		btncolor: PropTypes.string,
		normalsize: PropTypes.bool,
		hide_for_disable: PropTypes.bool,
	},
	onClick: function() {
		if (!this.props.enabled) {
			return;
		}
		this.props.onClick(this.props.reference);
	},
	render: function() {
		if (this.props.hide_for_disable && !this.props.enabled) {
			return (<span />);
		}

		var btn_classes = classNames("btn", this.props.btncolor, {
			"btn-mini": !this.props.normalsize,
			disabled: !this.props.enabled,
		});
		return (<button className={btn_classes} onClick={this.onClick}>{this.props.children}</button>);
	},
});


async.ButtonSort = createReactClass({
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

async.LabelBoolean = createReactClass({
	propTypes: {
		label_true: 	PropTypes.string.isRequired,
		label_false: 	PropTypes.string.isRequired,
		value: 			PropTypes.bool.isRequired,
		inverse: 		PropTypes.bool,
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

async.JsonCode = createReactClass({
	propTypes: {
		i18nlabel:		PropTypes.string.isRequired,
		json: 			PropTypes.object.isRequired,
	},
	render: function() {
		var i18nlabel = (<span className="jsontitle"> {i18n(this.props.i18nlabel)} </span>);

		var json_string = JSON.stringify(this.props.json, null, " ");
		if (json_string == "{}") {
			json_string = (<span className="label label-inverse" style={{fontFamily: "\"Helvetica Neue\",Helvetica,Arial,sans-serif",}}>{i18n("empty")}</span>);
		}
		
		return (<div>
			<code className="json" style={{marginTop: 10}}>
				<i className="icon-indent-left"></i>
				{i18nlabel}
				{json_string}
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

async.JavaClassNameLink = createReactClass({
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
				<img src={mydmam.routes.reverse("github_favicon")} style={icon_style} />
				&nbsp;
				<abbr title={javaclass}>
					{javaclass.substring(javaclass.lastIndexOf(".") + 1)}
				</abbr>
			</a>
		</span>);
	},
});

async.JavaStackTrace = createReactClass({
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

async.SearchInputBox = createReactClass({
	getInitialState: function() {
		return {timer: null};
	},
	onTimerDone: function() {
		this.setState({timer: null});
		this.props.onKeyPress(ReactDOM.findDOMNode(this.refs.inputbox).value);
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

async.NavTabsLink = createReactClass({
	onClick: function(e) {
		e.preventDefault();
		this.props.onActiveChange(this.props.pos);
		$(ReactDOM.findDOMNode(this.refs.tab)).blur();
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

async.NavTabs = createReactClass({
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

async.PageHeaderTitle = createReactClass({
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
			var go_back = null;
			if (this.props.go_back_url) {
				go_back = (<a className="btn btn-mini"
					style={{marginBottom: "6px", marginRight: "1em"}}
					href={this.props.go_back_url}
					title={i18n('browser.goback')}>
					<i className="icon-chevron-left"></i>
				</a>);
			}
			p_lead = (<p className="lead">{go_back}{this.props.title}</p>);
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

async.HeaderTab = createReactClass({
	onClick: function(e) {
		//e.preventDefault();
		//this.props.onActiveChange(this.props.pos);
		$(ReactDOM.findDOMNode(this.refs.tab)).blur();
	},
	render: function(){
		var li_class = classNames({
			"active": this.props.href == location.hash,
			"pull-right": this.props.pullright,
		});

		var badge = null;
		var badge_count = this.props.badge_count;
		if (badge_count) {
			var span_class = classNames("badge", this.props.badge_class);
			badge = (<span className={span_class} style={{marginLeft: 5}}>
				{badge_count}
			</span>);
		}

		return (<li className={li_class}>
			<a href={this.props.href} onClick={this.onClick} ref="tab">
				{i18n(this.props.i18nlabel)}
				{badge}
			</a>
		</li>);
	},
});

var BtnCalendar = createReactClass({
	onClick: function(e) {
		e.preventDefault();
		this.props.onChange(this.props.current);
	},
	render: function() {
		return (<div className={this.props.classname} style={this.props.style} onClick={this.onClick}>
			{this.props.current}
		</div>);
	},
});

async.Calendar = createReactClass({
	getInitialState: function() {
		var date = new Date();
		if (this.props.date) {
			date = this.props.date;
		}
		date = new Date(date.getFullYear(), date.getMonth(), date.getDate(), 12, 0, 0);

		var now = new Date();
		now = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 12,0,0);

		return {date: date, now: now, };
	},
	getRelativeDate: function(months) {
		var date = this.state.date;
		return new Date(date.getFullYear(), date.getMonth() + months, 1, 12,0,0);
	},
	onClickNextMonth: function(e) {
		e.preventDefault();
		this.setNewDate(this.getRelativeDate(1));
		$(ReactDOM.findDOMNode(this.refs.btnnext)).blur();
	},
	onClickPreviousMonth: function(e) {
		e.preventDefault();
		this.setNewDate(this.getRelativeDate(-1));
		$(ReactDOM.findDOMNode(this.refs.btnprevious)).blur();
	},
	onClickMonth: function(e) {
		e.preventDefault();
	},
	onChangeDate: function(monthday) {
		var date = this.state.date;
		var newdate = new Date(date.getFullYear(), date.getMonth(), monthday, 12,0,0);
		this.setNewDate(newdate);
	},
	setNewDate(date) {
		if (date >= this.state.now) {
			this.setState({date: date});
		} else {
			this.setState({date: this.state.now});
		}
		this.props.onChange(date);
	},
	render: function() {
		var date = this.state.date;
		var now = this.state.now;

		var y = date.getFullYear();
		var m = date.getMonth();
		var last_day = new Date(y, m + 1, 0, 12,0,0);

		var month = [];
		for (var i = 1; i < last_day.getDate() + 1; i++) {
			thisdate = new Date(y, m, i, 12,0,0);
			month.push({
				monthday: i,
				weekday: thisdate.getDay(),
				week: thisdate.getWeekNumber(),
				ispast: thisdate < now,
			});
		}

		var weeks = [];
		var week = null;
		for(var pos in month) {
			pos = Number(pos);
			if (pos > 0) {
				if (month[pos].week != month[pos - 1].week) {
					week = null;
				}
			}

			if (week == null) {
				week = [];
				weeks.push(<div className="week" key={month[pos].week}>{week}</div>);
			}

			var day_classnames = classNames("day", {
				selectable: month[pos].ispast == false,
				noselectable: month[pos].ispast,
				selected: date.getDate() == month[pos].monthday,
			});

			var style = null;
			if (pos == 0) {
				if (month[pos].weekday > 0) {
					style = {marginLeft: (100 * (month[pos].weekday - 1) / 7) + "%"};
				} else {
					style = {marginLeft: (6 * 100 / 7) + "%"};
				}
			} else if (pos + 1 == month.length) {
				if (month[pos].weekday > 0) {
					style = {marginRight: (100 * (7 - month[pos].weekday) / 7)+ "%"};
				}
			}

			week.push(<BtnCalendar key={pos}
				current={month[pos].monthday}
				onChange={this.onChangeDate}
				style={style}
				classname={day_classnames} />);
		}

		var btn_previous_date = null;
		if ((new Date(date.getFullYear(), date.getMonth(), 0, 12,0,0)) >= now) {
			btn_previous_date = (<li><a href="#" onClick={this.onClickPreviousMonth} ref="btnprevious">{this.getRelativeDate(-1).getI18nOnlyMonth()}</a></li>);
		}

		return (<div style={{maxWidth: "23em"}}>
			<div className="pagination pagination-small pagination-centered" style={{marginTop: "2px", marginBottom: "0px"}}>
				<ul>
					{btn_previous_date}
					<li className="active"><a href="#" onClick={this.onClickMonth}>{this.state.date.getI18nFullDisplay()}</a></li>
					<li><a href="#" onClick={this.onClickNextMonth} ref="btnnext">{this.getRelativeDate(1).getI18nOnlyMonth()}</a></li>
				</ul>
			</div>
			<div className="month" style={{marginLeft: "auto", marginRight: "auto"}}>
				{weeks}
			</div>
		</div>);
	},
});

async.HourMinInputbox = createReactClass({
	getInitialState: function() {
		var hrs = 0;
		var min = 0;
		var valid = false;
		if (this.props.hrs) {
			hrs = this.props.hrs;
			valid = true;
		}
		if (this.props.min) {
			min = this.props.min;
			valid = true;
		}

		return {
			hrs: hrs,
			min: min,
			valid: valid,
		};
	},
	getFullTime: function() {
		return this.state.hrs.twoDigit(24) + ":" + this.state.min.twoDigit(60);
	},
	onChange: function(e) {
		var value = ReactDOM.findDOMNode(this.refs.inputbox).value.trim();
		if (value.length > "00:00".length) {
			ReactDOM.findDOMNode(this.refs.inputbox).value = this.getFullTime();
		} else if (value.indexOf(":") == 2) {
			var raw = value.split(":");
			var min = raw[1];
			if (min != null) {
				if (min.length == 2) {
					var hrs = raw[0].twoDigit(24);
					var min = min.twoDigit(60);
					if (value == hrs + ":" + min) {
						this.setState({hrs: hrs, min: min, valid: true});
						this.props.onChange(hrs, min);
						return;
					}
				}
			}
		}
		if (this.state.valid != false) {
			this.props.onChange(null, null);
			this.setState({valid: false});
		}
	},
	render: function() {
		var display = (<i className="icon-exclamation-sign"></i>);
		if (this.state.valid) {
			display = (<span><i className="icon-arrow-right"></i> {this.getFullTime()}</span>);
		}
		return (<div>
			<input
				type="time"
				placeholder="00:00"
				style={{width: "90px", marginRight: "1em"}}
				defaultValue={this.getFullTime()}
				onChange={this.onChange}
				ref="inputbox" /> {display}
		</div>);
	}
});
