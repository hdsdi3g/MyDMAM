(function(a){a.reactExternalPosition=React.createClass({displayName:"reactExternalPosition",render:function(){var g=this.props.externalpos;
var h=this.props.pathindexkey;if(!g.positions){return(React.createElement("span",null));
}var d=g.positions[h];if(!d){return(React.createElement("span",null));}var f=null;
var c=[];var b;for(var e=0;e<d.length;e++){if(d[e]=="cache"){c.push(React.createElement("span",{style:f,className:"label label-success external-pathindex-position",key:e},React.createElement("i",{className:"icon-barcode icon-white"})," ",React.createElement("i",{className:"icon-ok icon-white"})," ",i18n("browser.externalposition.online")));
break;}b=g.locations[d[e]];if(!b){continue;}if(b.isexternal){c.push(React.createElement("span",{style:f,className:"label label-important external-pathindex-position",key:e},React.createElement("i",{className:"icon-barcode icon-white"})," ",b.barcode));
}else{c.push(React.createElement("span",{style:f,className:"label label-success external-pathindex-position",key:e},React.createElement("i",{className:"icon-barcode icon-white"})," ",React.createElement("i",{className:"icon-screenshot icon-white"})," ",mydmam.module.f.i18nExternalPosition(b.location)));
}}return(React.createElement("span",{style:{display:"inline-block",whiteSpace:"nowrap"}},c));
}});})(window.mydmam.async.pathindex);