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

pathindex.reactMetadata1Line = createReactClass({
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
		if (summary.summaries) {
			for (var summary_element in summary.summaries) {
				titles.push(<span key={summary_element}>{summary.summaries[summary_element]}</span>);
			}
		}

		var style = {marginLeft: 5};
		if (this.props.style) {
			style = this.props.style;
		}
		if (titles.length > 0) {
			return (
				<small style={style}>{mydmam.async.pathindex.mtdTypeofElement(summary)} :: {titles}</small>
			);
		} else {
			return (
				<small style={style}>{mydmam.async.pathindex.mtdTypeofElement(summary)}</small>
			);
		}
	}		
});
