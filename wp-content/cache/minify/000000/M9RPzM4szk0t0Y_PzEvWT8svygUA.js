jQuery(function($){var ak_js=$('#ak_js');if(ak_js.length==0){ak_js=$('<input type="hidden" id="ak_js" name="ak_js" />');}
else{ak_js.remove();}
ak_js.val((new Date()).getTime());$('#commentform, #replyrow td:first').append(ak_js);});