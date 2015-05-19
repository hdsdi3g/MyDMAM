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

pathindex.reactBasketButton = React.createClass({
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
		if (this.props.pathindexkey === md5('')) {
			return null;
		}
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