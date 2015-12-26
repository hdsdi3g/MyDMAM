(function(a){a.SearchBox=React.createClass({displayName:"SearchBox",getInitialState:function(){return{inputbox:$("#sitesearch")[0],timeoutid:null};
},componentDidMount:function(){this.state.inputbox.value="";this.state.inputbox.addEventListener("keypress",this.onTextChange);
},componentWillUnmount:function(){this.state.inputbox.removeEventListener("keypress",this.onTextChange);
window.clearTimeout(this.state.timeoutid);},shouldComponentUpdate:function(b,c){return false;
},onTextChange:function(){window.clearTimeout(this.state.timeoutid);this.setState({timeoutid:window.setTimeout(this.onEndTimeout,400)});
},onEndTimeout:function(){this.props.changeStateInputbox(this.state.inputbox);},render:function(){return null;
}});})(window.mydmam.async.navigate);