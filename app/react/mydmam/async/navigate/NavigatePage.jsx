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
 * Copyright (C) hdsdi3g for hd3g.tv 2015-2016
 * 
*/

navigate.Home = createReactClass({
	componentDidMount: function() {
		this.setDocumentTitle();
	},
	componentDidUpdate: function(dd) {
		this.setDocumentTitle();
	},
	setDocumentTitle: function(name) {
		var	new_title = i18n("application.navigate") + " :: " + i18n("site.name");
		if (name) {
			new_title = name + " :: " + new_title;
		}

		if (document.title != new_title) {
			document.title = new_title
		}
	},
	componentWillUnmount: function() {
		document.title = i18n("site.name");
	},
	render: function() {
		var target = null;

		if (this.props.params.storage) {
			var path = this.props.params.storage;
			if (path.endsWith(":") == false) {
				path = path + ":";
			}

			for (var p = 1; p < 21; p++) {
				if (this.props.params["p" + p]) {
					path = path + "/" + this.props.params["p" + p];
				}
			};

			if (path.endsWith(":")) {
				path = path + "/";
			}

			target = <navigate.NavigatePage pathindex_destination={path} setDocumentTitle={this.setDocumentTitle} q={this.props.q} />
		} else {
			target = <navigate.NavigatePage pathindex_destination="" setDocumentTitle={this.setDocumentTitle} q={this.props.q} />
		}
		return (
			<div className="container-fluid">{target}</div>
		);
	},
});

/**
 * Soo ugly...
 */
