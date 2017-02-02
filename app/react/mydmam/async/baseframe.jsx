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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
 */

/**
 * Plug an external React input box, and callback if user press enter.
 * Draw nothing.
 * @see https://facebook.github.io/react/tips/use-react-with-other-libraries.html
 * @see https://facebook.github.io/react/tips/dom-event-listeners.html
 */
async.SearchBox = React.createClass({
	getInitialState: function() {
		return {
			inputbox: $("#sitesearch")[0],
		};
	},
	componentDidMount: function() {
		this.state.inputbox.value = "";
		this.state.inputbox.addEventListener('keypress', this.onkeyPress);
		this.state.inputbox.style.display = "block";
	},
	componentWillUnmount: function() {
		this.state.inputbox.removeEventListener('keypress', this.onkeyPress);
		this.state.inputbox.style.display = "none";
	},
	shouldComponentUpdate: function(nextProps, nextState) {
	    return false;
	},
	onkeyPress: function(event) {
		if (!event) {
			event = window.event;
		}
    	var keyCode = event.keyCode || event.which;
    	if (keyCode == '13') {
      		this.props.onValidation(this.state.inputbox.value);
      		this.state.inputbox.value = "";

			event.preventDefault();
			event.stopImmediatePropagation();
			event.stopPropagation();
    	}
	},
	render: function() {
		return null;
	}
});

async.Home = React.createClass({
	render: function() {
		return (<div className="container">
			<div className="hero-unit">
				<h1>{i18n("site.name")}</h1>
				<p>{i18n("site.baseline")}</p>
			</div>
		</div>);
	}
});

async.Footer = React.createClass({
	render: function() {
		return (<div className="container-fluid" style={{textAlign: "center", marginTop: "1.5em"}}>
			<small className="muted">
				<a href="http://mydmam.org" style={{color: "#999999"}}>MyDMAM</a> {i18n("site.aboutfooter")}
			</small>
		</div>);
	}
});

