(function(a){a.reactMetadata1Line=React.createClass({displayName:"reactMetadata1Line",render:function(){var e=this.props.stat;
if(e==null){return null;}var c=e.mtdsummary;if(c==null){return null;}var f=[];if(c.summaries){for(var b in c.summaries){f.push(React.createElement("span",{key:b},c.summaries[b]));
}}var d={marginLeft:5};if(this.props.style){d=this.props.style;}if(f.length>0){return(React.createElement("small",{style:d},mydmam.async.pathindex.mtdTypeofElement(c)," :: ",f));
}else{return(React.createElement("small",{style:d},mydmam.async.pathindex.mtdTypeofElement(c)));
}}});})(window.mydmam.async.pathindex);