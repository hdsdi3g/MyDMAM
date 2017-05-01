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

dareport.reportnew_link = "dareport";

var Panel = createReactClass({
	render: function() {
		//this.props.panel
		//this.props.panelref
		//onChangeForm: function(panelref, content)
		/*async.CheckboxItem = createReactClass({
			propTypes: {
			reference: PropTypes.string.isRequired,
			checked: PropTypes.bool.isRequired,
			onChangeCheck: PropTypes.func.isRequired,
		*/
		return (<span>TODO3...</span>);
	}
}); 

var Event = createReactClass({
	render: function() {
		//this.props.event
		return (<span>TODO2...</span>);
	}
});

var EventList = createReactClass({
	render: function() {
		var events = this.props.events;
		if (events.length == 0) {
			return (<mydmam.async.AlertInfoBox title={i18n("dareport.report.events.empty")} />);
		}
		
		//this.props.onSelectEvent(name)
		return (<span>TODO1...</span>);
	}
});

dareport.NewReport = createReactClass({
	getInitialState: function() {
		return {
			events: null,
			panels: null,
			jobname: null,
			selected_event: null,
			form_ready_to_send: false,
			form_content: {},
			report_sended: false,
			report_notsended: false,
		};
	},
	componentWillMount: function() {
		mydmam.async.request("dareport", "eventlisttoday", null, function(data) {
			this.setState({events: data});
		}.bind(this));

		mydmam.async.request("dareport", "getpanelsformyjob", null, function(data) {
			this.setState({
				panels: data.panels,
				jobname: data.jobname,
			});
		}.bind(this));
	},
	onSelectEvent: function(event_name) {
		this.setState({
			selected_event: event_name,
			report_sended: false,
			report_notsended: false,
		});
	},
	onChangeForm: function(panelref, content) {
		var form_content = jQuery.extend(true, {}, this.state.form_content);
		form_content[panelref] = content;
		
		var form_ready_to_send = true;
		for (var panelref in panels) {
			if (! form_content[panelref]) {
				form_ready_to_send = false;
				break;
			}
		}

		this.setState({form_content: form_content, form_ready_to_send: form_ready_to_send});		
	},
	onSendForm: function() {
		var report = {
			event_name: this.state.selected_event.name,
			content: [],
		};

		var form_content = this.state.form_content;
		for (var panelref in form_content) {
			report.content.push({
				panelref: panelref,
				content: form_content[panelref],
			});
		}

		mydmam.async.request("dareport", "reportnew", report, function(data) {
			if (data.done) {
				this.setState({
					selected_event: null,
					form_ready_to_send: false,
					form_content: {},
					report_sended: true,
					report_notsended: false,
				});
			} else {
				this.setState({
					report_sended: false,
					report_notsended: true,
				});
			}
		}.bind(this));
	},
	onClickHideReportSendedAlert: function(e) {
		e.preventDefault();
		this.setState({report_sended: false, report_notsended: false});
	},
	render: function() {
		var events = this.state.events;
		var panels = this.state.panels;
		var selected_event = this.state.selected_event;

		var event_list = null;
		var report_form = null;

		if (events == null) {
			event_list = (<mydmam.async.PageLoadingProgressBar />);
		} else {
			if (selected_event == null) {
				event_list = (<EventList onSelectEvent={this.onSelectEvent} events={events} />);
			} else {
				event_list = (<div>
					<mydmam.async.SimpleBtn enabled={true} onClick={this.onSelectEvent} reference={null}>
						<i className="icon-chevron-left"></i> {i18n("dareport.report.changeevent")}
					</mydmam.async.SimpleBtn>
					<Event event={selected_event} />
				</div>);
				if (panels == null) {
					report_form = (<mydmam.async.PageLoadingProgressBar />);
				} else {
					var form_panels = [];
					for (var panelref in panels) {
						form_panels.push(<Panel panel={panels[panelref]} panelref={panelref} key={panelref} onChange={this.onChangeForm} />);
					}

					var btn_send_form = null;
					if (form_ready_to_send) {
						btn_send_form = (<FormControlGroup>
							<mydmam.async.SimpleBtn enabled={true} onClick={this.onSendForm} reference={null} btncolor="btn-primary">
								<i className="icon-ok icon-white"></i> {i18n("dareport.report.send")}
							</mydmam.async.SimpleBtn>
						</FormControlGroup>);
					}

					report_form = (<div>
						<hr />
						<p className="lead">{i18n("dareport.report.formlegend")}</p>
						<form className="form-horizontal">
							{form_panels}
							{btn_send_form}
						</form>
					</div>);
				}
			}
		}
	
		var jobname = this.state.jobname;
		var title = i18n("dareport.report.page");
		if (jobname != null) {
			title = i18n("dareport.report.pagewithjobname", jobname);
		}

		var alert_report_sended = null;
		if (this.state.report_sended) {
			alert_report_sended = (<mydmam.async.AlertInfoBox title={i18n("dareport.report.send.ok")}>
				<a href="#" className="close" onClick={this.onClickHideReportSendedAlert}>&times;</a>
			</mydmam.async.AlertInfoBox>);
		} else if (this.state.report_notsended) {
			alert_report_sended = (<mydmam.async.AlertErrorBox title={i18n("dareport.report.send.nok")}>
				<a href="#" className="close" onClick={this.onClickHideReportSendedAlert}>&times;</a>
			</mydmam.async.AlertErrorBox>);
		}

		return (<div className="container">
			<mydmam.async.PageHeaderTitle title={title} fluid="false">
				{alert_report_sended}
				{event_list}
				{report_form}
			</mydmam.async.PageHeaderTitle>
		</div>);
	}
});

mydmam.routes.push("dareport-new", dareport.reportnew_link, dareport.NewReport, [{name: "dareport", verb: "reportnew"}]);
