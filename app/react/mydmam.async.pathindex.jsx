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

(function(mydmam) {
	mydmam.async.pathindex = {};

	mydmam.async.pathindex.react2lines = React.createClass({
		getInitialState: function() {
			return {present_in_basket: mydmam.basket.isInBasket(this.props.result.key)};
		},
		handleBasketSwitch: function(event) {
			if (this.state.present_in_basket) {
				mydmam.basket.content.remove(this.props.result.key);
			} else {
				mydmam.basket.content.add(this.props.result.key);
			}
			this.setState({present_in_basket: !this.state.present_in_basket});
		},
		shouldComponentUpdate: function(nextProps, nextState) {
			//TODO return false if mtd & external pos == null
			return true;
		}
		render: function() {
			var result = this.props.result;
			if (result.index !== "pathindex") {
				return null;
			}

			var date_block = null;
			if (result.content.date) {
				date_block = (<span className="label">{mydmam.format.fulldate(result.content.date)}</span>);
			}

			var size_block = null;
			if (result.content.size) {
				size_block = (<span className="label label-important"><i className="icon-briefcase icon-white"></i> {mydmam.format.number(result.content.size)}</span>);
			}

			var directory_block = null;
			if (result.content.directory) {
				directory_block = (<span className="label label-success">{i18n("search.result.directory")}</span>);
			}

			//<span id="mtd-${item.getId()}"></span>

			var path_linked = [];
			var sub_paths = result.content.path.split("/");
			var sub_path;
			var currentpath = "";
			for (var i = 1; i < sub_paths.length; i++) {
				sub_path = sub_paths[i];
				path_linked.push(
					<span key={i}>/
						<a href={mydmam.metadatas.url.navigate + "#" + result.content.storagename + ':' + currentpath + "/" + sub_path}>
							{sub_path}
						</a>
					</span>
				);
				currentpath = currentpath + "/" + sub_path;
			};

			var btn_basket_classes = classNames({
			    'btn': true, 'btn-mini': true,
			    'active': this.state.present_in_basket,
			});

			return (
				<div className="pathindex">
					<span className="label label-inverse">{i18n("search.result.storage")}</span>
					<span className="label label-info">{result.content.id}</span> {date_block} {size_block} {directory_block}
					<br />
					<span>
						<button className={btn_basket_classes} type="button" onClick={this.handleBasketSwitch}><i className="icon-star"></i></button>&nbsp;
						<strong className="storagename">
							<a href={mydmam.metadatas.url.navigate + "#" + result.content.storagename + ":/"}>
								{result.content.storagename}
							</a>
						</strong>
						&nbsp;::&nbsp;
						<span className="path">
							{path_linked}
						</span>
					</span>
				</div>
			);
		}
	});

	var searchResult = function(result) {
		var React2lines = mydmam.async.pathindex.react2lines;
		return (<React2lines result={result} />);
	};

	/**
	 * We don't wait the document.ready because we sure the mydmam.module.f code is already loaded. 
	 */
	mydmam.module.register("PathIndexView", {processViewSearchResult: searchResult});
})(window.mydmam);
