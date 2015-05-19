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

search.SearchResults = React.createClass({
	render: function() {
		var stat = this.props.stat;
		var externalpos = this.props.externalpos;

		var resultList = this.props.results.results.map(function (result) {
			var ViewHander = mydmam.module.f.processViewSearchResult(result);
			if (!ViewHander) {
				console.error("Can't handle search result", result, stat[result.key]);
				return (
					<div style={{marginBottom: "1em"}} key={result.reactkey}>
						<div>Error</div>
					</div>
				);
			} else {
				return (
					<div style={{marginBottom: "1em"}} key={result.reactkey}>
						<ViewHander result={result} stat={stat[result.key]} externalpos={externalpos} />
					</div>
				);
			}
		});
	    return (
	    	<div>
		        {resultList}
			</div>
		);
	}
});