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

manager.Items = React.createClass({
	getInitialState: function() {
		return {
			items: {},
			interval: null,
			user_has_change_checks: false,
			selected_instances: [],
			selected_item_classes: [],
		};
	},
	componentWillMount: function() {
		this.refresh();
	},
	refresh: function() {
		mydmam.async.request("instances", "allitems", null, function(items) {
			this.setState({
				items: items,
			});
			if (this.state.user_has_change_checks == false) {
				var selected_instances = [];
				for (var instance_key in items) {
					if (items[instance_key].length > 0) {
						if (items[instance_key][0].content) {
							if (items[instance_key][0].content.brokeralive) {
								selected_instances.push(instance_key);
							}
						}
					}
				}
				this.setState({
					selected_instances: selected_instances,
					selected_item_classes: this.getAllClassesNames(items),
				});
			}
		}.bind(this));
	},
	componentDidMount: function(){
		this.setState({interval: setInterval(this.refresh, 5000)});
	},
	componentWillUnmount: function() {
		if (this.state.interval) {
			clearInterval(this.state.interval);
		}
	},
	onSelectInstance: function(instance_ref, add) {
		if (instance_ref == "_all") {
			var all = [];
			if (add) {
				for (var instance_key in this.state.items) {
					all.push(instance_key);
				}
			}
			this.setState({
				selected_instances: all,
				user_has_change_checks: true,
			});
			return;
		}

		var actual = this.state.selected_instances.slice();
		if (actual.indexOf(instance_ref) == -1 && add) {
			actual.push(instance_ref);
			this.setState({
				selected_instances: actual,
				user_has_change_checks: true,
			});
		} else if (actual.indexOf(instance_ref) > -1 && (add == false)) {
			actual.splice(actual.indexOf(instance_ref), 1);
			this.setState({
				selected_instances: actual,
				user_has_change_checks: true,
			});
		}
	},
	onSelectItemClasses: function(class_name, add) {
		if (class_name == "_all") {
			var all = [];
			if (add) {
				all = this.getAllClassesNames();
			}
			this.setState({
				selected_item_classes: all,
				user_has_change_checks: true,
			});
			return;
		}

		var actual = this.state.selected_item_classes.slice();
		if (actual.indexOf(class_name) == -1 && add) {
			actual.push(class_name);
			this.setState({
				selected_item_classes: actual,
				user_has_change_checks: true,
			});
		} else if (actual.indexOf(class_name) > -1 && (add == false)) {
			actual.splice(actual.indexOf(class_name), 1);
			this.setState({
				selected_item_classes: actual,
				user_has_change_checks: true,
			});
		}
	},
	getAllClassesNames: function(all_items) {
		if (all_items == null) {
			all_items = this.state.items;
		}
		var item_classes = [];
		for (var instance_key in all_items) {
			var items = all_items[instance_key];
			for (var pos_items in items) {
				var item_class = items[pos_items]["class"];
				if (item_classes.indexOf(item_class) == -1) {
					item_classes.push(item_class);
				}
			}
		}
		return item_classes;
	},
	onGotoTheTop: function(e) {
		var absolute = React.findDOMNode(this.refs.items_container).getBoundingClientRect().y;
		e.preventDefault();
		$("html, body").scrollTop($("html, body").scrollTop() + absolute - 50);
	},
	onGoToItemBlock: function(reference) {
		var absolute = React.findDOMNode(this.refs[reference]).getBoundingClientRect().y;
		$("html, body").scrollTop($("html, body").scrollTop() + absolute - 50);
	},
	render: function() {
		/**
		 * Left panel
		 */
		var display_instance_list = [];
		for (var instance_key in this.state.items) {
			var items = this.state.items[instance_key];
			var summary = this.props.summaries[instance_key];
			var checked = this.state.selected_instances.indexOf(instance_key) > -1;
			var label = instance_key;
			if (summary != null) {
				label = (<span>{summary.instance_name} <small>({summary.app_name})</small></span>);
			}
			display_instance_list.push(<manager.SelectNavItemInstance key={instance_key} onClick={this.onSelectInstance} reference={instance_key} checked={checked}>
				{label}
			</manager.SelectNavItemInstance>);
		}

		if (display_instance_list.length > 1) {
			var checked = this.state.selected_instances.length == display_instance_list.length;
			display_instance_list.splice(0, 0, (<manager.SelectNavItemInstance key="_all" onClick={this.onSelectInstance} reference="_all" checked={checked}>
				<em>{i18n("manager.items.chooseall")}</em>
			</manager.SelectNavItemInstance>));
		}

		var display_item_classes_list = [];
		var item_classes = this.getAllClassesNames();
		for (var pos in item_classes) {
			var classname = item_classes[pos];
			var checked = this.state.selected_item_classes.indexOf(classname) > -1;
			display_item_classes_list.push(<manager.SelectNavItemInstance key={pos} onClick={this.onSelectItemClasses} reference={classname} checked={checked}>
				{classname}
			</manager.SelectNavItemInstance>);
		}


		if (display_item_classes_list.length > 1) {
			var checked = this.state.selected_item_classes.length == display_item_classes_list.length;
			display_item_classes_list.splice(0, 0, (<manager.SelectNavItemInstance key="_all" onClick={this.onSelectItemClasses} reference="_all" checked={checked}>
				<em>{i18n("manager.items.chooseall")}</em>
			</manager.SelectNavItemInstance>));
		}

		/**
		 * Items
		 */
		var display_items = [];
		var summary_table_items = [];
		for (var instance_key in this.state.items) {
			if (this.state.selected_instances.indexOf(instance_key) == -1) {
				continue;
			}
			var items = this.state.items[instance_key];

			/**
			 * Display title
			 */
			var summary = this.props.summaries[instance_key];
			var summary_td_table = (<span>{instance_key}</span>);
			if (summary != null) {
				display_items.push(<h3 key={instance_key + "-title"} style={{marginBottom: 6}}>
					{summary.instance_name}&nbsp;
					<small>
						&bull; {summary.app_name} &bull; {summary.pid}@{summary.host_name}
					</small>
				</h3>);
				summary_td_table = (<span>
					{summary.instance_name}&nbsp;
					<small className="muted">
						({summary.app_name})
					</small>
				</span>);
			} else {
				display_items.push(<h3 key={instance_key + "-title"} style={{marginBottom: 6}}>
					{instance_key}
				</h3>);
			}

			for (var pos_items in items) {
				var json_item = items[pos_items];
				var item_class = json_item["class"];
				if (this.state.selected_item_classes.indexOf(item_class) == -1) {
					continue;
				}
				var item = mydmam.module.f.managerInstancesItems(json_item);
				if (item == null) {
					/**
					 * Display default view: raw json
					 */
					item = (<div>
						<code className="json" style={{marginTop: 10}}>
							<i className="icon-indent-left"></i>
							<span className="jsontitle"> {json_item["class"]} </span>
							{JSON.stringify(json_item.content, null, " ")}
						</code>
					</div>);
				}

				/**
				 * Add view in list.
				 */
				var ref = md5(instance_key + " " + pos_items);
				display_items.push(<div key={ref} ref={ref} style={{marginBottom: 26, marginLeft: 10}}>
					<div className="pull-right">{mydmam.async.broker.displayKey(json_item.key, true)}</div>
					<h4>
						<a href={location.hash} onClick={this.onGotoTheTop}><i className=" icon-arrow-up" style={{marginRight: 5, marginTop: 5}}></i></a>
						{item_class}
					</h4>
					<div className="instance-item-block">
						{item}
					</div>
				</div>);

				/**
				 * Add line to summary table
				 */
				var descr = mydmam.module.f.managerInstancesItemsDescr(json_item);
				if (descr == null) {
					descr = (<em>{i18n("manager.items.summarytable.descr.noset")}</em>);
				}
				summary_table_items.push(<tr key={ref}>
					<td>
						{summary_td_table}
					</td>
					<td>
						{item_class}
					</td>
					<td>
						<manager.btnArrowGoToItemBlock onGoToItemBlock={this.onGoToItemBlock} reference={ref}>
							<i className="icon-arrow-down" style={{marginTop: 2}}></i>&nbsp;
							{descr}
						</manager.btnArrowGoToItemBlock>
					</td>
				</tr>);
			}
			display_items.push(<hr key={instance_key + "-hr"} style={{marginBottom: 10}} />);
		}

		var table_summary_items = null;
		if (summary_table_items.length > 0) {
			table_summary_items = (<div>
				<table className="table table-bordered table-striped table-condensed table-hover">
					<thead>
						<tr>
							<th>{i18n("manager.items.summarytable.instance")}</th>
							<th>{i18n("manager.items.summarytable.item")}</th>
							<th>{i18n("manager.items.summarytable.descr")}</th>
						</tr>
					</thead>
					<tbody>
						{summary_table_items}
					</tbody>
				</table>
				<hr />
			</div>);
		}

		return (<div className="row-fluid">
			<div className="span3">
			 	<div className="well instancesnavlists">
				 	<pre>
	 					<strong>{i18n("manager.items.chooseinstancelist")}</strong>
				    	{display_instance_list}
					</pre>
						<hr />
				 	<pre>
	 					<strong>{i18n("manager.items.chooseitemlist")}</strong>
				    	{display_item_classes_list}
					</pre>
			    </div>
			</div>
			<div className="span9" style={{marginLeft: 15}} ref="items_container">
				{table_summary_items}
				{display_items}
			</div>
		</div>);
	},
});

