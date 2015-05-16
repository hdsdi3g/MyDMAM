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
		};
	},
	navigateTo: function(pathindex, page_from, page_size, sort) {
		var stat = mydmam.stat;
		var pathindex_key = md5(pathindex);
		if (sort == null) {
			sort = this.state.sort_order;
		}
		var request = {
			pathelementskeys: [pathindex_key],
			page_from: page_from,
			page_size: page_size,
			scopes_element: [stat.SCOPE_DIRLIST, stat.SCOPE_PATHINFO, stat.SCOPE_MTD_SUMMARY, stat.SCOPE_COUNT_ITEMS],
			scopes_subelements: [stat.SCOPE_MTD_SUMMARY, stat.SCOPE_COUNT_ITEMS],
			search: JSON.stringify(''),
			sort: sort,
		};

		//TODO manage search
		// https://facebook.github.io/react/tips/use-react-with-other-libraries.html
		/*
		$('#sitesearch').bind('keyup.DT', function(e) {
			var val = this.value === "" ? "" : this.value;
			$('.dataTables_filter input').val(val);
			$('.dataTables_filter input').trigger("keyup.DT");
		});
		*/

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
		if (stat.items_total) {
			var Pagination = mydmam.async.pagination.reactBlock;
			display_pagination = (
				<Pagination
					pagecount={Math.ceil(stat.items_total / stat.items_page_size)}
					currentpage={stat.items_page_from + 1}
					onClickButton={this.handlePaginationSwitchPage} />
			);
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
					navigate={this.handleOnClickANavigateToNewDest} />
				<mydmam.async.pathindex.reactMetadataFull
					reference={stat.reference}
					mtdsummary={stat.mtdsummary}
					navigate={this.handleOnClickANavigateToNewDest} />
				<navigate.NavigateTable
					stat={stat}
					navigate={this.handleOnClickANavigateToNewDest}
					changeOrderSort={this.handlechangeOrderSort} />
				{display_pagination}
			</div>
		);
	}
});