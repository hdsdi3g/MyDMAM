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
			selected_time_hrs: null,
			selected_time_min: null,
			event_name: null,
		}
	},
	onChangeName: function(e) {
		var value = ReactDOM.findDOMNode(this.refs.event_name).value.trim();
		if (value == "") {
			this.setState({event_name: null});
		} else {
			this.setState({event_name: value});
		}
	},
	onChangeDate: function(new_date) {
		this.setState({selected_date: new_date});
	},
	onChangeTime: function(hrs, min) {
		this.setState({selected_time_hrs: hrs, selected_time_min: min});
	},
	onAdd: function(e) {
		e.preventDefault();
		var date = new Date(this.state.selected_date.getTime());
		date.setHours(this.state.selected_time_hrs, this.state.selected_time_min, 0, 0);

		var request = {
			name: this.state.event_name,
			planned_date: date.getTime(),
		};

		mydmam.async.request("dareport", "eventnew", request, function(data) {
			this.props.onAddNewEventGetNewerList(data);
		}.bind(this));
	},
	render: function() {
		var FormControlGroup = mydmam.async.FormControlGroup;
		var btn_send = null;
		if (this.state.selected_time_hrs != null && this.state.selected_time_min != null && this.state.event_name != null) {
			btn_send = (<button className="btn btn-small btn-success" onClick={this.onAdd}>
				<i className="icon-ok icon-white"></i> {i18n("dareport.event.create")}
			</button>);
		}

		return (<form className="form-horizontal">
			<FormControlGroup label={i18n("dareport.event.name")}>
				<input type="text" className="span4" ref="event_name" onChange={this.onChangeName} />
			</FormControlGroup>
			<FormControlGroup label={i18n("dareport.event.date")}>
				<mydmam.async.Calendar onChange={this.onChangeDate} />
			</FormControlGroup>
			<FormControlGroup label={i18n("dareport.event.hour")}>
				<mydmam.async.HourMinInputbox onChange={this.onChangeTime} />
			</FormControlGroup>
			<FormControlGroup>
				{btn_send}
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
		mydmam.async.request("dareport", "eventlist", null, this.importEventList);
	},
	importEventList: function(new_list) {
		this.setState({
			list: new_list.events,
			report_authors_by_event_name: new_list.report_authors_by_event_name,
			usernames: new_list.usernames,
		});
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
				show_this = (<NewEvent onAddNewEventGetNewerList={this.importEventList} />);
			} else {
				show_this = (<EventListTable items={items} is_past={false} />);
			}
		}

		var future_count = null;
		var past_count = null;
		var list = this.state.list;
		if (list != null) {
			future_count = 0;
			past_count = 0;
			for (var pos in list) {
				var event = list[pos];
				if (event.planned_date > (new Date()).getTime()) {
					future_count++;
				} else {
					past_count++;
				}
			}
		}

		return (
			<mydmam.async.PageHeaderTitle title={i18n("dareport.eventlist.page")} fluid="true">
				<ul className="nav nav-tabs">
					<mydmam.async.HeaderTab href={"#" + eventlist_link_future}  i18nlabel="dareport.event.future" badge_count={future_count} />
					<mydmam.async.HeaderTab href={"#" + eventlist_link_past} 	i18nlabel="dareport.event.past" badge_count={past_count} />	
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
