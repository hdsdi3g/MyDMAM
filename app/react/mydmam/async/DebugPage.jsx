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

async.DebugPage = React.createClass({
	getInitialState: function() {
		return {
			ajscontrollers: null,
			version: null,
		};
	},
	componentWillMount: function() {
		mydmam.async.request("debugpage", "showcontrollers", {}, function(data) {
			this.setState({ajscontrollers: data.controllers, version: data.version});
		}.bind(this));
	},
	render: function() {
		var ajscontrollers = this.state.ajscontrollers;
		if (ajscontrollers == null) {
			return (<mydmam.async.PageLoadingProgressBar />);
		}


		var items = [];
		for (var controller_name in ajscontrollers) {
			var controller = ajscontrollers[controller_name];
			items.push(<tr key={"c:" + controller_name}>
				<td rowSpan={Object.keys(controller.verbs).length + 1}>
					<strong className="text-info">
						<async.JavaClassNameLink javaclass={controller.controller_class} version={this.state.version} />
					</strong>
				</td>
			</tr>);

			for (var verb_name in controller.verbs) {
				var verb = controller.verbs[verb_name];

				var privileges = [];
				for (var pos_privilege in verb.mandatory_privileges){
					privileges.push(verb.mandatory_privileges[pos_privilege]);
				}

				items.push(<tr key={"c:" + controller_name + ":v:" + verb_name}>
					<td>{verb_name}</td>
					<td><mydmam.async.JavaClassNameLink javaclass={verb.parameter_type} version={this.state.version} /></td>
					<td><mydmam.async.JavaClassNameLink javaclass={verb.return_type} version={this.state.version} /></td>
					<td className="muted">{privileges.join(" / ")}</td>
				</tr>);
			}
		}

		return (<mydmam.async.PageHeaderTitle title={i18n("ajscontroller.debugpage.title")} fluid="true">
			<table className="table table-striped table-hover table-condensed table-bordered">
				<thead>
					<th>{i18n("ajscontroller.debugpage.controller")}</th>
					<th>{i18n("ajscontroller.debugpage.verb")}</th>
					<th>{i18n("ajscontroller.debugpage.parametertype")}</th>
					<th>{i18n("ajscontroller.debugpage.returntype")}</th>
					<th>{i18n("ajscontroller.debugpage.privileges")}</th>
				</thead>
				<tbody>
					{items}
				</tbody>
			</table>
		</mydmam.async.PageHeaderTitle>);
	}
});

mydmam.routes.push("DebugPage", "debugpage", async.DebugPage, [{name: "debugpage", verb: "showcontrollers"}]);	
