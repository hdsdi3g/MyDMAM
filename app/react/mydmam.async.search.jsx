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
	mydmam.async.search = function(results, dom_target) {
		if (!dom_target) {
			return;
		}

		/**
		 * Create React uniq key.
		 */
		var result_list = results.results;
		for (var pos in result_list) {
			result_list[pos].reactkey = result_list[pos].index + ":" + result_list[pos].type + ":" + result_list[pos].key;
		};
		console.log(results);

		var SearchForm = React.createClass({
			handleChange: function(event) {
				var q = React.findDOMNode(this.refs.q).value;
				this.props.onSearchFormChange(q);
			},
			handleSubmit: function(e) {
				e.preventDefault();
				var q = React.findDOMNode(this.refs.q).value.trim();
			    if (!q) {
					return;
				}
				this.props.onSearchFormSubmit({q: q});
				React.findDOMNode(this.refs.q).value = '';
			    return;
			},
			render: function() {
			    return (
			    	<form className="search-query form-search" onSubmit={this.handleSubmit}>
						<div className="input-append">
							<input type="text" ref="q" value={this.props.results.q} placeholder={i18n("maingrid.search")} className="search-query span10" onChange={this.handleChange} />
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
							pageadd = (<span>{i18n("search.pageXonY", this.props.results.from, this.props.results.pagesize)}</span>);
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

		var Result = React.createClass({
			render: function() {
				var view_hander = mydmam.module.f.processViewSearchResult(this.props.result);
				if (!view_hander) {
					console.error("Can't handle search result", this.props.result);
					view_hander = (<div>Error</div>);
				}
			    return view_hander;
			}
		});

		var SearchResults = React.createClass({
			render: function() {
				var resultList = this.props.results.results.map(function (result) {
					return (
						<div style={{marginBottom: "1em"}} key={result.reactkey}>
							<Result result={result} />
						</div>
					);
				});
			    return (
			    	<div>
				        {resultList}
					</div>
				);
			}
		});

		var SearchPagination = React.createClass({
			render: function() {
			    return (
			    	<p>Pages !
						{/*<div class="pagination pagination-centered pagination-large">
								<ul>
									<li><a href="/oldsearch?q=+a&amp;from=1"><span>«</span></a></li>
				
																						<li><a href="/oldsearch?q=+a&amp;from=1">1</a></li>
																								<li class="active"><span>2</span></li>
																								<li><a href="/oldsearch?q=+a&amp;from=3">3</a></li>
																								<li><a href="/oldsearch?q=+a&amp;from=4">4</a></li>
																<li class="active"><span>&nbsp;</span></li>
																		<li><a href="/oldsearch?q=+a&amp;from=8">8</a></li>
																								<li><a href="/oldsearch?q=+a&amp;from=9">9</a></li>
																								<li><a href="/oldsearch?q=+a&amp;from=10">10</a></li>
																								<li><a href="/oldsearch?q=+a&amp;from=11">11</a></li>
									<li><a href="/oldsearch?q=+a&amp;from=3"><span>»</span></a></li>
							</ul>
						</div>*/}
			    	</p>
				);
			}
		});

		var SearchResultPage = React.createClass({
			handleSearchFormChange: function(q) {
				var results = this.state.results;
				results.q = q;
				this.setState({results: results});
			},
			handleSearchFormSubmit: function(search_request) {
				if (!search_request.q) {
					return;
				}
				var actual_results = this.state.results;
				actual_results.q = search_request.q;
				this.setState({results: actual_results});
				//TODO create server side, and update this:
				/*mydmam.async.request("demosasync", "add", comment, function(data) {
					this.setState({data: data.commentlist});
				}.bind(this));*/
			},
			getInitialState: function() {
				return {results: results, };
			},
			render: function() {
			    return (
			    	<div>
						<SearchForm results={this.state.results} onSearchFormSubmit={this.handleSearchFormSubmit} onSearchFormChange={this.handleSearchFormChange} />
						<SearchResultsHeader results={this.state.results} />
						<SearchResults results={this.state.results} />
						<SearchPagination results={this.state.results} />
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
