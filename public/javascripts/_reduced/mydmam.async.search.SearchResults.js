(function(a){a.SearchResults=React.createClass({displayName:"SearchResults",render:function(){var c=this.props.stat;
var d=this.props.externalpos;var b=this.props.results.results.map(function(e){var f=mydmam.module.f.processViewSearchResult(e);
if(!f){console.error("Can't handle search result",e,c[e.key]);return(React.createElement("div",{style:{marginBottom:"1em"},key:e.reactkey},React.createElement("div",null,"Error")));
}else{return(React.createElement("div",{style:{marginBottom:"1em"},key:e.reactkey},React.createElement(f,{result:e,stat:c[e.key],externalpos:d})));
}});return(React.createElement("div",null,b));}});})(window.mydmam.async.search);
