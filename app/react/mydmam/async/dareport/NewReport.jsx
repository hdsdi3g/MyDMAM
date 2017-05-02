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

/**
 * Actually only one type : radiobox
 */
var Panel = createReactClass({
	getInitialState: function() {
		return {
			check: null,
		};
	},
	onUpdate: function() {
		//TODO collect check (if false | if reverse_boolean&true) + comment zone > this.props.onChangeForm(panelref, content)
	},
	onClickRadioYes: function(e) {
		//e.preventDefault();
		//TODO empty textarea if reverse_boolean...
		this.setState({
			check: true,
		});
		this.onUpdate();
	},
	onClickRadioNo: function(e) {
		//e.preventDefault();
		//TODO empty textarea if !reverse_boolean...
		this.setState({
			check: false,
		});
		this.onUpdate();
	},
	isTextAreaEnabled: function() {
		var is_activate = (this.state.check != null);
		if (is_activate) {
			var reverse_boolean = this.props.panel.reverse_boolean;
			if (!reverse_boolean) {
				return !this.state.check;
			} else {
				return this.state.check;
			}
		} else {
			return false;
		}
	},
	onTextareaClick: function() {
		if (this.isTextAreaEnabled() == false) {
			var reverse_boolean = this.props.panel.reverse_boolean;
			if (!reverse_boolean) {
				this.onClickRadioNo();
			} else {
				this.onClickRadioYes();
			}
		}
	},
	render: function() {
		var FormControlGroup = mydmam.async.FormControlGroup;
		var panel = this.props.panel;

		if (panel.type != "radiobox") {
			return (<strong>Unknow panel type "{panel.type}"! Only "radiobox" panel type si supported! Check the configuration.</strong>);
		}

		var is_activate = (this.state.check != null);
		var is_set_true = false;
		var is_set_false = false;
		if (is_activate) {
			is_set_true = (this.state.check == true);
			is_set_false = !is_set_true;
		}

		var reverse_boolean = panel.reverse_boolean;
		var first_radio = (<label onClick={this.onClickRadioYes} className="checkbox">
			<input type="radio" checked={is_set_true} style={{marginTop: "0px", marginRight: "5px"}} />
			<span style={{marginTop: "4px"}}>{i18n("dareport.report.event.yes")}</span>
		</label>);
		var second_radio = (<label onClick={this.onClickRadioNo} className="checkbox">
			<input type="radio" checked={is_set_false} style={{marginTop: "0px", marginRight: "5px"}} />
			<span >{i18n("dareport.report.event.no")}</span>
		</label>);

		if (reverse_boolean) {
			var swap = second_radio;
			second_radio = first_radio;
			first_radio = swap;
		}

		//this.props.panelref

		/*async.CheckboxItem = createReactClass({
			propTypes: {
			reference: PropTypes.string.isRequired,
			checked: PropTypes.bool.isRequired,
			onChangeCheck: PropTypes.func.isRequired,
		*/

		return (<FormControlGroup label={panel.label}>
			{first_radio}
			{second_radio}
			<textarea
				rows="2"
				className="input-xxlarge dareport"
				defaultValue=""
				ref="comment"
				placeholder={panel.tips}
				readOnly={this.isTextAreaEnabled() == false}
				style={{fontFamily: "monospace", fontSize: 12, marginLeft: "12px"}}
				onClick={this.onTextareaClick} />

		</FormControlGroup>);
	}
}); 

var Event = createReactClass({
	onClick: function(e) {
		e.preventDefault();
		if (this.props.inlist) {
			this.props.onSelect(this.props.event.name);
		}
	},
	render: function() {
		var event = this.props.event;
		var inlist = this.props.inlist;

		var classname = classNames("row-fluid", "dareport-event", {
			"in-list": inlist,
		});

		var show_date = null;
		if (inlist) {
			show_date = (<div className="span4">
				<span className="pull-right" style={{paddingRight: "5px", }}>
					<mydmam.async.pathindex.reactDate date={event.planned_date} format="long" />
				</span>
			</div>);
		}

		var name_class = classNames({
			"text-success": !inlist,
		});

		return (<div className={classname} onClick={this.onClick}>
			<div className="span8">
				<i className="icon-calendar"></i> <span className={name_class}>{event.name}</span>
			</div>
			{show_date}
		</div>);
	}
});

var EventList = createReactClass({
	render: function() {
		var events = this.props.events;
		if (events.length == 0) {
			return (<mydmam.async.AlertInfoBox title={i18n("dareport.report.events.empty")} />);
		}

		var sorted_events = [];
		for (var pos in events) {
			sorted_events.push(events[pos]);
		}

		sorted_events.sort(function(a, b) {
			if (a.planned_date == b.planned_date) {
				return a.created_at - b.created_at;
			} else {
				return a.planned_date - b.planned_date;
			}
		});

		var items = [];
		for (var pos in sorted_events) {
			items.push(<Event key={events[pos].name} event={events[pos]} onSelect={this.props.onSelectEvent} inlist={true} />);
		}

		return (<div>
			<p className="text-warning"><strong>{i18n("dareport.report.events.select")}</strong></p>
		    <div className="container">
				<div className="row-fluid">
					<div className="span7">
						{items}
					</div>
				</div>
		    </div>
		</div>);
	}
});

dareport.NewReport = createReactClass({
	getInitialState: function() {
		return {
			events: null,
			panels: null,
			jobname: null,
			selected_event_name: null,
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
			selected_event_name: event_name,
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

		this.setState({
			form_content: form_content,
			form_ready_to_send: form_ready_to_send,
		});		
	},
	onSendForm: function() {
		var report = {
			event_name: this.state.selected_event_name,
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
					selected_event_name: null,
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
		var FormControlGroup = mydmam.async.FormControlGroup;

		var events = this.state.events;
		var panels = this.state.panels;
		var selected_event_name = this.state.selected_event_name;
		var form_ready_to_send = this.state.form_ready_to_send;

		var event_list = null;
		var report_form = null;

		if (events == null) {
			event_list = (<mydmam.async.PageLoadingProgressBar />);
		} else {
			if (selected_event_name == null) {
				event_list = (<EventList onSelectEvent={this.onSelectEvent} events={events} />);
			} else {
				var selected_event = null;
				for (var pos in events){
					if (events[pos].name == selected_event_name) {
						selected_event = events[pos];
						break;
					}
				}

				event_list = (<div>
				    <div className="container">
						<div className="row-fluid">
							<div className="span7">
								<Event event={selected_event} />
							</div>
						</div>
						<div className="row-fluid">
							<div className="span7">
								<mydmam.async.SimpleBtn enabled={true} onClick={this.onSelectEvent} reference={null}>
									<i className="icon-chevron-left"></i> {i18n("dareport.report.changeevent")}
								</mydmam.async.SimpleBtn>
							</div>
						</div>
				    </div>
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
						<form className="form-horizontal dareport" style={{marginTop: "15px"}}>
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
