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

		var BreadCrumb = React.createClass({
			render: function() {
				//TODO BreadCrumb
				/*
				storagename={storagename} path={path}

				navigator.createBreadcrumb = function(storagename, path) {
					var domid = "storageelem";
					if (storagename == null) {
						$("#" + domid).prepend('<ul class="breadcrumb"><li class="active">' + i18n('browser.storagestitle') + '</li></ul>');
						return;
					}
					var element_subpaths = path.split("/");
					var content = "";
					var currentpath = "";
					var newpath = "";
					for (var pos = 1; pos < element_subpaths.length; pos++) {
						newpath = storagename + ':' + currentpath + "/" + element_subpaths[pos];
						if (pos + 1 < element_subpaths.length) {
							content = content + '<li><span class="divider">/</span><a href="' + url_navigate + "#" + newpath + '">' + element_subpaths[pos] + '</a></li>';
						} else {
							content = content + '<li class="active"><span class="divider">/</span>' + element_subpaths[pos] + '</li>';
						}
						currentpath = currentpath + "/" + element_subpaths[pos];
					}
					if (content !== "") {
						var header = '<li><a href="' + url_navigate + '#">' + i18n('browser.storagestitle') + '</a> <span class="divider">::</span></li>';
						if (path != "/") {
							header = header + '<li><a href="' + url_navigate + "#" + storagename + ':/">' + storagename + '</a></li>';
						} else {
							header = header + '<li class="active">' + storagename + '</li>';
						}
						content = '<ul class="breadcrumb">' + header + content + '</ul>';
						$("#" + domid).prepend(content);
						$("#" + domid + " .breadcrumb a").click(function() {
							mydmam.navigator.displayStoragePathNavigator("storageelem", $(this).context.hash.substring(1), true);
						});
					}
				};
				*/
				return (
					<div>breadcrumb</div>
				);
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
					var url_goback = url_navigate + "#" + reference.storagename + ":" + reference.path.substring(0, reference.path.lastIndexOf("/"));
					if (reference.path == '/') {
						url_goback = url_navigate + "#";
					} else if (reference.path.lastIndexOf("/") === 0) {
						url_goback = url_navigate + "#" + reference.storagename + ":/";
					}
					var go_back = (
						<a className="btn btn-mini btngoback" style={{marginBottom: "6px", marginRight: "1em"}} href={url_goback} title={i18n('browser.goback')}>
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

		var ItemContent = React.createClass({
			/*
			if (stat.mtdsummary) {
				content = content + '<div>';
				content = content + mydmam.metadatas.display(stat.reference, stat.mtdsummary, mydmam.metadatas.displaymethod.NAVIGATE_SHOW_ELEMENT);
				content = content + '</div>';
			}
			*/
			//TODO display metadatas in place
			render: function() {
				return (
					<div>itemcontent</div>
				);
			}
		});

		var NavigateTable = React.createClass({
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

				/*dircontent = dircontent.sort(function(a, b) {
					if (a.directory & (b.directory === false)) {
						return -1;
					}
					if (b.directory & (a.directory === false)) {
						return 1;
					}
					return a.idxfilename < b.idxfilename ? -1 : 1;
				});*/
				//console.log(dircontent);

				var thead = null;
				if (reference.storagename) {
					thead = (<thead><tr><td>&nbsp;</td><td></td><td></td><td></td><td></td></tr></thead>);
				} else {
					thead = (<thead><tr><td>{i18n("browser.storagelist")}</td> <td></td> <td></td> <td></td> <td></td></tr></thead>);
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
								<a className="tlbdirlistitem" href={url_navigate + "#" + element.storagename + ":" + element.path}>
									{element.path.substring(element.path.lastIndexOf("/") + 1)}
								</a>
							);
						} else {
							name = (
								<a className="tlbdirlistitem" href={url_navigate + "#" + element.storagename + ':/'}>
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
								<a className="tlbdirlistitem" href={url_navigate + "#" + element.storagename + ":" + element.path}>
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

				/*var href = function(currentpage, pagecount) {
					return '#' + fullpath;
				};
				content = content + mydmam.pagination.create(items_page_from, Math.ceil(items_total / items_page_size), href, "navigator-" + md5_fullpath);

				$("#" + domid).empty();
				$("#" + domid).append(content);

				$('.navdatatable').dataTable({
					"bPaginate": false,
					"bLengthChange": false,
					"bSort": true,
					"bInfo": false,
					"bAutoWidth": false,
					"bFilter": true,
					"aoColumnDefs": [{
						"iDataSort": 2,
						"aTargets": [1],
						"bSearchable": false
					}, // SIZE displayed
					{
						"bVisible": false,
						"bSearchable": false,
						"aTargets": [2]
					}, // SIZE raw
					{
						"iDataSort": 4,
						"aTargets": [3],
						"bSearchable": false
					}, // DATE displayed
					{
						"bVisible": false,
						"bSearchable": false,
						"aTargets": [4]
					} // DATE raw
					]
				});

				$('#sitesearch').bind('keyup.DT', function(e) {
					var val = this.value === "" ? "" : this.value;
					$('.dataTables_filter input').val(val);
					$('.dataTables_filter input').trigger("keyup.DT");
				});

				mydmam.pagination.addevents(function(currentpage) {
					return function() {
						mydmam.navigator.displayStoragePathNavigator("storageelem", fullpath, true, currentpage);
					};
				}, "navigator-" + md5_fullpath);
				*/
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
					pathindexkey: md5(pathindex_destination),
				};
			},
			componentDidMount: function() {
				var stat = mydmam.stat;

				var stat_request = {
					pathelementskeys: [md5(pathindex_destination)],
					scopes_element: [stat.SCOPE_DIRLIST, stat.SCOPE_PATHINFO, stat.SCOPE_MTD_SUMMARY, stat.SCOPE_COUNT_ITEMS],
					scopes_subelements: [stat.SCOPE_MTD_SUMMARY, stat.SCOPE_COUNT_ITEMS],
					page_from: 0,
					page_size: 20,
					search: JSON.stringify(''),
				};

				document.body.style.cursor = 'wait';
				mydmam.async.request("stat", "cache", stat_request, function(data) {
					document.body.style.cursor = 'default';
					if (!data[this.state.pathindexkey]) {
						this.setState({
							stat: {},
							pathindexkey: md5(''),
						});
						return;
					}
					this.setState({
						stat: data,
					});
				}.bind(this));
			},
			handlePaginationSwitchPage: function(e) {
				console.log(e);
				//TODO change page...
				return null;
			},
			render: function() {
				var stat = this.state.stat[this.state.pathindexkey];
				if (!stat) {
					return (<div>{i18n('browser.emptydir')}</div>);
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
				var Pagination = mydmam.async.pagination.reactBlock;

				return (
					<div>
						<BreadCrumb storagename={storagename} path={path} />
						<HeaderItem reference={stat.reference} first_item_dateindex={first_item_dateindex} pathindexkey={this.state.pathindexkey} />
						<ItemContent reference={stat.reference} mtdsummary={stat.mtdsummary} />
						<NavigateTable stat={stat} />
						<Pagination
							pagecount={Math.ceil(stat.items_total / stat.items_page_size)}
							currentpage={stat.items_page_from}
							onClickButton={this.handlePaginationSwitchPage} />
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
