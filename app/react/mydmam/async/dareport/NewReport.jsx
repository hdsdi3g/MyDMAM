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
		if (this.state.check != this.props.panel.reverse_boolean) {
			ReactDOM.findDOMNode(this.refs.commentzone).value = "";
		}

		var content = {
			check: this.state.check,
			comment: ReactDOM.findDOMNode(this.refs.commentzone).value.trim(),
		};

		this.props.onChange(this.props.panelnum, content);
	},
	onClickRadioYes: function(e) {
		this.setState({
			check: true,
		}, this.onUpdate);
	},
	onClickRadioNo: function(e) {
		this.setState({
			check: false,
		}, this.onUpdate);
	},
	onTextareaTextChange: function(e) {
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

		return (<FormControlGroup label={panel.label}>
			{first_radio}
			{second_radio}
			<textarea
				rows="2"
				className="input-xxlarge dareport"
				defaultValue=""
				ref="commentzone"
				placeholder={panel.tips}
				readOnly={this.isTextAreaEnabled() == false}
				style={{fontFamily: "monospace", fontSize: 12, marginLeft: "12px"}}
				onClick={this.onTextareaClick}
				onKeyUp={this.onTextareaTextChange} />

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
				<span className="pull-right muted" style={{paddingRight: "5px", }}>
					{(new Date(event.planned_date)).getI18nFullDisplayTime()}
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
				return a.created_at > b.created_at;
			} else {
				return a.planned_date > b.planned_date;
			}
		});

		var items = [];
		for (var pos in sorted_events) {
			items.push(<Event key={sorted_events[pos].name} event={sorted_events[pos]} onSelect={this.props.onSelectEvent} inlist={true} />);
		}

		return (<div>
			<strong>{i18n("dareport.report.events.select")}</strong>
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
	onChangeForm: function(panelnum, content) {
		var panels = this.state.panels;
		var form_content = jQuery.extend(true, {}, this.state.form_content);
		form_content[panelnum] = content;
		
		var form_ready_to_send = true;
		for (var panelnum in panels) {
			if (! form_content[panelnum]) {
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
		var form_content = this.state.form_content;
		var content = [];
		for (var pos in form_content) {
			content.push(form_content[pos]);
		}

		var report = {
			event_name: this.state.selected_event_name,
			content: content,
		};

		mydmam.async.request("dareport", "reportnew", report, function(data) {
			if (data.done) {

				/** Remove the event from the sended report... */
				var actual_events = jQuery.extend(true, {}, this.state.events);
				var pos_to_delete = -1;
				for (var pos in actual_events) {
					if (actual_events[pos].name == this.state.selected_event_name) {
						pos_to_delete = pos;
						break;
					}
				}
				if (pos_to_delete > -1) {
					delete actual_events[pos_to_delete];
				}

				this.setState({
					selected_event_name: null,
					form_ready_to_send: false,
					form_content: {},
					report_sended: true,
					report_notsended: false,
					events: actual_events,
				});
			} else {
				this.setState({
					report_sended: false,
					report_notsended: true,
				});
			}
		}.bind(this));
	},
	onClickHideReportSendedAlert: function() {
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
					<mydmam.async.SimpleBtn enabled={true} onClick={this.onSelectEvent} reference={null} normalsize={true}>
						<i className="icon-chevron-left"></i> {i18n("dareport.report.changeevent")}
					</mydmam.async.SimpleBtn>
				</div>);
				if (panels == null) {
					report_form = (<mydmam.async.PageLoadingProgressBar />);
				} else {
					var form_panels = [];
					for (var panelnum in panels) {
						form_panels.push(<Panel panel={panels[panelnum]} panelnum={panelnum} key={panelnum} onChange={this.onChangeForm} />);
					}

					var btn_send_form = null;
					if (form_ready_to_send) {
						btn_send_form = (<FormControlGroup>
							<span style={{marginLeft: "18px"}}>
								<mydmam.async.SimpleBtn enabled={true} normalsize={true} onClick={this.onSendForm} reference={null} btncolor="btn-primary">
									<i className="icon-ok icon-white"></i> {i18n("dareport.report.send")}
								</mydmam.async.SimpleBtn>
							</span>
							<span style={{marginLeft: "15px"}}>
								<i className="icon-info-sign"></i> <em>{i18n("dareport.report.send.tips")}</em>
							</span>
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
		if (selected_event_name) {
			title = i18n("dareport.report.pagewithjobname", selected_event_name);
		}

		var alert_report_sended = null;
		if (this.state.report_sended) {
			alert_report_sended = (<mydmam.async.AlertInfoBox title={i18n("dareport.report.send.ok")} onClose={this.onClickHideReportSendedAlert} />);
		} else if (this.state.report_notsended) {
			alert_report_sended = (<mydmam.async.AlertErrorBox title={i18n("dareport.report.send.nok")} onClose={this.onClickHideReportSendedAlert} />);
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
