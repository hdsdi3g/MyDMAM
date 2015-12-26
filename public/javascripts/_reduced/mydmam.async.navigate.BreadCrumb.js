(function(a){a.BreadCrumb=React.createClass({displayName:"BreadCrumb",render:function(){var e=mydmam.metadatas.url.navigate_react;
var f=this.props.storagename;var j=this.props.path;if(f==null){return(React.createElement("ul",{className:"breadcrumb"},React.createElement("li",{className:"active"},i18n("browser.storagestitle"))));
}var i=j.split("/");var d="";var b="";var g=[];for(var h=1;h<i.length;h++){b=f+":"+d+"/"+i[h];
if(h+1<i.length){g.push(React.createElement("li",{key:h},React.createElement("span",{className:"divider"},"/"),React.createElement("a",{href:e+"#"+b,onClick:this.props.navigate,"data-navigatetarget":b},i[h])));
}else{g.push(React.createElement("li",{key:h,className:"active"},React.createElement("span",{className:"divider"},"/"),i[h]));
}d=d+"/"+i[h];}var c=[];if(g.length>0){c.push(React.createElement("li",{key:"storagestitle"},React.createElement("a",{href:e,onClick:this.props.navigate,"data-navigatetarget":""},i18n("browser.storagestitle")),React.createElement("span",{className:"divider"},"::")));
if(j!="/"){c.push(React.createElement("li",{key:"root"},React.createElement("a",{href:e+"#"+f+":/",onClick:this.props.navigate,"data-navigatetarget":f+":/"},f)));
}else{c.push(React.createElement("li",{key:"root",className:"active"},f));}return(React.createElement("ul",{className:"breadcrumb"},c,g));
}return null;}});})(window.mydmam.async.navigate);