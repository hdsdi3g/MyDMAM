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

//TODO refactor this
pathindex.resolveExternalPosition = function(externalpos_request_keys, data_callback) {
	return;
	/*if (!externalpos_request_keys) {
		return;
	}
	if (externalpos_request_keys.length == 0) {
		return;
	}
	$.ajax({
		url: mydmam.metadatas.url.resolvepositions,
		type: "POST",
		data: {
			"keys": externalpos_request_keys,
		},
		success: data_callback,
		error: function(jqXHR, textStatus, errorThrown) {
			console.error(jqXHR, textStatus, errorThrown);
		},
	});*/
};

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

pathindex.reactFileSize = React.createClass({
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

pathindex.reactDate = React.createClass({
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
		return (<span className="label" style={style}>{label}{mydmam.format.fulldate(this.props.date)}</span>);
	},
});

pathindex.reactSinceDate = React.createClass({
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

pathindex.reactBasketButton = React.createClass({
	getInitialState: function() {
		return {present_in_basket: mydmam.basket.isInBasket(this.props.pathindexkey)};
	},
	handleBasketSwitch: function(event) {
		if (this.state.present_in_basket) {
			mydmam.basket.content.remove(this.props.pathindexkey);
		} else {
			mydmam.basket.content.add(this.props.pathindexkey);
		}
		this.setState({present_in_basket: !this.state.present_in_basket});
	},
	render: function() {
		if (this.props.pathindexkey === md5('')) {
			return null;
		}
		var btn_basket_classes = classNames({
		    'btn': true, 'btn-mini': true, 'basket': true,
		    'active': this.state.present_in_basket,
		});

		var icon = (<i className="icon-star-empty"></i>);
		if (this.state.present_in_basket) {
			icon = (<i className="icon-star"></i>);
		}

		return (
			<button className={btn_basket_classes} type="button" onClick={this.handleBasketSwitch}>
				{icon}
			</button>
		);
	},
});