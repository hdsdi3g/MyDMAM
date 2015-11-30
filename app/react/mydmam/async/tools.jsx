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

