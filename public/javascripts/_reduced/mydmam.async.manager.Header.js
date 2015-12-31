(function(a){a.Header=React.createClass({displayName:"Header",render:function(){var b=null;
if(this.props.params){if(this.props.params.tab){if(this.props.params.tab=="summary"){b=(React.createElement(a.Summaries,null));
}else{if(this.props.params.tab=="classpath"){b=(React.createElement(a.Classpaths,null));
}}}}if(b==null){b=(React.createElement(a.InstancesSummaries,null));}return(React.createElement(mydmam.async.PageHeaderTitle,{title:i18n("manager.pagename"),fluid:"true"},React.createElement("ul",{className:"nav nav-tabs"},React.createElement(a.HeaderTab,{href:"#manager/summary",i18nlabel:"manager.summaries"}),React.createElement(a.HeaderTab,{href:"#manager/classpath",i18nlabel:"manager.classpath"})),b));
}});mydmam.routes.push("manager","manager/:tab",a.Header,[{name:"instances",verb:"allsummaries"}]);
a.HeaderTab=React.createClass({displayName:"HeaderTab",onClick:function(b){$(React.findDOMNode(this.refs.tab)).blur();
},render:function(){var b=classNames({active:this.props.href==location.hash});return(React.createElement("li",{className:b},React.createElement("a",{href:this.props.href},i18n(this.props.i18nlabel))));
}});})(window.mydmam.async.manager);