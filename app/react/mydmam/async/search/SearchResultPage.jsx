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

search.SearchResultPage = createReactClass({
	getInitialState: function() {
		return {stat: {}, external_storages_location: []};
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
	componentWillMount: function() {
		mydmam.async.pathindex.populateExternalLocationStorageList(function(storages) {
			this.setState({external_storages_location: storages});
		}.bind(this));
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
				scopes_element: [mydmam.async.navigate.SCOPE_MTD_SUMMARY],
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
				<search.SearchResults results={this.props.results} stat={this.state.stat} external_storages_location={this.state.external_storages_location} />
				<mydmam.async.pagination.reactBlock
					pagecount={this.props.results.pagecount}
					currentpage={this.props.results.from}
					onlinkTargeter={this.handlePaginationLinkTargeter} />
	    	</div>
		);
	}
});

search.Home = createReactClass({
	getInitialState: function() {
		return {
			q: this.props.q,
			qfrom: 1,
			results: null,
			inputboxsearch: null,
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

		var q = props.q;
		if (q == null) {
			q = decodeURIComponent(props.params.q);
		}

		this.setState({
			q: q,
			qfrom: qfrom,
			results: null,
		});

		var request = {
			q: q,
			from: qfrom,
		};

		mydmam.async.request("search", "query", request, function(data) {
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

		var content = null;
		if (results) {
			content = (<search.SearchResultPage results={results} />);
		} else {
			var q = this.state.q;
			if (q != null) {
				content = (<div>
					<form className="search-query form-search">
						<input value={q} readOnly={true} className="search-query span11" type="text" />
					</form>
					<mydmam.async.PageLoadingProgressBar />
				</div>);
			}
		}

		return (<div className={"container"}>
			<p className={"lead"}>{i18n("search.pagetitle")}</p>
			{content}
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
