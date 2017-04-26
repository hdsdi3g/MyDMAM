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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/

dareport.eventlist_link = "dareport/eventlist";
var eventlist_link_future = dareport.eventlist_link + "/future";
var eventlist_link_past = dareport.eventlist_link + "/past";
var eventlist_link_add = dareport.eventlist_link + "/add";


/*
public static void eventnew(AJS_DAR_EventNew order) throws Exception {
public static void eventsendmail(AJS_DAR_EventName order) throws Exception {
public static void eventdelete(AJS_DAR_EventName order) throws Exception {
*/

var NewEvent = createReactClass({
	getInitialState: function() {
		return  {
			selected_date: new Date(),
		}
	},
	onChangeDate: function(new_date) {
		this.setState({selected_date: new_date});
	},
	onNothing: function(e) {
		e.preventDefault();
	},
	onAdd: function(e) {
		e.preventDefault();
		var request = {
			name: ReactDOM.findDOMNode(this.refs.event_name).value,
			planned_date: this.state.selected_date,
		};
		console.log("TODO", request);
	},
	render: function() {
		var FormControlGroup = mydmam.async.FormControlGroup;

		return (<form className="form-horizontal" onSubmit={this.onNothing}>
			<FormControlGroup label={i18n("dareport.event.name")}>
				<input type="text" className="span4" ref="event_name" />
			</FormControlGroup>
			<FormControlGroup label={i18n("dareport.event.date")}>
				<mydmam.async.Calendar onChange={this.onChangeDate} />
			</FormControlGroup>
			<FormControlGroup>
				<button className="btn btn-small btn-success" onClick={this.onAdd}><i className="icon-ok icon-white"></i> {i18n("dareport.event.create")}</button>
			</FormControlGroup>
		</form>);
	},
});

var EventListTable = createReactClass({
	render: function() {
		var items = this.props.items;
		var is_past = this.props.is_past;

		var table = (<table className="table table-striped table-hover table-bordered table-condensed">
    			<thead>
    				<tr>
    					<th>{i18n("")}</th>
    				</tr>
    			</thead>
    			<tbody>
    			</tbody>
    		</table>);

		return (table);
	},
});

dareport.EventList = createReactClass({
	getInitialState: function() {
		return {
			list: null,
			report_authors_by_event_name: {},
			usernames: {},
		};
	},
	componentWillMount: function() {
		mydmam.async.request("dareport", "eventlist", null, function(data) {
			this.setState({
				list: data.events,
				report_authors_by_event_name: data.report_authors_by_event_name,
				usernames: data.usernames,
			});
		}.bind(this));
	},
	render: function(){
		var show_this = null;

		if (this.state.list == null) {
			show_this = (<mydmam.async.PageLoadingProgressBar />);
		} else {
			var items = [];
			//TODO this.state.list > item...
			if (location.hash.indexOf("#" + eventlist_link_future) == 0) {
				show_this = (<EventListTable items={items} is_past={false} />);
			} else if (location.hash.indexOf("#" + eventlist_link_past) == 0) {
				show_this = (<EventListTable items={items} is_past={true} />);
			} else if (location.hash.indexOf("#" + eventlist_link_add) == 0) {
				show_this = (<NewEvent />);
			} else {
				show_this = (<EventListTable items={items} is_past={false} />);
			}
		}

		return (
			<mydmam.async.PageHeaderTitle title={i18n("dareport.eventlist.page")} fluid="true">
				<ul className="nav nav-tabs">
					<mydmam.async.HeaderTab href={"#" + eventlist_link_future}  i18nlabel="dareport.event.future" />
					<mydmam.async.HeaderTab href={"#" + eventlist_link_past} 	i18nlabel="dareport.event.past" />
					<mydmam.async.HeaderTab href={"#" + eventlist_link_add}     i18nlabel="dareport.event.add" />
				</ul>
				{show_this}
			</mydmam.async.PageHeaderTitle>
		);
	},
});

mydmam.routes.push("dareport-eventlist",       dareport.eventlist_link,  dareport.EventList, [{name: "dareport", verb: "eventlist"}]);
mydmam.routes.push("dareport-eventlistfuture", eventlist_link_future,    dareport.EventList, [{name: "dareport", verb: "eventlist"}]);
mydmam.routes.push("dareport-eventlistpast",   eventlist_link_past,      dareport.EventList, [{name: "dareport", verb: "eventlist"}]);
mydmam.routes.push("dareport-eventlistadd",    eventlist_link_add,       dareport.EventList, [{name: "dareport", verb: "eventlist"}]);
