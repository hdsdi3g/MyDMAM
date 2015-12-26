(function(a){if(!a.stat){a.stat={};}if(!a.stat.url){a.stat.url={};}var b=a.stat;b.DEFAULT_PAGE_SIZE=500;
b.SCOPE_DIRLIST="dirlist";b.SCOPE_PATHINFO="pathinfo";b.SCOPE_MTD_SUMMARY="mtdsummary";
b.SCOPE_COUNT_ITEMS="countitems";b.SCOPE_ONLYDIRECTORIES="onlydirs";})(window.mydmam);
(function(a){a.query=function(h,d,c,e,i,g){var f=e;var j=i;var b=c;var l=g;if(!f){f=0;
}if(!j){j=a.DEFAULT_PAGE_SIZE;}if(!b){b=[];}if(l==null){l="";}var k=null;$.ajax({url:a.url,type:"POST",async:false,data:{fileshashs:h,scopes_element:d,scopes_subelements:b,page_from:f,page_size:j,search:JSON.stringify(l)},success:function(m){k=m;
},error:function(m,o,n){console.error(n);}});return k;};})(window.mydmam.stat);