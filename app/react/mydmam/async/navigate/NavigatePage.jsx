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

navigate.NavigatePage = React.createClass({
	getInitialState: function() {
		return {
			stat: {},
			pathindex: "",
			default_page_size: 20,
			sort_order: [],
			externalpos: {},
		};
	},
	navigateTo: function(pathindex, page_from, page_size, sort) {
		var stat = mydmam.stat;
		var pathindex_key = md5(pathindex);
		if (sort == null) {
			sort = this.state.sort_order;
		}
		var search = "";
		if (this.state.inputboxsearch) {
			if (pathindex !== this.state.pathindex) {
				this.state.inputboxsearch.value = "";
			}
			search = this.state.inputboxsearch.value;
		}
		search = JSON.stringify(search);

		var request = {
			pathelementskeys: [pathindex_key],
			page_from: page_from,
			page_size: page_size,
			scopes_element: [stat.SCOPE_DIRLIST, stat.SCOPE_PATHINFO, stat.SCOPE_MTD_SUMMARY, stat.SCOPE_COUNT_ITEMS],
			scopes_subelements: [stat.SCOPE_MTD_SUMMARY, stat.SCOPE_COUNT_ITEMS],
			search: search,
			sort: sort,
		};

		document.body.style.cursor = 'wait';
		mydmam.async.request("stat", "cache", request, function(data) {
			document.body.style.cursor = 'default';
			if (data[pathindex_key]) {
				this.setState({
					stat: data,
					pathindex: pathindex,
					default_page_size: page_size,
					sort_order: sort,
				});

				var externalpos_request_keys = [];
				for (var response_pathindexkey in data) {
					var this_response = data[response_pathindexkey];
					if (!this_response.reference) {
						continue;
					}
					if (!this_response.reference) {
						continue;
					}
					if (mydmam.module.f.wantToHaveResolvedExternalPositions("pathindex", this_response.reference.directory, this_response.reference.storagename)) {
						externalpos_request_keys.push(response_pathindexkey);
					}
					if (!this_response.items) {
						continue;
					}
					for (var responseitem_pathindexkey in this_response.items) {
						var this_responseitem = this_response.items[responseitem_pathindexkey];
						if (!this_responseitem.reference) {
							continue;
						}
						if (mydmam.module.f.wantToHaveResolvedExternalPositions("pathindex", this_responseitem.reference.directory, this_responseitem.reference.storagename)) {
							externalpos_request_keys.push(responseitem_pathindexkey);
						}
					}
				}
				var response_resolve_external = function(external_resolve_data) {
					this.setState({externalpos: external_resolve_data});
				}.bind(this);
				mydmam.async.pathindex.resolveExternalPosition(externalpos_request_keys, response_resolve_external);
			} else {
				if (page_from > 0) {
					this.navigateTo(pathindex, 0, page_size);
				}
				return;
			}
			window.location.hash = this.state.pathindex;
		}.bind(this));
	},
	componentDidMount: function() {
		this.navigateTo(this.props.pathindex_destination, 0, this.state.default_page_size);
	},
	handlePaginationSwitchPage: function(newpage, alt_pressed) {
		if (alt_pressed){
			this.navigateTo(this.state.pathindex, 0, this.state.default_page_size * 2);
		} else {
			this.navigateTo(this.state.pathindex, newpage - 1, this.state.default_page_size);
		}
	},
	handleOnClickANavigateToNewDest: function(e) {
		e.preventDefault();
		var pathindex_target = $(e.currentTarget).data("navigatetarget");
		if (pathindex_target === '../') {
			/** Go to the parent directory */
			if (this.state.pathindex === "") {
				return;
			}
			if (this.state.pathindex.endsWith(":/")) {
				pathindex_target = "";
			} else {
				pathindex_target = this.state.pathindex.substring(0, this.state.pathindex.lastIndexOf("/"));
				if (pathindex_target.endsWith(":")) {
					pathindex_target += '/';
				}
			}
		}
		this.navigateTo(pathindex_target, 0, this.state.default_page_size);
	},
	handlechangeOrderSort: function(colname, order) {
		var stat_order = [];
		if (order != null) {
			/*	@see stat.SortDirListing.Col: sortedfilename, date, directory, size
				ASC / DESC {colname: "size", order: "ASC"}
			*/
			var order = order.toUpperCase();
			if (colname === 'size') {
				// directories has not sizes. We put them to the bottom.
				stat_order = [
					{colname: "directory", order: "ASC"},
					{colname: "size", order: order},
				];
			} else if (colname === 'date') {
				stat_order = [
					{colname: "date", order: order},
				];
			} else {
				// path, with natural sort (no separate dirs and files).
				stat_order = [
					{colname: "sortedfilename", order: order},
				];
			}
		}
		this.navigateTo(this.state.pathindex, 0, this.state.default_page_size, stat_order);
	},
	handleChangeSearchBox: function(dom_inputbox) {
		if (!this.state.inputboxsearch) {
			this.setState({
				inputboxsearch: dom_inputbox,
			});
		}
		var stat = this.state.stat[md5(this.state.pathindex)];
		if (!stat) {
			return;
		}
		if (stat.reference.directory) {
			this.navigateTo(this.state.pathindex, 0, this.state.default_page_size, null);
		}
	},
	render: function() {
		var stat = this.state.stat[md5(this.state.pathindex)];
		if (!stat) {
			return (<div></div>);
		}

		/**
		 * Fake (get from the first item), but realist indexdate. It's a backup solution.
		 */
		var first_item_dateindex = 0;
		if (stat.items) {
			for (var item in stat.items) {
				var newitem = stat.items[item];
				if (newitem.reference) {
					if (newitem.reference.dateindex) {
						first_item_dateindex = newitem.reference.dateindex;
						break;
					}
				}
			}
		}
		var storagename = null;
		if (stat.reference) {
			storagename = stat.reference.storagename;
		}
		var path = null;
		if (stat.reference) {
			path = stat.reference.path;
		}
		
		var display_pagination = null;
		if (stat.items_total & !stat.search_return_nothing) {
			display_pagination = (
				<mydmam.async.pagination.reactBlock
					pagecount={Math.ceil(stat.items_total / stat.items_page_size)}
					currentpage={stat.items_page_from + 1}
					onClickButton={this.handlePaginationSwitchPage} />
			);
		}

		var noresult = null;
		if (stat.search_return_nothing) {
			noresult = (<navigate.NoResultsSearch />);
		}

		var is_in_search = false;
		if (this.state.inputboxsearch) {
			is_in_search = this.state.inputboxsearch.value != "";
		}

		return (
			<div>
				<navigate.BreadCrumb
					storagename={storagename}
					path={path}
					navigate={this.handleOnClickANavigateToNewDest} />
				<navigate.HeaderItem
					reference={stat.reference}
					first_item_dateindex={first_item_dateindex}
					pathindexkey={md5(this.state.pathindex)}
					navigate={this.handleOnClickANavigateToNewDest}
					is_in_search={is_in_search}
					externalpos={this.state.externalpos} />
				<mydmam.async.pathindex.reactMetadataFull
					reference={stat.reference}
					mtdsummary={stat.mtdsummary}
					navigate={this.handleOnClickANavigateToNewDest} />
				<navigate.NavigateTable
					stat={stat}
					navigate={this.handleOnClickANavigateToNewDest}
					changeOrderSort={this.handlechangeOrderSort}
					externalpos={this.state.externalpos} />
				{display_pagination}
				{noresult}
				<navigate.SearchBox
					changeStateInputbox={this.handleChangeSearchBox} />
			</div>
		);
	}
});