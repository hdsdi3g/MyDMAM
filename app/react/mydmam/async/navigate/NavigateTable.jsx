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

navigate.NavigateTable = createReactClass({
	getInitialState: function() {
		return {
			sorted_col: null,
			sorted_order: null,
		};
	},
	handleChangeSort: function(colname, previous_order) {
		var order = null;
		if (previous_order == null) {
			order = "desc";
		} else if (previous_order === "desc") {
			order = "asc";
		}
		this.setState({
			sorted_col: colname,
			sorted_order: order,
		});
		this.props.changeOrderSort(colname, order);
	},
	render: function() {
		var items = this.props.stat.items;
		if (!items) {
			return null;
		}
		if (items.length === 0) {
			return null;
		}
		var reference = this.props.stat.reference;

		var dircontent = [];
		for (var item in items) {
			var newitem = items[item];
			newitem.key = item;
			dircontent.push(newitem);
		}

		var ButtonSort = mydmam.async.ButtonSort;

		var thead = null;
		if (reference.storagename) {
			var order_path = (this.state.sorted_col === 'path' ? this.state.sorted_order : null);
			var order_size = (this.state.sorted_col === 'size' ? this.state.sorted_order : null);
			var order_date = (this.state.sorted_col === 'date' ? this.state.sorted_order : null);

			thead = (
				<thead><tr>
					<td><ButtonSort onChangeState={this.handleChangeSort} colname="path" order={order_path} /></td>
					<td className="pathindex-col-size"><ButtonSort onChangeState={this.handleChangeSort} colname="size" order={order_size} /></td>
					<td className="pathindex-col-date"><ButtonSort onChangeState={this.handleChangeSort} colname="date" order={order_date} /></td>
					<td>&nbsp;</td>
					<td>&nbsp;</td>
				</tr></thead>
			);
		}
		var tbody = [];
		for (var pos = 0; pos < dircontent.length; pos++) {
			var elementkey = dircontent[pos].key;
			var element = dircontent[pos].reference;
			var element_items_total = dircontent[pos].items_total;

			var td_element_name = null;
			var td_element_attributes = null;
			var td_element_date = (<td></td>);

			if (element.directory) {
				var name = null;
				if (reference.storagename) {
					name = (
						<a
							className="tlbdirlistitem"
							href={mydmam.routes.reverse("navigate") + element.storagename + ":" + element.path}>

							{element.path.substring(element.path.lastIndexOf("/") + 1)}
						</a>
					);
				} else {
					name = (
						<a
							className="tlbdirlistitem"
							href={mydmam.routes.reverse("navigate") + element.storagename + ":/"}>

							{element.storagename}
						</a>
					);
				}

				var empty_badge = null;
				if (element_items_total === 0) {
					empty_badge = (
						<span className="badge badge-success" style={{marginLeft: 5}}>
							{i18n('browser.emptydir')}
						</span>
					);
				}

				td_element_name = (
					<th>
						{name}
						{empty_badge}
					</th>
				);
			} else {
				var elementid = null;
				if (element.id) {
					elementid = (<span className="label label-info" style={{marginLeft: 5, marginRight: 5}}>{element.id}</span>);
				}
				td_element_name = (
					<td>
						<a
							className="tlbdirlistitem"
							href={mydmam.routes.reverse("navigate") + element.storagename + ":" + element.path}>

							{elementid}
							{element.path.substring(element.path.lastIndexOf("/") + 1)}
						</a>
					</td>
				);
			}

			if (element.directory) {
				var title = i18n('browser.storagetitle');
				if (reference.storagename != null) {
					title = i18n('browser.directorytitle');
				}
				if (element_items_total != null) {
					if (element_items_total === 0) {
						title += " " + i18n('browser.emptydir');
					} else if (element_items_total == 1) {
						title += ' - ' + i18n('browser.oneelement');
					} else {
						title += ' - ' + i18n('browser.Nelements', element_items_total);
					}
				}
				td_element_attributes = (
					<td>
						<span className="label label-success">
							{title}
						</span>
					</td>
				);
			} else {
				td_element_attributes = (
					<td>
						<mydmam.async.pathindex.reactFileSize size={element.size} style={{marginLeft: 0}}/>
					</td>
				);
			}

			if (reference.storagename != null) {
				td_element_date = (<td><mydmam.async.pathindex.reactDate date={element.date} /></td>);
			}

			tbody.push(
				<tr key={elementkey}>
					{td_element_name}
					{td_element_attributes}
					{td_element_date}
					<td>{mydmam.async.pathindex.mtdTypeofElement(dircontent[pos].mtdsummary)}</td>
					<td><mydmam.async.pathindex.reactExternalLocation storagename={element.storagename} path={element.path} /></td>
				</tr>
			);
		}

		return (
			<table className="table table-hover table-condensed">
				{thead}
				<tbody>{tbody}</tbody>
			</table>
		);
	}
});