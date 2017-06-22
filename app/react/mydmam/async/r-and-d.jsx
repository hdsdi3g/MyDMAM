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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
 * Research and development for some new tools
 */

/* 
 * Bound in a div one-line items, with a strict width
 * Usage : <DivWidth width="5em" dotted={true}>This is a too long text</DivWidth>
 *         <DivWidth width="2em" ellipsis={true}>Short text</DivWidth>
 */
async.DivWidth = createReactClass({
	propTypes: {
		/** css width */
		width: PropTypes.oneOfType([
			PropTypes.string,
			PropTypes.number,
		]),
		/** Add "..." before the end */ 
		ellipsis: PropTypes.bool,
		/** Add 1px dotted border, for debugging */
		dotted: PropTypes.bool,
		/** Items to add in the div */
		children: PropTypes.node.isRequired,
	},
	render: function() {
		var width = this.props.width;
		var ellipsis = this.props.ellipsis;
		var dotted = this.props.dotted;

		var text_overflow = "clip";
		if (ellipsis) {
			text_overflow = "ellipsis";
		}

		var border = null;
		if (dotted) {
			border = "1px dotted black";
		}	
			
		var style = {
			overflow: "hidden",
			border: border,
			whiteSpace: "nowrap",
			textOverflow: text_overflow,
			width: width,
		};

		return (<div style={style}>
			{this.props.children}
		</div>);
	}
});

