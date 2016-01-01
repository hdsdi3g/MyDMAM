(function(a){a.Items=React.createClass({displayName:"Items",getInitialState:function(){return{list:{}};
},componentWillMount:function(){mydmam.async.request("instances","allitems",null,function(b){this.setState({list:b});
}.bind(this));},render:function(){return(React.createElement("div",null,"Items"));
}});})(window.mydmam.async.manager);