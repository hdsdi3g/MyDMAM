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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
}*
/** Define URLs for JS functions */
(function(mydmam) {

mydmam.urlimgs.ajaxloader = "@{'/public/img/ajax-loader.gif'}";
	
#{secure.check 'navigate'}
	mydmam.metadatas.url.navigate = "@{Application.navigate()}";
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
	
#{/secure.check}

#{secure.check 'showQueue'}
	mydmam.queue.url.getall = "@{Queue.getall()}";
	mydmam.queue.url.getupdate = "@{Queue.getupdate()}";
	mydmam.workermanager.url.getworkers = "@{Queue.getworkers()}";
#{/secure.check}
#{secure.check 'updateQueue'}
	mydmam.queue.enable_update = true;
	mydmam.workermanager.enable_update = true;
	
	mydmam.queue.url.changetaskstatus = "@{Queue.changetaskstatus()}";
	mydmam.queue.url.changetaskpriority = "@{Queue.changetaskpriority()}";
	mydmam.queue.url.changetaskmaxage = "@{Queue.changetaskmaxage()}";
	mydmam.workermanager.url.changeworkerstate = "@{Queue.changeworkerstate()}";
	mydmam.workermanager.url.changeworkercyclicperiod = "@{Queue.changeworkercyclicperiod()}";
#{/secure.check}

#{secure.check 'showStatus'}
	mydmam.service.url.laststatusworkers = "@{Service.laststatusworkers()}";
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
		

mydmam.notification.url.notificationresolveusers = "@{UserNotifications.notificationresolveusers}";
mydmam.notification.url.notificationupdateread = "@{UserNotifications.notificationupdateread(key='keyparam1')}";
mydmam.notification.url.queuegettasksjobs = "@{Queue.gettasksjobs}";

#{secure.check 'userAction'}
mydmam.useraction.url.create = "@{UserAction.create()}";
mydmam.useraction.url.currentavailabilities = "@{UserAction.currentavailabilities()}";
mydmam.useraction.url.optionsforselectform = "@{UserAction.optionsforselectform()}";
#{/secure.check}

})(window.mydmam);
