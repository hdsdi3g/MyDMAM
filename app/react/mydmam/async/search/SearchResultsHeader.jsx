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

search.SearchResultsHeader = React.createClass({
	render: function() {
		if (this.props.results.results.length == 0) {
			return (
				<div className="alert alert-info">
					<h4>{i18n("search.noresults")}</h4>
					{i18n("search.noresultsfor")} <strong>{this.props.results.q}</strong><br />
					<small>({this.props.results.duration / 1000} {i18n("search.seconds")})</small>.
				</div>
			);
		} else {
			var pageon = (<span>{i18n("search.oneresult")}</span>);
			var pageadd = null;
			if (this.props.results.total_items_count > 1) {
				pageon = (<span>{this.props.results.total_items_count} {i18n("search.results")}</span>);
				if (this.props.results.pagecount > 1) {
					pageadd = (<span>{i18n("search.pageXonY", this.props.results.from, this.props.results.pagecount)}</span>);
				}
			}

			return (
				<p><small className="muted">
					{pageon} {pageadd} ({this.props.results.duration / 1000} {i18n("search.seconds")})
				</small><br />
				<small>
					{i18n("search.method." + this.props.results.mode.toLowerCase())}
				</small>
				</p>
			);
		}
	}
});