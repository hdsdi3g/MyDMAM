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

navigate.BreadCrumb = React.createClass({
	render: function() {
		var url_navigate = mydmam.metadatas.url.navigate_react;
		
		var storagename = this.props.storagename;
		var path = this.props.path;
		if (storagename == null) {
			return (
				<ul className="breadcrumb">
					<li className="active">
						{i18n('browser.storagestitle')}
					</li>
				</ul>
			);
		}
		
		var element_subpaths = path.split("/");
		var currentpath = "";
		var newpath = "";
		var items = [];
		for (var pos = 1; pos < element_subpaths.length; pos++) {
			newpath = storagename + ':' + currentpath + "/" + element_subpaths[pos];
			if (pos + 1 < element_subpaths.length) {
				items.push(
					<li key={pos}>
						<span className="divider">/</span>
						<a href={url_navigate + newpath}>
							{element_subpaths[pos]}
						</a>
					</li>
				);
			} else {
				items.push(
					<li key={pos} className="active">
						<span className="divider">/</span>
						{element_subpaths[pos]}
					</li>
				);
			}
			currentpath = currentpath + "/" + element_subpaths[pos];
		}

		var header = [];
		if (items.length > 0) {
			header.push(
				<li key="storagestitle">
					<a href={url_navigate}>
						{i18n('browser.storagestitle')}
					</a>
					<span className="divider">::</span>
				</li>
			);
			if (path != "/") {
				header.push(
					<li key="root">
						<a href={url_navigate + storagename + ':/'}>
							{storagename}
						</a>
					</li>
				);
			} else {
				header.push(
					<li key="root" className="active">
						{storagename}
					</li>
				);
			}
			return (
				<ul className="breadcrumb">
					{header}
					{items}
				</ul>
			);
		} 
		return null;
	}
});

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
		if (this.props.in_search != null) {
			is_in_search_label = (<span className="badge badge-info" style={{marginLeft: 10}}>{i18n("browser.search", this.props.in_search.trim())}</span>);
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

		return (
			<div className="page-header">
				<h3>{header_title}</h3>
				<mydmam.async.pathindex.reactBasketButton pathindexkey={this.props.pathindexkey} />
				<mydmam.async.pathindex.reactDate date={reference.date} i18nlabel={"browser.file.modifiedat"} />
				<mydmam.async.pathindex.reactDate date={dateindex} i18nlabel={"browser.file.indexedat"} />
				<mydmam.async.pathindex.reactFileSize size={reference.size} />
				<mydmam.async.pathindex.reactExternalLocation storagename={reference.storagename} path={reference.path} external_location={this.props.external_location} />
			</div>
		);
	}
});

navigate.NoResultsSearch = React.createClass({
	render: function() {
		return (
			<div className="alert alert-info">
				<h4>{i18n("search.noresults")}</h4>
			</div>
		);
	}
});