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
search.SearchForm = React.createClass({
	componentDidMount: function() {
		React.findDOMNode(this.refs.q).focus();
		React.findDOMNode(this.refs.q).value = this.getDefaultText();
	},
	componentDidUpdate: function() {
		React.findDOMNode(this.refs.q).focus();
		React.findDOMNode(this.refs.q).value = this.getDefaultText();
	},
	handleSubmit: function(e) {
		e.preventDefault();
		var q = React.findDOMNode(this.refs.q).value.trim();
	    if (!q) {
			return;
		}
		this.props.onSearchFormSubmit({q: q});
	},
	getDefaultText: function() {
		var value = "";
		if (this.props.results) {
			value = this.props.results.q;
			if (!value) {
				value = "";
			}
		}
		return value;
	},
	render: function() {
	    return (
	    	<form className="search-query form-search" onSubmit={this.handleSubmit}>
				<div className="input-append">
					<input type="text" ref="q" placeholder={i18n("maingrid.search")} className="search-query span10" />
					<button className="btn btn-info" type="submit">{i18n("maingrid.search")}</button>
				</div>
			</form>
		);
	}
});