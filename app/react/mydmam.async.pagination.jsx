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
	mydmam.async.pagination = {};
	var pagination = mydmam.async.pagination;

	var PageButton = React.createClass({
		handleClick: function(e) {
			e.preventDefault();
			this.props.onClickButton(this.props.num);
		},
		render: function() {
			if (this.props.currentpage === this.props.num) {
			    return (<li className="active"><span>{this.props.num}</span></li>);
			}

		    return (<li>
			    	<a href={this.props.linkHref} onClick={this.handleClick}>{this.props.num}</a>
				</li>);
		}
	});

	var PagePreviousNext = React.createClass({
		handleClick: function(e) {
			e.preventDefault();
			this.props.onClickButton(this.props.num);
		},
		render: function() {
			if (this.props.direction === "previous") {
			    return (<li>
				    	<a href={this.props.linkHref} onClick={this.handleClick}>&laquo; {i18n("search.previous")}</a>
					</li>);
			} else {
			    return (<li>
				    	<a href={this.props.linkHref} onClick={this.handleClick}>{i18n("search.next")} &raquo;</a>
					</li>);
			}
		}
	});

	var PageSpacer = React.createClass({
		render: function() {
			return (<li className="active"><span>&hellip;</span></li>);
		}
	});

	pagination.reactBlock = React.createClass({
		onlinkTargeter: function(new_pos) {
			if (this.props.onlinkTargeter) {
				return this.props.onlinkTargeter(new_pos);
			} else {
				return "";
			}
		},
		render: function() {
			var pagecount = this.props.pagecount;
			if (pagecount < 2) {
				return (<div className="pagination pagination-centered pagination-large"></div>);
			}

			var currentpage = this.props.currentpage;

			var list = [];
			var pageSelected;
			if (currentpage > 1 && pagecount > 2) {
				list.push(<PagePreviousNext key="previous" direction="previous" linkHref={this.onlinkTargeter(currentpage - 1)} num={currentpage - 1} onClickButton={this.props.onClickButton} />);
			}

			if (pagecount < 6) {
				for (var i = 1; i < pagecount + 1; i++) {
					list.push(<PageButton key={i} num={i} linkHref={this.onlinkTargeter(i)} currentpage={currentpage} onClickButton={this.props.onClickButton} />);
				}
			} else {
				/*
				 light_side
				   |-|    
				[<][ ]...[ ][ ][ ][ ][ ]{!}[ ][ ][>]
				         |-----strong_side------|
				         
			    light_side    center_side     light_side
                   |-|   |-------| & |-------|   |-|
				[<][ ]...[ ][ ][ ]{!}[ ][ ][ ]...[ ][>]
				*/
				var light_side = 1;
				var strong_side = 8;
				var center_side = 3;

				if (currentpage <= 6) {
					for (var i = 1; i <= strong_side; i++) {
						/** strong_side */
						list.push(<PageButton key={i} num={i} linkHref={this.onlinkTargeter(i)} currentpage={currentpage} onClickButton={this.props.onClickButton} />);
					}
					list.push(<PageSpacer key="spc" />);
					for (var i = pagecount - (light_side - 1); i <= pagecount; i++) {
						/** light_side */
						list.push(<PageButton key={i} num={i} linkHref={this.onlinkTargeter(i)} currentpage={currentpage} onClickButton={this.props.onClickButton} />);
					}
				} else if (currentpage >= (pagecount - 5)) {
					for (var i = 1; i <= light_side; i++) {
						/** light_side */
						list.push(<PageButton key={i} num={i} linkHref={this.onlinkTargeter(i)} currentpage={currentpage} onClickButton={this.props.onClickButton} />);
					}
					list.push(<PageSpacer key="spc" />);
					for (var i = pagecount - (strong_side - 1); i <= pagecount; i++) {
						/** strong_side */
						list.push(<PageButton key={i} num={i} linkHref={this.onlinkTargeter(i)} currentpage={currentpage} onClickButton={this.props.onClickButton} />);
					}
				} else {
					for (var i = 1; i <= light_side; i++) {
						/** light_side */
						list.push(<PageButton key={i} num={i} linkHref={this.onlinkTargeter(i)} currentpage={currentpage} onClickButton={this.props.onClickButton} />);
					}
					list.push(<PageSpacer key="spc1" />);
					for (var i = currentpage - center_side; i <= currentpage + center_side; i++) {
						/** center_side */
						list.push(<PageButton key={i} num={i} linkHref={this.onlinkTargeter(i)} currentpage={currentpage} onClickButton={this.props.onClickButton} />);
					}
					list.push(<PageSpacer key="spc2" />);
					for (var i = pagecount - (light_side - 1); i <= pagecount; i++) {
						/** light_side */
						list.push(<PageButton key={i} num={i} linkHref={this.onlinkTargeter(i)} currentpage={currentpage} onClickButton={this.props.onClickButton} />);
					}
				}
			}

			if (currentpage < pagecount && pagecount > 2) {
				list.push(<PagePreviousNext key="next" direction="next" linkHref={this.onlinkTargeter(currentpage + 1)} num={currentpage + 1} onClickButton={this.props.onClickButton} />);
			}
		    return (<div className="pagination pagination-centered pagination-large"><ul>{list}</ul></div>);
		}
	});
})(window.mydmam);
