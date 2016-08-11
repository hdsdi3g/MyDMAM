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