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
/*jshint eqnull:true, loopfunc:true, shadow:true, jquery:true */

routes.loadBackbone = function(dom_target) {
	if (!dom_target) {
		return;
	}

	var rlite = Rlite();

	ReactDOM.render(
		<routes.Backbone rlite={rlite} />,
		dom_target
	);
};

routes.Backbone = React.createClass({
	getInitialState: function() {
		return {dest: null, params: null, q: null, };
	},
  	processHash: function() {
		var hash = location.hash || '#';
		this.props.rlite.run(hash.slice(1));
	},
	onChangePage: function(route_name, params) {
		this.setState({dest: route_name, params: params, q: null});
	},
  	componentWillMount: function() {
  		var r = this.props.rlite;
  		
		r.add('', function () {
			this.setState({dest: null, params: null, q: null});
		}.bind(this));

		routes.populate(r, this.onChangePage);
		/*r.add('var/:content', function (r) {
			this.setState({dest: r.params.content});
		}.bind(this));*/

		this.processHash();
  	},
   	componentDidMount: function() {
  		window.addEventListener('hashchange', this.processHash);
   	},
 	componentWillUnmount: function() {
  		window.removeEventListener('hashchange', this.processHash);
  	},
  	doDirectSearch: function(q) {
  		location.hash = "#" + mydmam.async.search.urlify(q, 0);
	},
	onDirectSearch: function(q) {
		if (q == '') {
			q = null;
		}

		if (routes.canHandleSearch(this.state.dest)) {
			this.setState({q: q});
		} else if (q != null) {
			this.doDirectSearch(q);
		}
	},
	render: function() {
		var main = null;

		var display_search_inputbox = mydmam.async.isAvaliable("search", "query");

		if (this.state.dest) {
			var ReactTopLevelClass = routes.getReactTopLevelClassByRouteName(this.state.dest);

			display_search_inputbox = display_search_inputbox | routes.canHandleSearch(this.state.dest);
		
			if (ReactTopLevelClass) {
				return (<div>
					<mydmam.async.TopMenu onDirectSearch={this.onDirectSearch} display_search_inputbox={display_search_inputbox} />
					<ReactTopLevelClass params={this.state.params} q={this.state.q} />
					<mydmam.async.Footer />
				</div>);
			}
		}

		return (<div>
			<mydmam.async.TopMenu onDirectSearch={this.doDirectSearch} display_search_inputbox={display_search_inputbox} />
			<mydmam.async.Home />
			<mydmam.async.Footer />
		</div>);
	}
});
