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

var searchResult = function(result) {
	if (result.index !== "pathindex") {
		return null;
	}
	return pathindex.react2lines;
};

/**
 * We don't wait the document.ready because we are sure the mydmam.module.f code is already loaded. 
 */
mydmam.module.register("PathIndexView", {
	processViewSearchResult: searchResult,
});

pathindex.reactStoragePathLink = createReactClass({
	render: function() {
		var storagename = this.props.storagename;
		var path = this.props.path;
		var add_link = this.props.add_link;

		var storage_linked = storagename;
		if (add_link) {
			storage_linked = (<a href={mydmam.routes.reverse("navigate") + storagename + ":/"}>{storagename}</a>);
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
						<a href={mydmam.routes.reverse("navigate") + storagename + ':' + currentpath + "/" + sub_path}>
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

pathindex.reactFileSize = createReactClass({
	render: function() {
		if (!this.props.size) {
			return null;
		}
		var style = {marginLeft: 5};
		if (this.props.style) {
			style = this.props.style;
		}
		return (<span className="label label-important" style={style}><i className="icon-briefcase icon-white"></i> {mydmam.format.number(this.props.size)}</span>);
	},
});

pathindex.reactDate = createReactClass({
	render: function() {
		if (!this.props.date) {
			return null;
		}
		if (this.props.date == 0) {
			return null;
		}
		var label = null;
		if (this.props.i18nlabel != null) {
			label = i18n(this.props.i18nlabel) + " ";
		}
		var style = {marginLeft: 5};
		if (this.props.style != null) {
			style = this.props.style;
		}

		var content = mydmam.format.fulldate(this.props.date);

		if (this.props.format == "long") {
			content = (new Date(this.props.date)).getI18nFullDisplayTime();
		}

		return (<span className="label" style={style}>{label}{content}</span>);
	},
});

pathindex.reactSinceDate = createReactClass({
	render: function() {
		if (!this.props.date) {
			return null;
		}
		if (this.props.date == 0) {
			return null;
		}
		var label = null;
		if (this.props.i18nlabel) {
			label = i18n(this.props.i18nlabel) + " ";
		}
		var style = {marginLeft: 5};
		if (this.props.style) {
			style = this.props.style;
		}

		var since = Math.round((new Date().getTime() - this.props.date) / 1000);
		if (since < 0) {
			return <pathindex.reactDate date={this.props.date} i18nlabel={this.props.i18nlabel} style={this.props.style} />;
		}

		return (<span className="label" style={style}>
			<i className="icon-time icon-white" style={{marginTop: 0}}></i>&nbsp;{label}{mydmam.format.secondsToYWDHMS(since)}
		</span>);
	},
});

/**
 * Transform "application/x-dummy" to "application-dummy", and translate it.
 */
pathindex.mtdTypeofElement = function(mtd_element) {
	if (!mtd_element) {
		return "";
	}
	if (!mtd_element.mimetype) {
		return "";
	}
	var element = mtd_element.mimetype;
	var element_type = element.substr(0, element.indexOf('/'));
	var element_subtype = element.substr(element.indexOf('/') + 1);

	if (element_subtype.startsWith("x-")) {
		element_subtype = element_subtype.substr(2);
	}
	element = element_type + "-" + element_subtype;

	var translated_element = i18n("mime." + element);
	if (translated_element.startsWith("mime.")) {
		translated_element = translated_element.substr(5);
	}
	return translated_element;
};