mydmam.routes.push("navigate-root", "navigate", navigate.Home, [{name: "stat", verb: "cache"}]);	
mydmam.routes.push("navigate-subdir0", "navigate/:storage", navigate.Home, [{name: "stat", verb: "cache"}]);
mydmam.routes.push("navigate-subdir1", "navigate/:storage/:p1", navigate.Home, [{name: "stat", verb: "cache"}]);
mydmam.routes.push("navigate-subdir2", "navigate/:storage/:p1/:p2", navigate.Home, [{name: "stat", verb: "cache"}]);
mydmam.routes.push("navigate-subdir3", "navigate/:storage/:p1/:p2/:p3", navigate.Home, [{name: "stat", verb: "cache"}]);
mydmam.routes.push("navigate-subdir4", "navigate/:storage/:p1/:p2/:p3/:p4", navigate.Home, [{name: "stat", verb: "cache"}]);
mydmam.routes.push("navigate-subdir5", "navigate/:storage/:p1/:p2/:p3/:p4/:p5", navigate.Home, [{name: "stat", verb: "cache"}]);
mydmam.routes.push("navigate-subdir6", "navigate/:storage/:p1/:p2/:p3/:p4/:p5/:p6", navigate.Home, [{name: "stat", verb: "cache"}]);
mydmam.routes.push("navigate-subdir7", "navigate/:storage/:p1/:p2/:p3/:p4/:p5/:p6/:p7", navigate.Home, [{name: "stat", verb: "cache"}]);
mydmam.routes.push("navigate-subdir8", "navigate/:storage/:p1/:p2/:p3/:p4/:p5/:p6/:p7/:p8", navigate.Home, [{name: "stat", verb: "cache"}]);
mydmam.routes.push("navigate-subdir9", "navigate/:storage/:p1/:p2/:p3/:p4/:p5/:p6/:p7/:p8/:p9", navigate.Home, [{name: "stat", verb: "cache"}]);
mydmam.routes.push("navigate-subdir10", "navigate/:storage/:p1/:p2/:p3/:p4/:p5/:p6/:p7/:p8/:p9/:p10", navigate.Home, [{name: "stat", verb: "cache"}]);
mydmam.routes.push("navigate-subdir11", "navigate/:storage/:p1/:p2/:p3/:p4/:p5/:p6/:p7/:p8/:p9/:p10/:p11", navigate.Home, [{name: "stat", verb: "cache"}]);
mydmam.routes.push("navigate-subdir12", "navigate/:storage/:p1/:p2/:p3/:p4/:p5/:p6/:p7/:p8/:p9/:p10/:p11/:p12", navigate.Home, [{name: "stat", verb: "cache"}]);
mydmam.routes.push("navigate-subdir13", "navigate/:storage/:p1/:p2/:p3/:p4/:p5/:p6/:p7/:p8/:p9/:p10/:p11/:p12/:p13", navigate.Home, [{name: "stat", verb: "cache"}]);
mydmam.routes.push("navigate-subdir14", "navigate/:storage/:p1/:p2/:p3/:p4/:p5/:p6/:p7/:p8/:p9/:p10/:p11/:p12/:p13/:p14/", navigate.Home, [{name: "stat", verb: "cache"}]);
mydmam.routes.push("navigate-subdir15", "navigate/:storage/:p1/:p2/:p3/:p4/:p5/:p6/:p7/:p8/:p9/:p10/:p11/:p12/:p13/:p14/:p15", navigate.Home, [{name: "stat", verb: "cache"}]);
mydmam.routes.push("navigate-subdir16", "navigate/:storage/:p1/:p2/:p3/:p4/:p5/:p6/:p7/:p8/:p9/:p10/:p11/:p12/:p13/:p14/:p15/:p16", navigate.Home, [{name: "stat", verb: "cache"}]);
mydmam.routes.push("navigate-subdir17", "navigate/:storage/:p1/:p2/:p3/:p4/:p5/:p6/:p7/:p8/:p9/:p10/:p11/:p12/:p13/:p14/:p15/:p16/:p17", navigate.Home, [{name: "stat", verb: "cache"}]);
mydmam.routes.push("navigate-subdir18", "navigate/:storage/:p1/:p2/:p3/:p4/:p5/:p6/:p7/:p8/:p9/:p10/:p11/:p12/:p13/:p14/:p15/:p16/:p17/:p18", navigate.Home, [{name: "stat", verb: "cache"}]);
mydmam.routes.push("navigate-subdir19", "navigate/:storage/:p1/:p2/:p3/:p4/:p5/:p6/:p7/:p8/:p9/:p10/:p11/:p12/:p13/:p14/:p15/:p16/:p17/:p18/:p19", navigate.Home, [{name: "stat", verb: "cache"}]);
mydmam.routes.push("navigate-subdir20", "navigate/:storage/:p1/:p2/:p3/:p4/:p5/:p6/:p7/:p8/:p9/:p10/:p11/:p12/:p13/:p14/:p15/:p16/:p17/:p18/:p19/:p20", navigate.Home, [{name: "stat", verb: "cache"}]);

mydmam.routes.setNeedsToRedirectSearch("navigate-root");
mydmam.routes.setNeedsToRedirectSearch("navigate-subdir0");
mydmam.routes.setNeedsToRedirectSearch("navigate-subdir1");
mydmam.routes.setNeedsToRedirectSearch("navigate-subdir2");
mydmam.routes.setNeedsToRedirectSearch("navigate-subdir3");
mydmam.routes.setNeedsToRedirectSearch("navigate-subdir4");
mydmam.routes.setNeedsToRedirectSearch("navigate-subdir5");
mydmam.routes.setNeedsToRedirectSearch("navigate-subdir6");
mydmam.routes.setNeedsToRedirectSearch("navigate-subdir7");
mydmam.routes.setNeedsToRedirectSearch("navigate-subdir8");
mydmam.routes.setNeedsToRedirectSearch("navigate-subdir9");
mydmam.routes.setNeedsToRedirectSearch("navigate-subdir10");
mydmam.routes.setNeedsToRedirectSearch("navigate-subdir11");
mydmam.routes.setNeedsToRedirectSearch("navigate-subdir12");
mydmam.routes.setNeedsToRedirectSearch("navigate-subdir13");
mydmam.routes.setNeedsToRedirectSearch("navigate-subdir14");
mydmam.routes.setNeedsToRedirectSearch("navigate-subdir15");
mydmam.routes.setNeedsToRedirectSearch("navigate-subdir16");
mydmam.routes.setNeedsToRedirectSearch("navigate-subdir17");
mydmam.routes.setNeedsToRedirectSearch("navigate-subdir18");
mydmam.routes.setNeedsToRedirectSearch("navigate-subdir19");
mydmam.routes.setNeedsToRedirectSearch("navigate-subdir20");

