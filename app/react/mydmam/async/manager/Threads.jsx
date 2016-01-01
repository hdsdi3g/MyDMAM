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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/

manager.Threads = React.createClass({
	getInitialState: function() {
		return {
			thread_list: {},
			selected_instance: null,
		};
	},
	componentWillMount: function() {
		mydmam.async.request("instances", "allthreads", null, function(list) {
			this.setState({thread_list: list});
		}.bind(this));
	},
	onSelectInstance: function(ref) {
		this.setState({selected_instance: ref});
		$("html, body").scrollTop(0);
	},
	onSelectThread: function(thread_pos) {
		var absolute = React.findDOMNode(this.refs[thread_pos]).getBoundingClientRect().y;
		$("html, body").scrollTop($("html, body").scrollTop() + absolute - 50);
	},
	onGotoTheTop: function(e) {
		e.preventDefault();
		$("html, body").scrollTop(0);
	},
	render: function() {
		if (this.state.selected_instance == null) {
			return (<manager.InstancesNavList onSelectInstance={this.onSelectInstance} />);
		}
		var current_threads = this.state.thread_list[this.state.selected_instance];
		if (current_threads == null) {
			return (<manager.InstancesNavList onSelectInstance={this.onSelectInstance} />);
		}

		current_threads.sort(function(a, b) {
			return a.id > b.id;
		});

		var items = [];
		var thread_names = [];
		for (var pos in current_threads) {
			var thread = current_threads[pos];
			var daemon = null;
			if (thread.isdaemon) {
				daemon = (<span className="badge">DAEMON</span>);
			}

			var stacktrace = [];
			if (thread.execpoint == "") {
				stacktrace.push(<div key="-1">{thread.classname}</div>);
			} else {
				var execpoints = thread.execpoint.split("\n");
				for (var pos_execpoint in execpoints) {
					stacktrace.push(<div key={pos_execpoint}>{execpoints[pos_execpoint]}</div>);
				}
			}

			items.push(<div key={pos} ref={pos}>
				<h4>
					<a href={location.hash} onClick={this.onGotoTheTop}><i className=" icon-arrow-up" style={{marginRight: 5, marginTop: 5}}></i></a>
					{thread.name}
				</h4>
				<span className="badge badge-inverse">#{thread.id}</span> <span className="label label-info">{thread.state}</span> {daemon}<br />
				<div className="thread-stacktrace">
					{stacktrace}
				</div>
			</div>);

			thread_names.push(<span>{thread.id} &bull; {thread.name.substring(0, 50)}</span>);
		}

		return (<manager.InstancesNavList
				onSelectInstance={this.onSelectInstance}
				onSelectItem={this.onSelectThread}
				items={thread_names}
				item_list_i18n_title="manager.threads">
			{items}
		</manager.InstancesNavList>);
	},
});