manager.btnArrowGoToItemBlock = React.createClass({
	onGoto: function (e) {
		e.preventDefault();
		$(React.findDOMNode(this.refs.a)).blur();
		this.props.onGoToItemBlock(this.props.reference);
	},
 	render: function() {
 		return (<a href={location.hash} onClick={this.onGoto} ref="a" style={{color: "inherit"}}>
 			{this.props.children}
 		</a>);
	},
});

manager.SelectNavItemInstance = React.createClass({
	onClick: function (e) {
		e.preventDefault();
		$(React.findDOMNode(this.refs.a)).blur();
		this.props.onClick(this.props.reference, ! this.props.checked);
	},
 	render: function() {
 		return (<div>
 			<a href={location.hash} onClick={this.onClick} ref="a">
	 			{this.props.checked ? "[X]" : "[ ]"} {this.props.children}
	 		</a>
	 	</div>);
	},
});

mydmam.module.register("AppManager", {
	managerInstancesItems: function(item) {
		if (item["class"] != "AppManager") {
			return null;
		}
		return (<div>
			<mydmam.async.LabelBoolean label_true={i18n("manager.items.AppManager.broker.on")} label_false={i18n("manager.items.AppManager.broker.off")} value={item.content.brokeralive} inverse={true} />&nbsp;
			<mydmam.async.LabelBoolean label_true={i18n("manager.items.AppManager.inoffhours")} label_false={i18n("manager.items.AppManager.innormalhours")} value={item.content.is_off_hours} />&nbsp;
			<mydmam.async.pathindex.reactDate date={item.content.next_updater_refresh_date} i18nlabel={i18n("manager.items.AppManager.next_updater_refresh_date")} style={{marginLeft: 0}} />
		</div>);
	},
	managerInstancesItemsDescr: function(item) {
		if (item["class"] != "AppManager") {
			return null;
		}
		return i18n("manager.items.AppManager.descr");
	},
});

