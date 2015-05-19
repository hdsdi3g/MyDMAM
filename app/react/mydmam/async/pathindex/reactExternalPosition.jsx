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

pathindex.reactExternalPosition = React.createClass({
	render: function() {
		var externalpos = this.props.externalpos;
		var pathindexkey = this.props.pathindexkey;

		if (!externalpos.positions) {
			return (
				<span />
			);
		}
		var positions = externalpos.positions[pathindexkey];
		if (!positions) {
			return (
				<span />
			);
		}
		
		var style = {float: "right"};

		var react_positions = [];
		var tapelocation;
		for (var i = 0; i < positions.length; i++) {
			if (positions[i] == "cache") {
				react_positions.push(
					<span style={style} className="label label-success external-pathindex-position" key={i}>
						<i className="icon-barcode icon-white"></i> <i className="icon-ok icon-white"></i> {i18n("browser.externalposition.online")}
					</span>
				);
				break;
			}
		 	tapelocation = externalpos.locations[positions[i]];
			if (!tapelocation) {
				continue;
			}
			if (tapelocation.isexternal) {
				react_positions.push(
					<span style={style} className="label label-important external-pathindex-position" key={i}>
						<i className="icon-barcode icon-white"></i> {tapelocation.barcode}
					</span>
				);
			} else {
				react_positions.push(
					<span style={style} className="label label-success external-pathindex-position" key={i}>
						<i className="icon-barcode icon-white"></i> <i className="icon-screenshot icon-white"></i> {mydmam.module.f.i18nExternalPosition(tapelocation.location)}
					</span>
				);
			}
		}; 
		return (
			<span style={{float: "clear"}}>{react_positions}</span>
		);
	}
});