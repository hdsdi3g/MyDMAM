if(!mydmam){mydmam={};}if(!mydmam.module){mydmam.module={};}if(!mydmam.routes){mydmam.routes={};
}if(!mydmam.urlimgs){mydmam.urlimgs={};}(function(a){String.prototype.trim=function(){return(this.replace(/^[\s\xA0]+/,"").replace(/[\s\xA0]+$/,""));
};String.prototype.startsWith=function(b){return(this.match("^"+b)==b);};String.prototype.endsWith=function(b){return(this.match(b+"$")==b);
};String.prototype.append=function(b){return this+b;};String.prototype.nl2br=function(){return this.replace(/([^>\r\n]?)(\r\n|\n\r|\r|\n)/g,"$1<br>$2");
};Storage.prototype.setObject=function(b,c){return this.setItem(b,JSON.stringify(c));
};Storage.prototype.getObject=function(b){return JSON.parse(this.getItem(b));};a.keycodemap={down:40,up:38,enter:13,backspace:8,esc:27};
if(a.console==null){a.console={};a.console.log=function(){};a.console.err=function(){};
}})(window);(function(b){var a={};b.register=function(g,f){if(!g){throw"No module name for register !";
}if(!f){throw"No correct declaration for register module: "+g;}for(var e in f){if(!a[e]){a[e]=[];
}a[e].push({name:g,callback:f[e]});}};b.dumpList=function(){console.log(a);};b.getCallbacks=function(e){if(a[e]){return a[e];
}else{return[];}};var c=function(e){return function(h){var j=b.getCallbacks(e);var f;
var m;var k;var g={};for(var l in j){m=j[l];k=m.name;try{var f=m.callback.apply(this,arguments);
g[k]=f;}catch(i){console.error(i,k,e);}}return g;};};var d=function(e){return function(g){var i=b.getCallbacks(e);
var f;var k;for(var j in i){k=i[j];try{var f=k.callback.apply(this,arguments);if(f){return f;
}}catch(h){console.error(h,k.name,e);}}return null;};};b.f={};b.f.helloworld=c("helloworld");
b.f.processViewSearchResult=d("processViewSearchResult");b.f.wantToHaveResolvedExternalPositions=d("wantToHaveResolvedExternalPositions");
b.f.i18nExternalPosition=d("i18nExternalPosition");})(window.mydmam.module);(function(b){var c={};
b.push=function(d,f,e,g){c[d]={};c[d].path=f;c[d].react_top_level_class=e;if(g){c[d].async_needs=g;
}};var a=function(e,d){return function(f){e(d,f.params);};};b.populate=function(d,i){for(var e in c){var g=c[e].async_needs;
if(g!=null){for(var h in g){var f=g[h];if(f.name==null){console.error("Name param missing for async_need",f);
}if(f.verb==null){console.error("Verb param missing for async_need",f);}}if(mydmam.async.isAvaliable(f.name,f.verb)==false){continue;
}}d.add(c[e].path,a(i,e));}};b.getReactTopLevelClassByRouteName=function(d){if(c[d]){return c[d].react_top_level_class;
}return null;};})(window.mydmam.routes);