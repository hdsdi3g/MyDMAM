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

dareport.eventlist_link = "dareport/events";
var eventlist_link_future = dareport.eventlist_link + "/future";
dareport.eventlist_link_future = eventlist_link_future;
var eventlist_link_past = dareport.eventlist_link + "/past";
var eventlist_link_add = dareport.eventlist_link + "/add";

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

var AuthorList = createReactClass({
	getInitialState: function() {
		return {
			display: false,
		};
	},
	onClickExpand: function(e) {
		e.preventDefault();
		this.setState({display: true,});
	},
	render: function() {
		var report_authors = this.props.report_authors;
		if (report_authors == null) {
			return (<span />);
		}

		if (this.state.display) {
			var usernames = this.props.usernames;
			var authors_names = [];
			for (var pos in report_authors) {
				authors_names.push(usernames[report_authors[pos]]);
			}

			return (<span style={{marginLeft: "6px"}}>
				<i className="icon-user"></i> <small>{authors_names.join(", ")}</small>
			</span>);
		} else {
			return (<span style={{marginLeft: "6px"}}>
				[<a href="#" onClick={this.onClickExpand} style={{textDecoration: "underline dashed #08c"}}>{report_authors.length}</a>]
			</span>);
		}
	},
});

var EventListTable = createReactClass({
	onClickDelete: function(ref) {
		mydmam.async.request("dareport", "eventdelete", {name: ref}, this.props.onDeleteEventGetNewerList);
	},
	onClickSend: function(ref) {
		mydmam.async.request("dareport", "eventsendmail", {name: ref}, function(){
		});
	},
	render: function() {
		var items = this.props.items;
		var report_authors_by_event_name = this.props.report_authors_by_event_name;
		var is_past = this.props.is_past;

		var content = [];
		for (var pos in items) {
			var item = items[pos];

			content.push(<tr key={pos}>
				<td>
					{item.name}
					<AuthorList report_authors={report_authors_by_event_name[item.name]} usernames={this.props.usernames} />
					<span className="pull-right">
						<mydmam.async.SimpleBtn enabled={report_authors_by_event_name[item.name] != null} hide_for_disable={true} onClick={this.onClickSend} reference={item.name} btncolor="btn-info">
							<i className="icon-envelope icon-white"></i>
						</mydmam.async.SimpleBtn>
					</span>
				</td>
				<td><mydmam.async.pathindex.reactDate date={item.planned_date} format="long" /></td>
				<td><mydmam.async.pathindex.reactDate date={item.created_at} format="long" /></td>
				<td>{item.creator}</td>
				<td><mydmam.async.BtnDelete enabled={report_authors_by_event_name[item.name] == null} hide_for_disable={true} onClickDelete={this.onClickDelete} reference={item.name} /></td>
			</tr>);
		}

		var table = (<table className="table table-striped table-hover table-bordered table-condensed">
			<thead>
				<tr>
					<th>{i18n("dareport.event.list.name")}
						<span className="pull-right">{i18n("dareport.event.list.resendreport")}</span>
					</th>
					<th>{i18n("dareport.event.list.scheduled")}</th>
					<th>{i18n("dareport.event.list.createdat")}</th>
					<th>{i18n("dareport.event.list.createdby")}</th>
					<th>{i18n("dareport.event.list.delete")}</th>
				</tr>
			</thead>
			<tbody>
    			{content}
			</tbody>
		</table>);

		return (table);
	},
});

dareport.Events = createReactClass({
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
		var list = this.state.list;
		var future_count = null;
		var past_count = null;
		var show_this = (<mydmam.async.PageLoadingProgressBar />);

		if (list != null) {
			var items_future = [];
			var items_past = [];
			future_count = 0;

			for (var pos in list) {
				var event = list[pos];
				if (event.planned_date > (new Date()).getTime()) {
					future_count++;
					items_future.push(event);
				} else {
					items_past.push(event);
				}
			}

			if (location.hash.indexOf("#" + eventlist_link_future) == 0) {
				show_this = (<EventListTable items={items_future} is_past={false} onDeleteEventGetNewerList={this.importEventList} report_authors_by_event_name={this.state.report_authors_by_event_name} usernames={this.state.usernames} />);
			} else if (location.hash.indexOf("#" + eventlist_link_past) == 0) {
				show_this = (<EventListTable items={items_past} is_past={true} report_authors_by_event_name={this.state.report_authors_by_event_name} usernames={this.state.usernames}  />);
			} else if (location.hash.indexOf("#" + eventlist_link_add) == 0) {
				show_this = (<NewEvent onAddNewEventGetNewerList={this.importEventList} />);
			} else {
				show_this = (<EventListTable items={items_future} is_past={false} onDeleteEventGetNewerList={this.importEventList} report_authors_by_event_name={this.state.report_authors_by_event_name} usernames={this.state.usernames} />);
			}
		}

		return (
			<mydmam.async.PageHeaderTitle title={i18n("dareport.eventlist.page")} fluid="true">
				<ul className="nav nav-tabs">
					<mydmam.async.HeaderTab href={"#" + eventlist_link_future}  i18nlabel="dareport.event.future" badge_count={future_count} />
					<mydmam.async.HeaderTab href={"#" + eventlist_link_past} 	i18nlabel="dareport.event.past" />	
					<mydmam.async.HeaderTab href={"#" + eventlist_link_add}     i18nlabel="dareport.event.add" />
				</ul>
				{show_this}
			</mydmam.async.PageHeaderTitle>
		);
	},
});

mydmam.routes.push("dareport-events",       dareport.eventlist_link,  dareport.Events, [{name: "dareport", verb: "eventlist"}]);
mydmam.routes.push("dareport-eventsfuture", eventlist_link_future,    dareport.Events, [{name: "dareport", verb: "eventlist"}]);
mydmam.routes.push("dareport-eventspast",   eventlist_link_past,      dareport.Events, [{name: "dareport", verb: "eventlist"}]);
mydmam.routes.push("dareport-eventsadd",    eventlist_link_add,       dareport.Events, [{name: "dareport", verb: "eventlist"}]);
