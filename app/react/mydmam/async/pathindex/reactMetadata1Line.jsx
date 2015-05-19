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

pathindex.reactMetadata1Line = React.createClass({
	render: function() {
		var stat = this.props.stat;
		if (stat == null){
			return null;
		}
		var summary = stat.mtdsummary;
		if (summary == null) {
			return null;
		}

		var titles = [];
		for (var summary_element in summary) {
			if (summary_element == "mimetype") {
				continue;
			}
			if (summary_element == "previews") {
				continue;
			}
			if (summary_element == "master_as_preview") {
				continue;
			}
			titles.push(<span key={summary_element}>{summary[summary_element]}</span>);
		}
		// className="spanmetadata"

		var style = {marginLeft: 5};
		if (this.props.style) {
			style = this.props.style;
		}
		if (titles.length > 0) {
			return (
				<small style={style}>{mydmam.metadatas.typeofelement(summary)} :: {titles}</small>
			);
		} else {
			return (
				<small style={style}>{mydmam.metadatas.typeofelement(summary)}</small>
			);
		}
	}		
});
