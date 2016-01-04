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

manager.Items = React.createClass({
	getInitialState: function() {
		return {
			items: {},
			interval: null,
			selected_instances: [],
			selected_item_classes: [],
		};
	},
	componentWillMount: function() {
		this.refresh(true);
	},
	refresh: function(select_all) {
		mydmam.async.request("instances", "allitems", null, function(items) {
			var selected_instances = this.state.selected_instances;
			if (select_all) {
				selected_instances = [];
				for (var instance_key in items) {
					selected_instances.push(instance_key);
				}
				selected_item_classes = this.getAllClassesNames(items);
			}
			this.setState({
				items: items,
				selected_instances: selected_instances,
				selected_item_classes: selected_item_classes,
			});
		}.bind(this));
	},
	componentDidMount: function(){
		//this.setState({interval: setInterval(this.refresh, 10000)});
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
			this.setState({selected_instances: all});
			return;
		}

		var actual = this.state.selected_instances.slice();
		if (actual.indexOf(instance_ref) == -1 && add) {
			actual.push(instance_ref);
			this.setState({selected_instances: actual});
		} else if (actual.indexOf(instance_ref) > -1 && (add == false)) {
			actual.splice(actual.indexOf(instance_ref), 1);
			this.setState({selected_instances: actual});
		}
	},
	onSelectItemClasses: function(class_name, add) {
		if (class_name == "_all") {
			var all = [];
			if (add) {
				all = this.getAllClassesNames();
			}
			this.setState({selected_item_classes: all});
			return;
		}

		var actual = this.state.selected_item_classes.slice();
		if (actual.indexOf(class_name) == -1 && add) {
			actual.push(class_name);
			this.setState({selected_item_classes: actual});
		} else if (actual.indexOf(class_name) > -1 && (add == false)) {
			actual.splice(actual.indexOf(class_name), 1);
			this.setState({selected_item_classes: actual});
		}
	},
	getAllClassesNames: function(items) {
		if (items == null) {
			items = this.state.items;
		}
		var item_classes = [];
		for (var instance_key in items) {
			var items = items[instance_key];
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
		e.preventDefault();
		$("html, body").scrollTop(0);
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
				ALL
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
				ALL
			</manager.SelectNavItemInstance>));
		}

		/**
		 * Items
		 */
		var display_items = [];
		for (var instance_key in this.state.items) {
			if (this.state.selected_instances.indexOf(instance_key) == -1) {
				continue;
			}
			var items = this.state.items[instance_key];

			/**
			 * Display title
			 */
			var summary = this.props.summaries[instance_key];
			if (summary != null) {
				display_items.push(<h3 key={instance_key + "-title"} style={{marginBottom: 6}}>
					{summary.instance_name}&nbsp;
					<small>
						&bull; {summary.app_name} &bull; {summary.pid}@{summary.host_name}
					</small>
				</h3>);
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
				display_items.push(<div key={md5(instance_key + " " + pos_items)} style={{marginBottom: 26, marginLeft: 10}}>
					<div className="pull-right">{mydmam.async.broker.displayKey(json_item.key, true)}</div>
					<h4>
						<a href={location.hash} onClick={this.onGotoTheTop}><i className=" icon-arrow-up" style={{marginRight: 5, marginTop: 5}}></i></a>
						{item_class}
					</h4>
					<div className="instance-item-block">
						{item}
					</div>
				</div>);
			}
			display_items.push(<hr key={instance_key + "-hr"} style={{marginBottom: 10}} />);
		}

		return (<div className="row-fluid">
			<div className="span3">
			 	<div className="well" style={{padding: 8}}>
				    {display_instance_list}
				    <hr />
				    {display_item_classes_list}
			    </div>
			</div>
			<div className="span9" style={{marginLeft: 15}}>
				{display_items}
			</div>
		</div>);
	},
});

manager.SelectNavItemInstance = React.createClass({
	getInitialState: function() {
		return {
			checked: this.props.checked,
		};
	},
	componentWillReceiveProps: function(nextProps) {
		this.setState({checked: nextProps.checked});
	},
	onClick: function (e) {
		e.preventDefault();
		this.props.onClick(this.props.reference, ! this.state.checked);
		this.setState({checked: ! this.state.checked});
	},
 	render: function() {

 		return (<div>
 			<label className="checkbox" onClick={this.onClick} >
				<input type="checkbox" ref="cb" checked={this.state.checked} onChange={this.onClick} /> {this.props.children}
			</label>
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
	}
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
	}
});

//TODO WORKER !