navigate.NavigatePage = createReactClass({
	getInitialState: function() {
		return {
			stat: {},
			pathindex: "",
			default_page_size: 20,
			sort_order: [],
			external_location: null,
		};
	},
	navigateTo: function(pathindex, page_from, page_size, q, sort) {
		var stat = mydmam.stat;
		var pathindex_key = md5(pathindex);
		if (sort == null) {
			sort = this.state.sort_order;
		}

		var search = "";
		if (q != null) {
			search = q;
		}

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
				
				if (data[pathindex_key].reference.storagename) {
					mydmam.async.pathindex.resolveExternalLocation(data[pathindex_key].reference.storagename, data[pathindex_key].reference.path, function(resolve_result){
						this.setState({
							external_location: resolve_result,
						});
					}.bind(this));
				}
			} else {
				if (page_from > 0) {
					this.navigateTo(pathindex, 0, page_size);
				}
				return;
			}

			var dirname = this.state.pathindex;
			dirname = dirname.substring(dirname.lastIndexOf("/") + 1, dirname.length);
			if (dirname == "") {
				dirname = this.state.pathindex.substring(0, this.state.pathindex.length - 2);
			}
			this.props.setDocumentTitle(dirname);

		}.bind(this));
	},
	componentDidMount: function() {
		this.navigateTo(this.props.pathindex_destination, 0, this.state.default_page_size, this.props.q);
	},
	componentWillUpdate: function(nextProps, nextState) {
		var need_to_update = false;

		need_to_update = (this.props.pathindex_destination != nextProps.pathindex_destination);
		if (need_to_update == false) {
			need_to_update = (this.props.q != nextProps.q);
		}

		if (need_to_update) {
			this.navigateTo(nextProps.pathindex_destination, 0, this.state.default_page_size, nextProps.q);
		}
	},
	handlePaginationSwitchPage: function(newpage, alt_pressed) {
		if (alt_pressed){
			this.navigateTo(this.state.pathindex, 0, this.state.default_page_size * 2, this.props.q);
		} else {
			this.navigateTo(this.state.pathindex, newpage - 1, this.state.default_page_size, this.props.q);
		}
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
		this.navigateTo(this.state.pathindex, 0, this.state.default_page_size, this.props.q, stat_order);
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
		var path = null;
		if (stat.reference) {
			storagename = stat.reference.storagename;
			path = stat.reference.path;
		}
		
		var noresult = null;
		var display_pagination = null;
		if (stat.search_return_nothing) {
			noresult = (<navigate.NoResultsSearch />);
		} else if (stat.items_total) {
			display_pagination = (
				<mydmam.async.pagination.reactBlock
					pagecount={Math.ceil(stat.items_total / stat.items_page_size)}
					currentpage={stat.items_page_from + 1}
					onClickButton={this.handlePaginationSwitchPage} />
			);
		}

		return (
			<div>
				<navigate.BreadCrumb
					storagename={storagename}
					path={path} />
				<navigate.HeaderItem
					stat={stat}
					first_item_dateindex={first_item_dateindex}
					pathindexkey={md5(this.state.pathindex)}
					in_search={this.props.q}
					external_location={this.state.external_location} />
				<mydmam.async.pathindex.reactMetadataFull
					reference={stat.reference}
					mtdsummary={stat.mtdsummary} />
				<navigate.NavigateTable
					stat={stat}
					changeOrderSort={this.handlechangeOrderSort}
					external_location={this.state.external_location} />
				{display_pagination}
				{noresult}
			</div>
		);
	}
});

