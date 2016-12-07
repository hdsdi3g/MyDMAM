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

	React.render(
		<routes.Backbone rlite={rlite} />,
		dom_target
	);
};

routes.Backbone = React.createClass({
	getInitialState: function() {
		return {dest: null, params: null};
	},
  	processHash: function() {
		var hash = location.hash || '#';
		this.props.rlite.run(hash.slice(1));
	},
	onChangePage: function(route_name, params) {
		this.setState({dest: route_name, params: params});
	},
  	componentWillMount: function() {
  		var r = this.props.rlite;
  		
		r.add('', function () {
			this.setState({dest: null, params: null});
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
	render: function() {
		var main = null;

		var search = null;
		if (mydmam.async.isAvaliable("search", "query")) {
			search = (<mydmam.async.SearchBox onValidation={this.doDirectSearch} />);
		}

		if (this.state.dest) {
			var ReactTopLevelClass = routes.getReactTopLevelClassByRouteName(this.state.dest);
			if (ReactTopLevelClass) {
				return (<div>
					<ReactTopLevelClass params={this.state.params} />
					{search}
				</div>);
			}
		}

		return (<div>
			<mydmam.async.Home />
			{search}
		</div>);
	}
});
