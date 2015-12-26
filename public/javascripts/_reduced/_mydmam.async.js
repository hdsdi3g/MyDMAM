(function(a){if(!a.async){a.async={};}if(!a.async.controllers){a.async.controllers={};
}a.async.request=function(c,f,e,d,b){if(a.async.isAvaliable(c,f)===false){if(b){b();
}return;}$.ajax({url:a.async.url.replace("nameparam1",c).replace("verbparam2",f),type:"POST",dataType:"json",data:{jsonrq:JSON.stringify(e)},error:function(g,i,h){if(b){b();
}},success:d});};a.async.isAvaliable=function(b,c){if((c!=null)&(a.async.controllers[b]!=null)){return(a.async.controllers[b].indexOf(c)>-1);
}return(a.async.controllers[b]!=null);};})(window.mydmam);