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

async.PageHeaderTitle = React.createClass({
	componentDidMount: function() {
		if (this.props.title) {
			document.title = this.props.title + " :: " + i18n("site.name");
		} else {
			document.title = i18n("site.name");
		}
	},
	componentWillUnmount: function() {
		document.title = i18n("site.name");
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

		return (<div className={main_class_name}>
			{p_lead}
			<div>{this.props.children}</div>
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
		var class_name_icon = classNames({
			"icon-white":        this.props.enabled,
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