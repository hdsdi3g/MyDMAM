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
	mydmam.metadatas.url.navigate_react = "/#navigate/";
	mydmam.metadatas.url.metadatafile = "@{Application.metadatafile(filehash='filehashparam1',type='typeparam2',file='fileparam3')}";
#{/secure.check}

#{secure.check 'adminFtpServer'}
	mydmam.manager.url_ftpserver_export_user_sessions = "@{Manager.ftpserver_export_user_sessions(user_session_ref='keyparam1')}";
#{/secure.check}

})(window.mydmam);
