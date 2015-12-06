*{
 * This file is part of MyDMAM.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2015
 * 
}*

mydmam = {};

/** Define URLs for JS functions */
(function(mydmam) {

mydmam.urlimgs = {};
mydmam.async = {};
mydmam.metadatas = {};
mydmam.metadatas.url = {};
mydmam.stat = {};
mydmam.basket = {};
mydmam.basket.url = {};
mydmam.basket.allusers = {};
mydmam.manager = {};
mydmam.manager.url = {};
mydmam.notification = {};
mydmam.notification.url = {};

mydmam.urlimgs.ajaxloader = "@{'/public/img/ajax-loader.gif'}";
mydmam.urlimgs.github_favicon = "@{'/public/img/github-favicon.ico'}";

mydmam.async.url = "@{AsyncJavascript.index(name='nameparam1',verb='verbparam2')}";	
mydmam.async.controllers = %{out.print(hd3gtv.mydmam.manager.AppManager.getGson().toJson(hd3gtv.mydmam.web.AJSController.getAllControllersVerbsForThisUser())); }% ;

#{secure.check 'navigate'}
	mydmam.metadatas.url.navigate = "@{Application.navigate()}";
	mydmam.metadatas.url.navigate_react = "@{Application.navigate()}";
	mydmam.metadatas.url.resolvepositions = "@{Application.resolvepositions()}";
	mydmam.metadatas.url.metadatafile = "@{Application.metadatafile(filehash='filehashparam1',type='typeparam2',file='fileparam3')}";
	mydmam.stat.url = "@{Application.stat()}";
	
	mydmam.basket.url.push = "@{UserBasket.basket_push}";
	mydmam.basket.url.pull = "@{UserBasket.basket_pull}";
	mydmam.basket.url.all = "@{UserBasket.basket_get_all_user}";
	mydmam.basket.url.selected = "@{UserBasket.basket_get_selected}";
	mydmam.basket.url.bdelete = "@{UserBasket.basket_delete}";
	mydmam.basket.url.truncate = "@{UserBasket.basket_truncate}";
	mydmam.basket.url.rename = "@{UserBasket.basket_rename}";
	mydmam.basket.url.create = "@{UserBasket.basket_create}";
	mydmam.basket.url.switch_selected = "@{UserBasket.basket_switch_selected}";
	#{secure.check 'adminUsers'}
		mydmam.basket.allusers.ajaxurl = "@{UserBasket.basket_admin_action}";
	#{/secure.check}
	mydmam.async.baseURLsearch = "@{Application.search(q='param1query',from='param2from')}";
#{/secure.check}

#{secure.check 'showManager'}
	mydmam.manager.url.allinstances = "@{Manager.allinstances()}";
	mydmam.manager.url.allworkers = "@{Manager.allworkers()}";
#{/secure.check}

#{secure.check 'actionManager'}
	mydmam.manager.url.instanceaction = "@{Manager.instanceaction()}";
#{/secure.check}
	
#{secure.check 'showJobs'}
	mydmam.manager.url.alljobs = "@{Manager.alljobs()}";
	mydmam.manager.url.recentupdatedjobs = "@{Manager.recentupdatedjobs()}";
#{/secure.check}

#{secure.check 'actionJobs'}
	mydmam.manager.url.jobaction = "@{Manager.jobaction()}";
#{/secure.check}
		
mydmam.notification.url.notificationresolveusers = "@{UserNotifications.notificationresolveusers}";
mydmam.notification.url.notificationupdateread = "@{UserNotifications.notificationupdateread(key='keyparam1')}";
mydmam.notification.url.associatedjobs = "@{UserNotifications.associatedjobs}";

})(window.mydmam);
