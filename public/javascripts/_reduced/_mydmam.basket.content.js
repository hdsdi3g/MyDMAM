(function(b){var a=b.backend;b.add=function(c){if(b.contain(c)===false){var d=a.pullData();
d.push(c);a.pushData(d);}};})(window.mydmam.basket.content);(function(b){var a=b.backend;
b.remove=function(c){var d=a.pullData();var e=$.inArray(c,d);if(e>-1){d.splice(e,1);
}a.pushData(d);};})(window.mydmam.basket.content);(function(b){var a=b.backend;b.contain=function(c){var d=$.inArray(c,a.pullData());
return(d>-1);};})(window.mydmam.basket.content);