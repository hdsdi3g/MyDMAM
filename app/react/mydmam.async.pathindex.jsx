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

	var externalPos = function(result) {
		if (result.index !== "pathindex") {
			return null;
		}
		if (result.content.directory) {
			return null;
		}
		for (var pos = 0; pos < list_external_positions_storages.length; pos++) {
			if (list_external_positions_storages[pos] === result.content.storagename) {
				return result.key;
			}
		}
		return null;
	};

	mydmam.async.pathindex.reactBasketButton = React.createClass({
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

	mydmam.async.pathindex.reactMetadata1Line = React.createClass({
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

	mydmam.async.pathindex.reactExternalPosition = React.createClass({
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
			
			var react_positions = [];
			var tapelocation;
			for (var i = 0; i < positions.length; i++) {
				if (positions[i] == "cache") {
					react_positions.push(
						<span className="label label-success external-pathindex-position" key={i}>
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
						<span className="label label-important external-pathindex-position" key={i}>
							<i className="icon-barcode icon-white"></i> {tapelocation.barcode}
						</span>
					);
				} else {
					react_positions.push(
						<span className="label label-success external-pathindex-position" key={i}>
							<i className="icon-barcode icon-white"></i> <i className="icon-screenshot icon-white"></i> {mydmam.module.f.i18nExternalPosition(tapelocation.location)}
						</span>
					);
				}
			}; 
			return (
				<span>{react_positions}</span>
			);
		}
	});

	mydmam.async.pathindex.reactDate = React.createClass({
		render: function() {
			if (!this.props.date) {
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
			return (<span className="label" style={style}>{label}{mydmam.format.fulldate(this.props.date)}</span>);
		},
	});

	mydmam.async.pathindex.reactFileSize = React.createClass({
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

	mydmam.async.pathindex.react2lines = React.createClass({
		shouldComponentUpdate: function(nextProps, nextState) {
			if (nextProps.externalpos.positions) {
				if (nextProps.externalpos.positions[nextProps.result.key]) {
					return true;
				}
			}

			if (nextProps.stat == null) {
				return false;
			}

			return true;
		},
		render: function() {
			var url_navigate = mydmam.metadatas.url.navigate_react;
			
			var result = this.props.result;
			var directory_block = null;
			if (result.content.directory) {
				directory_block = (<span className="label label-success" style={{marginLeft: 5}}>{i18n("search.result.directory")}</span>);
			}

			var path_linked = [];
			var sub_paths = result.content.path.split("/");
			var sub_path;
			var currentpath = "";
			for (var i = 1; i < sub_paths.length; i++) {
				sub_path = sub_paths[i];
				path_linked.push(
					<span key={i}>/
						<a href={url_navigate + "#" + result.content.storagename + ':' + currentpath + "/" + sub_path}>
							{sub_path}
						</a>
					</span>
				);
				currentpath = currentpath + "/" + sub_path;
			};

			var BasketButton = mydmam.async.pathindex.reactBasketButton;
			var Metadata1Line = mydmam.async.pathindex.reactMetadata1Line;
			var ExternalPosition = mydmam.async.pathindex.reactExternalPosition;
			var DateBlock = mydmam.async.pathindex.reactDate;
			var SizeBlock = mydmam.async.pathindex.reactFileSize;

			return (
				<div className="pathindex">
					<span className="label label-inverse">{i18n("search.result.storage")}</span>
					<span className="label label-info" style={{marginLeft: 5}}>{result.content.id}</span>
					<DateBlock date={result.content.date} />
					<SizeBlock size={result.content.size} />
					{directory_block}
					<Metadata1Line stat={this.props.stat} />
					<br />
					<span>
						<BasketButton pathindexkey={this.props.result.key}/>
						<ExternalPosition pathindexkey={this.props.result.key} externalpos={this.props.externalpos} />&nbsp;
						<strong className="storagename">
							<a href={url_navigate + "#" + result.content.storagename + ":/"}>
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
		if (result.index !== "pathindex") {
			return null;
		}
		return mydmam.async.pathindex.react2lines;
	};

	/**
	 * We don't wait the document.ready because we sure the mydmam.module.f code is already loaded. 
	 */
	mydmam.module.register("PathIndexView", {
		processViewSearchResult: searchResult,
		wantToHaveResolvedExternalPositions: externalPos,
	});
})(window.mydmam);
