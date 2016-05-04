/*
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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.mydmam.auth;

import java.io.File;
import java.util.List;

import org.w3c.dom.Element;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.orm.CrudOrmEngine;
import hd3gtv.tools.XmlData;
import models.ACLGroup;
import models.ACLRole;
import models.ACLUser;
import models.UserProfile;

public class DbAccountExtractor {
	
	public static final DbAccountExtractor extractor;
	
	static {
		extractor = new DbAccountExtractor();
	}
	
	private DbAccountExtractor() {
	}
	
	public void save() {
		try {
			Loggers.Play.info("Start DbAccountExtractor.save");
			
			XmlData xml_data = XmlData.createEmptyDocument();
			
			Element document_element = xml_data.getDocument().createElement("account_extractor");
			
			SelfExtractor self_extractor;
			
			List<ACLUser> user_list = ACLUser.all().fetch();
			for (int pos = 0; pos < user_list.size(); pos++) {
				self_extractor = user_list.get(pos);
				document_element.appendChild(self_extractor.exportToXML(xml_data.getDocument()));
			}
			
			List<ACLGroup> group_list = ACLGroup.all().fetch();
			for (int pos = 0; pos < group_list.size(); pos++) {
				self_extractor = group_list.get(pos);
				document_element.appendChild(self_extractor.exportToXML(xml_data.getDocument()));
			}
			
			List<ACLRole> role_list = ACLRole.all().fetch();
			for (int pos = 0; pos < role_list.size(); pos++) {
				self_extractor = role_list.get(pos);
				document_element.appendChild(self_extractor.exportToXML(xml_data.getDocument()));
			}
			
			CrudOrmEngine<UserProfile> engine = new CrudOrmEngine<UserProfile>(new UserProfile());
			List<UserProfile> userprofile_list = engine.list();
			for (int pos = 0; pos < userprofile_list.size(); pos++) {
				self_extractor = userprofile_list.get(pos);
				document_element.appendChild(self_extractor.exportToXML(xml_data.getDocument()));
			}
			
			List<Authenticator> auths = AuthenticationBackend.getAuthenticators();
			for (int pos = 0; pos < auths.size(); pos++) {
				if (auths.get(pos) instanceof AuthenticatorLocalsqlite) {
					self_extractor = (AuthenticatorLocalsqlite) auths.get(pos);
					document_element.appendChild(self_extractor.exportToXML(xml_data.getDocument()));
				}
			}
			
			xml_data.getDocument().appendChild(document_element);
			
			xml_data.writeToFile(new File(Configuration.getGlobalConfigurationDirectory().getParent() + File.separator + "account_export.xml"));
			
		} catch (Exception e) {
			Loggers.Play.warn("Can't extract accounts", e);
		}
	}
	
}
