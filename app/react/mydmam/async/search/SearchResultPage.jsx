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

//TODO Search box in menus
//TODO Correct links for search results
//TODO external location

search.SearchResultPage = React.createClass({
	getInitialState: function() {
		return {stat: {}, externalpos: {}};
	},
	handleSearchFormSubmit: function(search_request) {
		if (!search_request.q) {
			return;
		}
		window.location.hash = search.urlify(search_request.q, 1);
	},
	handlePaginationLinkTargeter: function(button_num) {
		return "#" + search.urlify(this.props.results.q, button_num);
	},
	doSubSearch: function(nextProps) {
		var results = this.props.results.results;
		if (nextProps) {
			results = nextProps.results.results;
		}

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

		/*var externalpos_request_keys = [];
		for (var i = 0; i < results.length; i++) {
			if (mydmam.module.f.wantToHaveResolvedExternalPositions(results[i].index, results[i].content.directory, results[i].content.storagename)) {
				externalpos_request_keys.push(results[i].key);
			}
		}
		var response_resolve_external = function(data) {
			this.setState({externalpos: data});
		}.bind(this);
		
		mydmam.async.pathindex.resolveExternalPosition(externalpos_request_keys, response_resolve_external);*/
	},
	componentDidMount: function() {
		this.doSubSearch();
	},
	componentWillReceiveProps: function(nextProps) {
		this.doSubSearch(nextProps);
	},
	render: function() {
	    return (
	    	<div>
				<search.SearchForm results={this.props.results} onSearchFormSubmit={this.handleSearchFormSubmit} onSearchFormChange={this.handleSearchFormChange} />
				<search.SearchResultsHeader results={this.props.results} />
				<search.SearchResults results={this.props.results} stat={this.state.stat} externalpos={{}} />
				<mydmam.async.pagination.reactBlock
					pagecount={this.props.results.pagecount}
					currentpage={this.props.results.from}
					onlinkTargeter={this.handlePaginationLinkTargeter} />
	    	</div>
		);
	}
});

search.Home = React.createClass({
	getInitialState: function() {
		return {
			q: null,
			qfrom: 1,
			results: null,
		};
	},
	doSearch: function(nextProps) {
		var props = this.props;
		if (nextProps) {
			props = nextProps;
		}

		var qfrom = 1;
		if (props.params.from) {
			qfrom = props.params.from;
		}
		this.setState({
			q: decodeURIComponent(props.params.q),
			qfrom: qfrom,
			results: null,
		});

		var request = {
			q: decodeURIComponent(props.params.q),
			from: qfrom,
		};

		var createReactKey = function(result_list) {
			for (var pos in result_list) {
				result_list[pos].reactkey = result_list[pos].index + ":" + result_list[pos].type + ":" + result_list[pos].key;
			};
		}

		mydmam.async.request("search", "query", request, function(data) {
			createReactKey(data.results);

			this.setState({
				q: data.q,
				qfrom: data.from,
				results: data,
			});
			this.setDocumentTitle(data.q);
		}.bind(this));
	},
	setDocumentTitle: function(name) {
		var	new_title = i18n("search.pagetitle") + " :: " + i18n("site.name");
		if (name) {
			new_title = name + " :: " + new_title;
		}

		if (document.title != new_title) {
			document.title = new_title
		}
	},
	componentDidMount: function() {
		this.doSearch();
	},
	componentWillReceiveProps: function(nextProps) {
		this.doSearch(nextProps);
	},
	componentWillUnmount: function() {
		document.title = i18n("site.name");
	},
	render: function() {
		var results = this.state.results;

		if (!results) {
			results = {
				q: this.state.q,
				from: this.state.from,
				duration: 0,
				total_items_count: 0,
				max_score: 0,
				pagesize: 1,
				pagecount: 1,
				mode: "BY_FULL_TEXT",
				results: [],
			}
		}

		return (<div className={"container"}>
			<p className={"lead"}>{i18n("search.pagetitle")}</p>
			<search.SearchResultPage results={results} />
		</div>);
	}
});

search.urlify = function(q, qfrom) {
	if (qfrom > 1) {
		return "search/" + encodeURIComponent(q) + "?from=" + qfrom;
	} else {
		return "search/" + encodeURIComponent(q);
	}
};

mydmam.routes.push("search-simple", "search/:q", search.Home, [{name: "search", verb: "query"}]);	
