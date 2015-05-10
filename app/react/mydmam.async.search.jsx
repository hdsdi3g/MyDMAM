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
(function(mydmam) {

	mydmam.async.makeURLsearch = function (q, from) {
		return mydmam.async.baseURLsearch.replace("param1query", encodeURIComponent(q)).replace("param2from", from);
	};

	mydmam.async.search = function(results, dom_target) {
		if (!dom_target) {
			return;
		}

		/**
		 * Create React uniq key.
		 */
		var createReactKey = function(result_list) {
			for (var pos in result_list) {
				result_list[pos].reactkey = result_list[pos].index + ":" + result_list[pos].type + ":" + result_list[pos].key;
			};
			console.log(results);
		}

		createReactKey(results.results);

		var SearchForm = React.createClass({
			getInitialState: function() {
				return {new_q: ""};
			},
			componentDidMount: function() {
				this.setState({new_q: this.props.results.q});
			},
			handleChange: function(event) {
				this.setState({new_q: React.findDOMNode(this.refs.q).value});
			},
			handleSubmit: function(e) {
				e.preventDefault();
				var q = React.findDOMNode(this.refs.q).value.trim();
			    if (!q) {
					return;
				}
				this.props.onSearchFormSubmit({q: q});
			},
			render: function() {
			    return (
			    	<form className="search-query form-search" onSubmit={this.handleSubmit}>
						<div className="input-append">
							<input type="text" ref="q" value={this.state.new_q} placeholder={i18n("maingrid.search")} className="search-query span10" onChange={this.handleChange} />
							<button className="btn btn-info" type="submit">{i18n("maingrid.search")}</button>
						</div>
					</form>
				);
			}
		});

		var SearchResultsHeader = React.createClass({
			render: function() {
				if (this.props.results.results.length == 0) {
					return (
						<div className="alert alert-info">
							<h4>{i18n("search.noresults")}</h4>
							{i18n("search.noresultsfor")} <strong>{this.props.results.q}</strong><br />
							<small>({this.props.results.duration / 1000} {i18n("search.seconds")})</small>.
						</div>
					);
				} else {
					var pageon = (<span>{i18n("search.oneresult")}</span>);
					var pageadd = null;
					if (this.props.results.total_items_count > 1) {
						pageon = (<span>{this.props.results.total_items_count} {i18n("search.results")}</span>);
						if (this.props.results.pagecount > 1) {
							pageadd = (<span>{i18n("search.pageXonY", this.props.results.from, this.props.results.pagecount)}</span>);
						}
					}

					return (
						<p><small className="muted">
							{pageon} {pageadd} ({this.props.results.duration / 1000} {i18n("search.seconds")})
						</small><br />
						<small>
							{i18n("search.method." + this.props.results.mode.toLowerCase())}
						</small>
						</p>
					);
				}
			}
		});

		var SearchResults = React.createClass({
			render: function() {
				var stat = this.props.stat;
				var externalpos = this.props.externalpos;

				var resultList = this.props.results.results.map(function (result) {
					var ViewHander = mydmam.module.f.processViewSearchResult(result);
					if (!ViewHander) {
						console.error("Can't handle search result", result, stat[result.key]);
						return (
							<div style={{marginBottom: "1em"}} key={result.reactkey}>
								<div>Error</div>
							</div>
						);
					} else {
						return (
							<div style={{marginBottom: "1em"}} key={result.reactkey}>
								<ViewHander result={result} stat={stat[result.key]} externalpos={externalpos} />
							</div>
						);
					}
				});
			    return (
			    	<div>
				        {resultList}
					</div>
				);
			}
		});

		var SearchPagination = mydmam.async.pagination.reactBlock;

		var SearchResultPage = React.createClass({
			handleSearchFormSubmit: function(search_request) {
				if (!search_request.q) {
					return;
				}
				/*actual_results.q = search_request.q;*/
				window.location = mydmam.async.makeURLsearch(search_request.q, 1);
			},
			handlePaginationLinkTargeter: function(button_num) {
				return mydmam.async.makeURLsearch(this.state.results.q, button_num);
			},
			handlePaginationSwitchPage: function(new_page_pos) {
			},
			getInitialState: function() {
				return {results: results, stat: {}, externalpos: {}};
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
				var external_key;
				for (var i = 0; i < results.length; i++) {
					external_key = mydmam.module.f.wantToHaveResolvedExternalPositions(results[i]);
					if (external_key) {
						externalpos_request_keys.push(external_key);
					}
				}
				if (externalpos_request_keys.length > 0) {
					$.ajax({
						url: mydmam.metadatas.url.resolvepositions,
						type: "POST",
						data: {
							"keys": externalpos_request_keys,
						},
						success: function(data) {
							this.setState({externalpos: data});
						}.bind(this),
						error: function(jqXHR, textStatus, errorThrown) {
							console.error(jqXHR, textStatus, errorThrown);
						},
					});
				}
			},
			render: function() {
			    return (
			    	<div>
						<SearchForm results={this.state.results} onSearchFormSubmit={this.handleSearchFormSubmit} onSearchFormChange={this.handleSearchFormChange} />
						<SearchResultsHeader results={this.state.results} />
						<SearchResults results={this.state.results} stat={this.state.stat} externalpos={this.state.externalpos} />
						<SearchPagination
							pagecount={this.state.results.pagecount}
							currentpage={this.state.results.from}
							onlinkTargeter={this.handlePaginationLinkTargeter}
							onClickButton={this.handlePaginationSwitchPage} />
			    	</div>
				);
			}
		});

		React.render(
			<SearchResultPage />,
			dom_target
		);
		
	};
})(window.mydmam);
