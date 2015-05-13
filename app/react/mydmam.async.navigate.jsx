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

	mydmam.async.navigate = function(pathindex_destination, dom_target) {
		if (!dom_target) {
			return;
		}
		
		var url_navigate = mydmam.metadatas.url.navigate_react;

		var BasketButton = mydmam.async.pathindex.reactBasketButton;
		var DateBlock = mydmam.async.pathindex.reactDate;
		var FileSize = mydmam.async.pathindex.reactFileSize;
		var Metadata1Line = mydmam.async.pathindex.reactMetadata1Line;

		var ButtonSort = React.createClass({
			getInitialState: function() {
				return {
					sort: null,
				};
			},
			componentDidMount: function() {
				//TODO get current sort by stat result
			},
			handleClick: function(e) {
				e.preventDefault();
				//TODO
			},
			render: function() {
				//this.props.colname
				//this.props.onChangeState(colname, order);
				//TODO

				var is_up = false;
				var is_down = true;
				var btn_active = false;
				if (this.state.sort != null) {
					is_up = (this.state.sort === 'asc');
					is_down = (this.state.sort === 'desc');
					btn_active = true;
				}

				var btn_classes = classNames({
				    'btn': true, 'btn-mini': true, 'pull-right': true,
			    	'active': btn_active,
				});
				var icon_classes = classNames({
					'pull-right': true,
				    'icon-chevron-up': is_up,
				    'icon-chevron-down': is_down,
				});

				return (
					<button className={btn_classes}>
						<i className={icon_classes}></i>
					</button>
				);
			}
		});

		var BreadCrumb = React.createClass({
			render: function() {
				var storagename = this.props.storagename;
				var path = this.props.path;
				if (storagename == null) {
					return (
						<ul className="breadcrumb">
							<li className="active">
								{i18n('browser.storagestitle')}
							</li>
						</ul>
					);
				}
				
				var element_subpaths = path.split("/");
				var currentpath = "";
				var newpath = "";
				var items = [];
				for (var pos = 1; pos < element_subpaths.length; pos++) {
					newpath = storagename + ':' + currentpath + "/" + element_subpaths[pos];
					if (pos + 1 < element_subpaths.length) {
						items.push(
							<li key={pos}>
								<span className="divider">/</span>
								<a href={url_navigate + "#" + newpath} onClick={this.props.navigate} data-navigatetarget={newpath}>
									{element_subpaths[pos]}
								</a>
							</li>
						);
					} else {
						items.push(
							<li key={pos} className="active">
								<span className="divider">/</span>
								{element_subpaths[pos]}
							</li>
						);
					}
					currentpath = currentpath + "/" + element_subpaths[pos];
				}

				var header = [];
				if (items.length > 0) {
					header.push(
						<li key="storagestitle">
							<a href={url_navigate} onClick={this.props.navigate} data-navigatetarget="">
								{i18n('browser.storagestitle')}
							</a>
							<span className="divider">::</span>
						</li>
					);
					if (path != "/") {
						header.push(
							<li key="root">
								<a href={url_navigate + "#" + storagename + ':/'} onClick={this.props.navigate} data-navigatetarget={storagename + ':/'}>
									{storagename}
								</a>
							</li>
						);
					} else {
						header.push(
							<li key="root" className="active">
								{storagename}
							</li>
						);
					}
					return (
						<ul className="breadcrumb">
							{header}
							{items}
						</ul>
					);
				} 
				return null;
			}
		});

		var HeaderItem = React.createClass({
			render: function() {
				var reference = this.props.reference;
				var first_item_dateindex = this.props.first_item_dateindex;
				if (!reference) {
					return null;
				}

				var header_title = null;
				if (reference.storagename) {
					var navigatetarget = reference.storagename + ":" + reference.path.substring(0, reference.path.lastIndexOf("/"));
					if (reference.path == '/') {
						navigatetarget = "";
					} else if (reference.path.lastIndexOf("/") === 0) {
						navigatetarget = reference.storagename + ":/";
					}
					var url_goback = url_navigate + "#" + navigatetarget;
					var go_back = (
						<a
							className="btn btn-mini btngoback"
							style={{marginBottom: "6px", marginRight: "1em"}}
							href={url_goback}
							title={i18n('browser.goback')}
							onClick={this.props.navigate}
							data-navigatetarget={navigatetarget}>
							<i className="icon-chevron-left"></i>
						</a>
					);

					var summary = null;
					if (reference.path != "/") {
						var element_name = reference.path.substring(reference.path.lastIndexOf("/") + 1);
						if (reference.mtdsummary) {
							summary = (<small>{mydmam.metadatas.typeofelement(reference.mtdsummary)}</small>);
						} else {
							if (reference.directory) {
								summary = (<small>{i18n("browser.directory")}</small>);
							} else {
								summary = (<small>{i18n("browser.file")}</small>);
							}
						}
						summary = (<span>{element_name} {summary}</span>);
					} else {
						summary = (<span>{reference.storagename} <small>{i18n("browser.storage")}</small></span>);
					}
					header_title = (<span>{go_back} {summary}</span>);
				} else {
					header_title = (
						<span>
							{i18n("browser.storagestitle")} <small>{i18n("browser.storagebaseline")}</small>
						</span>
					);
				}

				var dateindex = first_item_dateindex;
				if (reference.dateindex) {
					dateindex = reference.dateindex;
				}

				if (reference.directory === false) {
					//external_elements_to_resolve.push(md5_fullpath);
					//content = content + '<span id="elmextern-' + md5_fullpath + '"></span>'; TODO show external key.
				}
				
				return (
					<div className="page-header">
						<h3>{header_title}</h3>
						<BasketButton pathindexkey={this.props.pathindexkey} />
						<DateBlock date={reference.date} i18nlabel={"browser.file.modifiedat"} />
						<DateBlock date={dateindex} i18nlabel={"browser.file.indexedat"} />
						<FileSize size={reference.size} />
					</div>
				);
			}
		});

		var ItemContent = mydmam.async.pathindex.reactMetadataFull;

		var NavigateTable = React.createClass({
			handleChangeSort: function(colname) {
				console.log(colname);//TODO
			},
			render: function() {
				var items = this.props.stat.items;
				if (!items) {
					return null;
				}
				if (items.length === 0) {
					return null;
				}
				var reference = this.props.stat.reference;

				var dircontent = [];
				for (var item in items) {
					var newitem = items[item];
					newitem.key = item;
					dircontent.push(newitem);
				}

				var thead = null;
				if (reference.storagename) {
					thead = (
						<thead><tr>
							<td><ButtonSort onChangeState={this.handleChangeSort} colname="path" /></td>
							<td><ButtonSort onChangeState={this.handleChangeSort} colname="size" /></td>
							<td><ButtonSort onChangeState={this.handleChangeSort} colname="date" /></td>
							<td>&nbsp;</td>
							<td>&nbsp;</td>
						</tr></thead>
					);
				}
				var tbody = [];
				for (var pos = 0; pos < dircontent.length; pos++) {
					var elementkey = dircontent[pos].key;
					var element = dircontent[pos].reference;
					var element_items_total = dircontent[pos].items_total;

					if (element.directory === false) {
						//external_elements_to_resolve.push(elementkey); TODO external pos
					}

					var td_element_name = null;
					var td_element_attributes = null;
					var td_element_date = (<td></td>);

					if (element.directory) {
						var name = null;
						if (reference.storagename) {
							name = (
								<a
									className="tlbdirlistitem"
									href={url_navigate + "#" + element.storagename + ":" + element.path}
									onClick={this.props.navigate}
									data-navigatetarget={element.storagename + ":" + element.path}>

									{element.path.substring(element.path.lastIndexOf("/") + 1)}
								</a>
							);
						} else {
							name = (
								<a
									className="tlbdirlistitem"
									href={url_navigate + "#" + element.storagename + ":/"}
									onClick={this.props.navigate}
									data-navigatetarget={element.storagename + ":/"}>

									{element.storagename}
								</a>
							);
						}

						var empty_badge = null;
						if (element_items_total === 0) {
							empty_badge = (
								<span className="badge badge-success" style={{marginLeft: 5}}>
									{i18n('browser.emptydir')}
								</span>
							);
						}

						td_element_name = (
							<th>
								<BasketButton pathindexkey={elementkey} />
								{name}
								{empty_badge}
							</th>
						);
					} else {
						var elementid = null;
						if (element.id) {
							elementid = (<span className="label label-info" style={{marginLeft: 5, marginRight: 5}}>{element.id}</span>);
						}
						td_element_name = (
							<td>
								<BasketButton pathindexkey={elementkey} />
								<a
									className="tlbdirlistitem"
									href={url_navigate + "#" + element.storagename + ":" + element.path}
									onClick={this.props.navigate}
									data-navigatetarget={element.storagename + ":" + element.path}>

									{elementid}
									{element.path.substring(element.path.lastIndexOf("/") + 1)}
								</a>
							</td>
						);
					}

					if (element.directory) {
						var title = i18n('browser.storagetitle');
						if (reference.storagename != null) {
							title = i18n('browser.directorytitle');
						}
						if (element_items_total != null) {
							if (element_items_total === 0) {
								title += i18n('browser.emptydir');
							} else if (element_items_total == 1) {
								title += ' - ' + i18n('browser.oneelement');
							} else {
								title += ' - ' + i18n('browser.Nelements', element_items_total);
							}
						}
						td_element_attributes = (
							<td>
								<span className="label label-success">
									{title}
								</span>
							</td>
						);
					} else {
						td_element_attributes = (
							<td>
								<FileSize size={element.size} style={{marginLeft: 0}}/>
							</td>
						);
					}

					if (reference.storagename != null) {
						td_element_date = (<td><DateBlock date={element.date} /></td>);
					}

					//content = content + '<td id="elmextern-' + elementkey + '"></td>'; TODO external pos...

					tbody.push(
						<tr key={elementkey}>
							{td_element_name}
							{td_element_attributes}
							{td_element_date}
							<td><Metadata1Line stat={dircontent[pos]} /></td>
						</tr>
					);
				}

				return (
					<table className="table table-hover table-condensed">
						{thead}
						<tbody>{tbody}</tbody>
					</table>
				);
			}
		});

		var NavigatePage = React.createClass({
			getInitialState: function() {
				return {
					stat: {},
					pathindex: "",
					default_page_size: 20,
				};
			},
			navigateTo: function(pathindex, page_from, page_size) {
				var stat = mydmam.stat;
				var pathindex_key = md5(pathindex);
				var request = {
					pathelementskeys: [pathindex_key],
					page_from: page_from,
					page_size: page_size,
					scopes_element: [stat.SCOPE_DIRLIST, stat.SCOPE_PATHINFO, stat.SCOPE_MTD_SUMMARY, stat.SCOPE_COUNT_ITEMS],
					scopes_subelements: [stat.SCOPE_MTD_SUMMARY, stat.SCOPE_COUNT_ITEMS],
					search: JSON.stringify(''),
					sort: [
						//@see stat.SortDirListing.Col: sortedfilename, date, directory, size
						// ASC / DESC
						//{colname: "size", order: "ASC"}
					],
				};

				//TODO manage search
				// https://facebook.github.io/react/tips/use-react-with-other-libraries.html
				/*
				$('#sitesearch').bind('keyup.DT', function(e) {
					var val = this.value === "" ? "" : this.value;
					$('.dataTables_filter input').val(val);
					$('.dataTables_filter input').trigger("keyup.DT");
				});
				*/

				document.body.style.cursor = 'wait';
				mydmam.async.request("stat", "cache", request, function(data) {
					document.body.style.cursor = 'default';
					if (data[pathindex_key]) {
						this.setState({
							stat: data,
							pathindex: pathindex,
							default_page_size: page_size,
						});
					} else {
						if (page_from > 0) {
							this.navigateTo(pathindex, 0, page_size);
						}
						return;
					}
					window.location.hash = this.state.pathindex;
				}.bind(this));
			},
			componentDidMount: function() {
				this.navigateTo(pathindex_destination, 0, this.state.default_page_size);
			},
			handlePaginationSwitchPage: function(newpage, alt_pressed) {
				if (alt_pressed){
					this.navigateTo(this.state.pathindex, 0, this.state.default_page_size * 2);
				} else {
					this.navigateTo(this.state.pathindex, newpage - 1, this.state.default_page_size);
				}
			},
			handleOnClickANavigateToNewDest: function(e) {
				e.preventDefault();
				var pathindex_target = $(e.currentTarget).data("navigatetarget");
				this.navigateTo(pathindex_target, 0, this.state.default_page_size);
			},
			render: function() {
				var stat = this.state.stat[md5(this.state.pathindex)];
				if (!stat) {
					return (<div></div>);
				}

				/**
				 * Fake (get from the first item), but realist indexdate. It's a backup solution.
				 */
				var first_item_dateindex = 0;
				if (stat.items) {
					for (var item in stat.items) {
						var newitem = stat.items[item];
						if (newitem.reference) {
							if (newitem.reference.dateindex) {
								first_item_dateindex = newitem.reference.dateindex;
								break;
							}
						}
					}
				}
				var storagename = null;
				if (stat.reference) {
					storagename = stat.reference.storagename;
				}
				var path = null;
				if (stat.reference) {
					path = stat.reference.path;
				}
				
				var display_pagination = null;
				if (stat.items_total) {
					var Pagination = mydmam.async.pagination.reactBlock;
					display_pagination = (
						<Pagination
							pagecount={Math.ceil(stat.items_total / stat.items_page_size)}
							currentpage={stat.items_page_from + 1}
							onClickButton={this.handlePaginationSwitchPage} />
					);
				}

				return (
					<div>
						<BreadCrumb
							storagename={storagename}
							path={path}
							navigate={this.handleOnClickANavigateToNewDest} />
						<HeaderItem
							reference={stat.reference}
							first_item_dateindex={first_item_dateindex}
							pathindexkey={md5(this.state.pathindex)}
							navigate={this.handleOnClickANavigateToNewDest} />
						<ItemContent
							reference={stat.reference}
							mtdsummary={stat.mtdsummary}
							navigate={this.handleOnClickANavigateToNewDest} />
						<NavigateTable
							stat={stat}
							navigate={this.handleOnClickANavigateToNewDest} />
						{display_pagination}
					</div>
				);
			}
		});

		React.render(
			<NavigatePage />,
			dom_target
		);
		
	};

})(window.mydmam);
