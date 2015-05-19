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

search.SearchResultPage = React.createClass({
	handleSearchFormSubmit: function(search_request) {
		if (!search_request.q) {
			return;
		}
		/*actual_results.q = search_request.q;*/
		window.location = search.makeURLsearch(search_request.q, 1);
	},
	handlePaginationLinkTargeter: function(button_num) {
		return search.makeURLsearch(this.state.results.q, button_num);
	},
	getInitialState: function() {
		return {results: this.props.results, stat: {}, externalpos: {}};
	},
	componentDidMount: function() {
		var results = this.state.results.results;
		var stat_request_keys = [];
		for (var i = 0; i < results.length; i++) {
			if (results[i].index === "pathindex") {
				stat_request_keys.push(results[i].key);
			}
		}
		if (stat_request_keys.length > 0) {
			var stat_request = {
				pathelementskeys: stat_request_keys,
				scopes_element: [mydmam.stat.SCOPE_MTD_SUMMARY],
				scopes_subelements: [],
				page_from: 0,
				page_size: 500,
				search: JSON.stringify(''),
				sort: [],
			};

			mydmam.async.request("stat", "cache", stat_request, function(data) {
				this.setState({stat: data});
			}.bind(this));
		}

		var externalpos_request_keys = [];
		for (var i = 0; i < results.length; i++) {
			if (mydmam.module.f.wantToHaveResolvedExternalPositions(results[i].index, results[i].content.directory, results[i].content.storagename)) {
				externalpos_request_keys.push(results[i].key);
			}
		}
		var response_resolve_external = function(data) {
			this.setState({externalpos: data});
		}.bind(this);

		mydmam.async.pathindex.resolveExternalPosition(externalpos_request_keys, response_resolve_external);
	},
	render: function() {
	    return (
	    	<div>
				<search.SearchForm results={this.state.results} onSearchFormSubmit={this.handleSearchFormSubmit} onSearchFormChange={this.handleSearchFormChange} />
				<search.SearchResultsHeader results={this.state.results} />
				<search.SearchResults results={this.state.results} stat={this.state.stat} externalpos={this.state.externalpos} />
				<mydmam.async.pagination.reactBlock
					pagecount={this.state.results.pagecount}
					currentpage={this.state.results.from}
					onlinkTargeter={this.handlePaginationLinkTargeter} />
	    	</div>
		);
	}
});