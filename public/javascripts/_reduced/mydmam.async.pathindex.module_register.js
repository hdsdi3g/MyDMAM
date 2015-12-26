(function(c){var b=function(g,d,e){if(!g|!e){return false;}if(g!=="pathindex"){return false;
}if(d){return false;}if(!list_external_positions_storages){return false;}for(var f=0;
f<list_external_positions_storages.length;f++){if(list_external_positions_storages[f]===e){return true;
}}return false;};var a=function(d){if(d.index!=="pathindex"){return null;}return c.react2lines;
};mydmam.module.register("PathIndexView",{processViewSearchResult:a,wantToHaveResolvedExternalPositions:b});
})(window.mydmam.async.pathindex);