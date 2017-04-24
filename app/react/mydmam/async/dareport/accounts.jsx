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

dareport.accountlist_link = "dareport/accountlist";

var UserInfo = createReactClass({
	onClick: function(e) {
		e.preventDefault();
		this.props.onSelectUser(this.props.user);
	},
	render: function() {
		return (<div className="btn btn-link" onClick={this.onClick} style={{paddingRight: "8px", paddingLeft: "2px"}}>
			{this.props.user.full_name}
		</div>);
	}
});

dareport.AccountAdd = createReactClass({
	getInitialState: function() {
		return {
			selected_user: null,
			founded_users: null,
			alldeclaredjobslist: null,
			selected_job: null,
		};
	},
	componentWillMount: function() {
		mydmam.async.request("dareport", "alldeclaredjobslist", {}, function(data) {
			this.setState({alldeclaredjobslist: data, });
		}.bind(this));
	},
	onKeyPressUserInput: function(e) {
		var raw_value = ReactDOM.findDOMNode(this.refs.user_key).value;
		raw_value = raw_value.trim();
		if (raw_value.length < 3) {
			this.setState({selected_user: null, founded_users: [], selected_job: null,});
			return
		}
		mydmam.async.request("auth", "searchuser", raw_value, function(data) {
			ReactDOM.findDOMNode(this.refs.user_key).value = data.q;
			this.setState({selected_user: null, founded_users: data.results, selected_job: null,});
		}.bind(this));
	},
	onSelectUser: function(user) {
		this.setState({selected_user: user, founded_users: null, selected_job: null, });
		ReactDOM.findDOMNode(this.refs.user_key).value = "";
	},
	onSelectJob: function(e) {
		e.preventDefault();
		this.setState({selected_job: ReactDOM.findDOMNode(this.refs.select_job).value});
	},
	onAdd: function(e) {
		e.preventDefault();
		if (this.state.selected_user == null | this.state.selected_job == null) {
			return;
		}
		this.props.onValidate(this.state.selected_user, this.state.selected_job);
	},
	render: function() {
		var FormControlGroup = mydmam.async.FormControlGroup;

		var label_selected = null;
		var submit_btn = null;
		var job_list = null;
		if (this.state.selected_user) {
			label_selected = (<strong><i className="icon-user"></i> {this.state.selected_user.full_name}</strong>);

			if (this.state.alldeclaredjobslist != null) {
				var alldeclaredjobslist = this.state.alldeclaredjobslist;
				var content = [<option key={0} value=""></option>];
				for (var name in alldeclaredjobslist) {
					content.push(<option key={name} value={name}>{i18n(alldeclaredjobslist[name])}</option>);
				}
				job_list = (<FormControlGroup>
					<select ref="select_job" onChange={this.onSelectJob} required="true">
						{content}
					</select>
				</FormControlGroup>);
			}

			if (this.state.selected_job != null) {
				submit_btn = (<FormControlGroup>
					<button type="submit" className="btn btn-success"><i className="icon-ok icon-white"></i> {i18n("dareport.accountlist.addbtn")}</button>
				</FormControlGroup>);
			}
		}

		var founded_users = this.state.founded_users;
		var block_search_result = null;
		if (founded_users) {
			if (founded_users.length > 0) {
				var content = [];
				for (var pos in founded_users) {
					content.push(<UserInfo onSelectUser={this.onSelectUser} key={pos} user={founded_users[pos]} />);
				}
				block_search_result = (<FormControlGroup>
					<i className="icon-search"></i> {content}
				</FormControlGroup>);
			}
		}

		return (<form className="form-horizontal" onSubmit={this.onAdd}>
			<fieldset>
			    <legend>{i18n("dareport.accountlist.declareuser")}</legend>
			</fieldset>
			<FormControlGroup label={i18n("dareport.accountlist.searchuser")}>
				<input type="text" className="search-query span2" placeholder={i18n("")} ref="user_key" onKeyUp={this.onKeyPressUserInput} /> {label_selected}
			</FormControlGroup>
			{block_search_result}
			{job_list}
			{submit_btn}
		</form>);
	}
});

var DeleteBtn = createReactClass({
	onClick: function(e) {
		e.preventDefault();
		this.props.removeUser(this.props.userkey);
	},
	render: function() {
		return (<button onClick={this.onClick} className="btn btn-mini btn-danger"><i className="icon-remove icon-white"></i> {i18n("dareport.delete")}</button>);
	}
});

dareport.AccountList = createReactClass({
	getInitialState: function() {
		return {list: null, jobnames: {}, show_add_form: false, usernames: {}};
	},
	componentWillMount: function() {
		mydmam.async.request("dareport", "alldeclaredjobslist", {}, function(data) {
			this.setState({jobnames: data, });
		}.bind(this));

		mydmam.async.request("dareport", "accountlist", {}, function(data) {
			this.setState({list: data.list, usernames: data.usernames, });
		}.bind(this));
	},
	onAddUser: function(user, job_key) {
		var request = {
			userkey: user.key,
			jobkey: job_key,
		};

		mydmam.async.request("dareport", "accountnewupdate", request, function(data) {
			this.setState({list: data.list, usernames: data.usernames, });
		}.bind(this));
	},
	onClickShowAddForm: function(e) {
		e.preventDefault();
		this.setState({show_add_form: true});
	},
	getUsername: function(userkey) {
		if (this.state.usernames[userkey]) {
			return this.state.usernames[userkey];
		}
		return i18n("dareport.deleteduser");
	},
	onRemoveUser: function(userkey) {
		mydmam.async.request("dareport", "accountdelete", userkey, function(data) {
			this.setState({list: data.list, usernames: data.usernames, });
		}.bind(this));
	},
	render: function() {
		var show_this = (<mydmam.async.PageLoadingProgressBar />);

		if (this.state.list != null) {
			var table_content = [];

			for (var pos in this.state.list) {
				var item = this.state.list[pos];
				table_content.push(<tr key={pos}>
					<td>
						{this.getUsername(item.userkey)}
						<span className="pull-right">
							<DeleteBtn removeUser={this.onRemoveUser} userkey={item.userkey} />
						</span>
					</td>
					<td>{this.state.jobnames[item.jobkey]}</td>
					<td><mydmam.async.pathindex.reactDate date={item.created_at} /></td>
				</tr>);
			}

			var table = (<table className="table table-striped table-hover table-bordered table-condensed">
    			<thead>
    				<tr>
    					<th>{i18n("dareport.user")}</th>
    					<th>{i18n("dareport.job")}</th>
    					<th>{i18n("dareport.created_at")}</th>
    				</tr>
    			</thead>
    			<tbody>
    				{table_content}
    			</tbody>
    		</table>);

			var add_form = (<div style={{marginBottom: "12px", }}>
				<button onClick={this.onClickShowAddForm} className="btn btn-primary"><i className="icon-plus icon-white"></i> {i18n("dareport.accountlist.showformadduserbtn")}</button>
			</div>);

			if (this.state.show_add_form) {
				add_form = (<div>
					<dareport.AccountAdd onValidate={this.onAddUser} />
					<hr />
				</div>);
			}

			show_this = (<div>
				{add_form}
				{table}
			</div>);
		}

		return (<div className="container">
			<mydmam.async.PageHeaderTitle title={i18n("dareport.accountlist.page")} fluid="false">
				{show_this}
			</mydmam.async.PageHeaderTitle>
		</div>);
	}
});

mydmam.routes.push("dareport-accountlist", dareport.accountlist_link, dareport.AccountList, [{name: "dareport", verb: "accountlist"}]);