mydmam.module.register("CyclicJobCreator", {
	managerInstancesItems: function(item) {
		if (item["class"] != "CyclicJobCreator") {
			return null;
		}
		var content = item.content;

		var declaration_list = [];
		for (var pos in content.declarations) {
			var declaration = content.declarations[pos];
			
			var context_list = [];
			for (var pos_ctx in declaration.contexts) {
				var context = declaration.contexts[pos_ctx];
				context_list.push(<div key={pos_ctx} style={{marginLeft: 10}}>
					{mydmam.async.broker.displayContext(context)}
				</div>);
			}
			declaration_list.push(<div key={pos} style={{marginLeft: 12}}>
				<strong>&bull; {declaration.job_name}</strong><br />
				{context_list}
			</div>);
		}

		return (<div>
			<strong>{content.long_name} :: {content.vendor_name}</strong><br />
			<mydmam.async.LabelBoolean label_true={i18n("manager.items.CyclicJobCreator.enabled")} label_false={i18n("manager.items.CyclicJobCreator.disabled")} value={content.enabled} inverse={true} />&nbsp;
			<mydmam.async.pathindex.reactDate date={content.next_date_to_create_jobs} i18nlabel={i18n("manager.items.CyclicJobCreator.next_date_to_create_jobs")} style={{marginLeft: 0}} />&nbsp;
			<mydmam.async.LabelBoolean label_true={i18n("manager.items.CyclicJobCreator.onlyoff")} label_false={i18n("manager.items.CyclicJobCreator.norestricted")} value={content.only_off_hours} />&nbsp;
			<br />
			{i18n("manager.items.CyclicJobCreator.period", content.period / 1000)}
			<br />
			{i18n("manager.items.CyclicJobCreator.creator")} <mydmam.async.JavaClassNameLink javaclass={content.creator} />
			<div style={{marginTop: 16}}>
				<i className="icon-th-list"></i> {i18n("manager.items.CyclicJobCreator.declarations")}<br />
				{declaration_list}
			</div>
		</div>);
	},
	managerInstancesItemsDescr: function(item) {
		if (item["class"] != "CyclicJobCreator") {
			return null;
		}
		var content = item.content;

		var declaration_list = [];
		for (var pos in content.declarations) {
			var declaration = content.declarations[pos];
			declaration_list.push(declaration.job_name);
		}
		
		return declaration_list.join(", ");
	},
});

mydmam.module.register("WorkerNG", {
	managerInstancesItems: function(item) {
		if (item["class"] != "WorkerNG") {
			return null;
		}
		var content = item.content;

		var current_job_key = null;
		if (content.current_job_key != null) {
			current_job_key = mydmam.async.broker.displayKey(content.current_job_key, true);
		}

		var capablities_list = [];
		for (var pos in content.capablities) {
			var declaration = content.capablities[pos];
			capablities_list.push(<div key={pos} style={{marginLeft: 16}}>
				{mydmam.async.broker.displayContext(declaration)}
			</div>);
		}

		var specific = null;
		if (content.specific != null) {
			specific = (<div style={{marginTop: 10}}>
				<mydmam.async.JsonCode i18nlabel="manager.items.worker.specific" json={content.specific} />
			</div>);
		}

		return (<div>
			<strong>{content.long_name} :: {content.vendor}</strong> ({content.category}) <mydmam.async.JavaClassNameLink javaclass={content.worker_class} /><br />
			<span className="badge badge-info">{content.state}</span> {current_job_key}<br />
			<div style={{marginTop: 16}}>
				<i className="icon-th-list"></i> {i18n("manager.items.worker.capablities")}<br />
				{capablities_list}
			</div>
			{specific}
		</div>);

	},
	managerInstancesItemsDescr: function(item) {
		if (item["class"] != "WorkerNG") {
			return null;
		}
		return item.content.long_name;
	},
});

