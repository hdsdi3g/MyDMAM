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
 * Plug an external React input box, and callback if user input text after some time.
 * Draw nothing.
 * @see https://facebook.github.io/react/tips/use-react-with-other-libraries.html
 * @see https://facebook.github.io/react/tips/dom-event-listeners.html
 */
navigate.SearchBox = React.createClass({
	getInitialState: function() {
		return {
			inputbox: $("#sitesearch")[0],
			timeoutid: null,
		};
	},
	componentDidMount: function() {
		this.state.inputbox.value = "";
		this.state.inputbox.addEventListener('keypress', this.onTextChange);
	},
	componentWillUnmount: function() {
		this.state.inputbox.removeEventListener('keypress', this.onTextChange);
		window.clearTimeout(this.state.timeoutid);
	},
	shouldComponentUpdate: function(nextProps, nextState) {
		/*if (nextProps.reset) {
			this.componentWillUnmount();
			this.componentDidMount();
		}*/
	    return false;
	},
	onTextChange: function() {
		window.clearTimeout(this.state.timeoutid);
		this.setState({
			timeoutid: window.setTimeout(this.onEndTimeout, 400),
		});
	},
	onEndTimeout: function() {
		this.props.changeStateInputbox(this.state.inputbox);
	},
	render: function() {
		return null;
	}
});