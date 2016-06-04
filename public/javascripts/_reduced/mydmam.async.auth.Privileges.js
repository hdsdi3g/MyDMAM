(function(a){a.Privileges=React.createClass({displayName:"Privileges",getInitialState:function(){return{fulllist:{}};
},componentWillMount:function(){mydmam.async.request("auth","getallprivilegeslist",null,function(b){this.setState({fulllist:b});
}.bind(this));},render:function(){var c=this.state.fulllist;var b=[];var d=function(f){var e=[];
for(pos in f){e.push(React.createElement("div",{key:pos},f[pos]));}return e;};for(privilege_name in c){b.push(React.createElement("tr",{key:privilege_name},React.createElement("td",null,privilege_name),React.createElement("td",null,d(c[privilege_name]))));
}return(React.createElement("div",null,React.createElement("table",{className:"table table-bordered table-striped table-condensed"},React.createElement("thead",null,React.createElement("tr",null,React.createElement("th",null,i18n("auth.privilege")),React.createElement("th",null,i18n("auth.privilege.controllers")))),React.createElement("tbody",null,b))));
}});})(window.mydmam.async.auth);