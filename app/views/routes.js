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

(function(mydmam) {

if(!mydmam.routes){mydmam.routes = {};}
if(!mydmam.routes.statics){mydmam.routes.statics = {};}

mydmam.routes.statics.home = "@{Application.index()}";
mydmam.routes.statics.disconnect = "@{Secure.logout()}";
mydmam.routes.statics.github_favicon = "@{'/public/img/github-favicon.ico'}"; 
mydmam.routes.statics.async = "@{AsyncJavascript.index(name='nameparam1',verb='verbparam2')}";	
mydmam.routes.statics.navigate = "@{Application.index()}#navigate/"; // TODO check navigate !
mydmam.routes.statics.metadatafile = "@{Application.metadatafile(filehash='filehashparam1',type='typeparam2',file='fileparam3')}"; // TODO check navigate !
mydmam.routes.statics.ftpserver_export_user_sessions = "@{Manager.ftpserver_export_user_sessions(user_session_ref='keyparam1')}"; //TODO check adminFtpServer !

/*if(!mydmam.async){mydmam.async = {};}
mydmam.async.controllers = %{out.print(hd3gtv.mydmam.manager.AppManager.getGson().toJson(hd3gtv.mydmam.web.AJSController.getAllControllersVerbsForThisUser())); }% ;
*/

/*if(!mydmam.user){mydmam.user = {};}
mydmam.user.long_name = "%{out.print(hd3gtv.mydmam.web.AJSController.getUserProfileLongName())}%";
*/

})(window.mydmam);
