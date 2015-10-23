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

pathindex.reactStoragePathLink = React.createClass({
	render: function() {
		var url_navigate = mydmam.metadatas.url.navigate_react;

		var storagename = this.props.storagename;
		var path = this.props.path;
		var add_link = this.props.add_link;

		var storage_linked = storagename;
		if (add_link) {
			storage_linked = (<a href={url_navigate + "#" + storagename + ":/"}>{storagename}</a>);
		}		

		var path_linked = path;
		if (add_link) {
			path_linked = [];
			var sub_paths = path.split("/");
			var sub_path;
			var currentpath = "";
			for (var i = 1; i < sub_paths.length; i++) {
				sub_path = sub_paths[i];
				path_linked.push(
					<span key={i}>/
						<a href={url_navigate + "#" + storagename + ':' + currentpath + "/" + sub_path}>
							{sub_path}
						</a>
					</span>
				);
				currentpath = currentpath + "/" + sub_path;
			};
		}

		return (
			<span>
				<strong className="storagename">
					{storage_linked}
				</strong>
				&nbsp;::&nbsp;
				<span className="path">
					{path_linked}
				</span>
			</span>
		);
	}
});