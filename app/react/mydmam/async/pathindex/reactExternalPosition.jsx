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

var external_location_storage_list = null;

pathindex.resolveExternalLocation = function(storagename, path, onResult) {
	if (external_location_storage_list == null) {
		mydmam.async.request("stat", "getExternalLocationStorageList", null, function(data) {
			external_location_storage_list = data;
			pathindex.resolveExternalLocation(storagename, path, onResult);
		});
		return;
	}

	if (external_location_storage_list.indexOf(storagename) == -1) {
		return;
	}

	mydmam.async.request("stat", "getExternalLocation", {storagename: storagename, path: path}, function(data) {
		onResult(data);
	});
}

/**
 * You must call pathindex.resolveExternalLocation before this. Else it never display anything.
 */
pathindex.reactExternalLocation = React.createClass({
	getInitialState: function() {
		return {
			expand_tapes: false,
			external_location: null,
		};
	},
	onExpandLabelForDisplayTapes: function() {
		this.setState({expand_tapes: !this.state.expand_tapes});
	},
	onStartSearchLocalisation: function() {
		pathindex.resolveExternalLocation(this.props.storagename, this.props.path, function(external_location) {
			this.setState({external_location: external_location});
		}.bind(this));
	},
	render: function() {
		if (!external_location_storage_list) {
			return (
				<span />
			);	
		}

		var storagename = this.props.storagename;
		var path = this.props.path;
		var external_location = this.props.external_location;
		if (!external_location) {
			external_location = this.state.external_location;
		}

		if (external_location) {
			/** Display location */
			var style = {cursor: "pointer"};
			var tapes = [];
			if (this.state.expand_tapes) {
				for (var pos in external_location.locations) {
				 	var location = external_location.locations[pos];
				 	var type = location.type;
				 	if (type != "TAPE") {
				 		type = <span style={{marginLeft: 5,}}>{i18n("browser.externallocation." + type.toLowerCase())}</span>;
				 	} else {
				 		type = null;
				 	}

				 	if (location.availability == "ONLINE") {
					 	tapes.push(<span style={{marginLeft: 5,}} key={pos}>
					 		{type} {location.source} 
					 	</span>);
				 	} else if (location.availability == "NEARLINE") {
					 	tapes.push(<span style={{marginLeft: 5,}} key={pos}>
					 		<i className="icon-white icon-ok"></i>
					 		{type} {location.source} 
					 	</span>);
				 	} else {
					 	tapes.push(<span style={{marginLeft: 5,}} key={pos}>
					 		<i className="icon-white icon-hand-right"></i>
					 		{type} {location.source} 
					 	</span>);
				 	}

				}
			}

			var availability = external_location.availability;
			if (availability == "ONLINE") {
				availability = (<span style={style} className="label label-success external-pathindex-position" onClick={this.onExpandLabelForDisplayTapes}>
					<i className="icon-barcode icon-white"></i> <i className="icon-ok icon-white"></i> {i18n("browser.externallocation.online")}
					{tapes}
				</span>);
			} else if (availability == "NEARLINE") {
				availability = (<span style={style} className="label label-success external-pathindex-position" onClick={this.onExpandLabelForDisplayTapes}>
					<i className="icon-barcode icon-white"></i> <i className="icon-ok icon-white"></i> {i18n("browser.externallocation.nearline")}
					{tapes}
				</span>);
			} else {
				availability = (<span style={style} className="label label-important external-pathindex-position" onClick={this.onExpandLabelForDisplayTapes}>
					<i className="icon-barcode icon-white"></i> <i className="icon-warning-sign icon-white"></i> {i18n("browser.externallocation.offline")}
					{tapes}
				</span>);
			}

			return (
				<span style={{display: "inline-block", whiteSpace: "nowrap"}}>{availability}</span>
			);
		} else {
			if (external_location_storage_list.indexOf(storagename) > -1) {
				/** add button to get location */
				return (<span style={{display: "inline-block", whiteSpace: "nowrap", cursor: "pointer"}} className="label label-important external-pathindex-position" onClick={this.onStartSearchLocalisation}>
						<i className="icon-barcode icon-white"></i> <i className="icon-search icon-white"></i>
				</span>);
			}
		}

		/** Nothing */
		return (
			<span />
		);
	}
});