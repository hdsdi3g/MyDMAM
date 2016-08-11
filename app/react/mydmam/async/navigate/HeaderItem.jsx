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

navigate.HeaderItem = React.createClass({
	render: function() {
		var url_navigate = mydmam.metadatas.url.navigate_react;
		
		var reference = this.props.stat.reference;
		var mtdsummary = this.props.stat.mtdsummary;

		var first_item_dateindex = this.props.first_item_dateindex;
		if (!reference) {
			return null;
		}

		var header_title = null;
		if (reference.storagename) {
			var navigatetarget = reference.storagename + ":" + reference.path.substring(0, reference.path.lastIndexOf("/"));
		if (reference.path == '/') {
			navigatetarget = "";
		} else if (reference.path.lastIndexOf("/") === 0) {
			navigatetarget = reference.storagename + ":/";
		}

		var url_goback = url_navigate + navigatetarget;
		var go_back = (
			<a
				className="btn btn-mini btngoback"
				style={{marginBottom: "6px", marginRight: "1em"}}
				href={url_goback}
				title={i18n('browser.goback')}>
				<i className="icon-chevron-left"></i>
			</a>
		);

		var is_in_search_label = null;
		if (this.props.is_in_search) {
			is_in_search_label = (<span className="badge badge-info" style={{marginLeft: 10}}>{i18n("browser.search")}</span>);
		}

		var summary = null;
		if (reference.path != "/") {
				var element_name = reference.path.substring(reference.path.lastIndexOf("/") + 1);
				if (mtdsummary) {
					summary = (<small>{mydmam.async.pathindex.mtdTypeofElement(mtdsummary)}</small>);
				} else {
					if (reference.directory) {
						summary = (<small>{i18n("browser.directory")}</small>);
					} else {
						summary = (<small>{i18n("browser.file")}</small>);
					}
				}
				summary = (<span>{element_name} {summary}</span>);
			} else {
				summary = (<span>{reference.storagename} <small>{i18n("browser.storage")}</small></span>);
			}
			header_title = (<span>{go_back} {summary}{is_in_search_label}</span>);
		} else {
			header_title = (
				<span>
					{i18n("browser.storagestitle")} <small>{i18n("browser.storagebaseline")}</small>
				</span>
			);
		}

		var dateindex = first_item_dateindex;
		if (reference.dateindex) {
			dateindex = reference.dateindex;
		}

		var external_pos = null;
		if (reference.directory === false) {
			external_pos = (<mydmam.async.pathindex.reactExternalPosition pathindexkey={this.props.pathindexkey} externalpos={this.props.externalpos} />);
		}

		return (
			<div className="page-header">
				<h3>{header_title}</h3>
				<mydmam.async.pathindex.reactBasketButton pathindexkey={this.props.pathindexkey} />
				<mydmam.async.pathindex.reactDate date={reference.date} i18nlabel={"browser.file.modifiedat"} />
				<mydmam.async.pathindex.reactDate date={dateindex} i18nlabel={"browser.file.indexedat"} />
				<mydmam.async.pathindex.reactFileSize size={reference.size} />
				{external_pos}
			</div>
		);
	}
});