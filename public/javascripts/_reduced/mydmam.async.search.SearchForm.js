(function(a){a.SearchForm=React.createClass({displayName:"SearchForm",getInitialState:function(){return{new_q:""};
},componentDidMount:function(){this.setState({new_q:this.props.results.q});React.findDOMNode(this.refs.q).focus();
},handleChange:function(b){this.setState({new_q:React.findDOMNode(this.refs.q).value});
},handleSubmit:function(c){c.preventDefault();var b=React.findDOMNode(this.refs.q).value.trim();
if(!b){return;}this.props.onSearchFormSubmit({q:b});},render:function(){return(React.createElement("form",{className:"search-query form-search",onSubmit:this.handleSubmit},React.createElement("div",{className:"input-append"},React.createElement("input",{type:"text",ref:"q",value:this.state.new_q,placeholder:i18n("maingrid.search"),className:"search-query span10",onChange:this.handleChange}),React.createElement("button",{className:"btn btn-info",type:"submit"},i18n("maingrid.search")))));
}});})(window.mydmam.async.search);