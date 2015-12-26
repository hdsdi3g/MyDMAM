(function(a){a.found=function(b,d){if(!d){return;}var c=function(e){for(var f in e){e[f].reactkey=e[f].index+":"+e[f].type+":"+e[f].key;
}};c(b.results);React.render(React.createElement(a.SearchResultPage,{results:b}),d);
};})(window.mydmam.async.search);