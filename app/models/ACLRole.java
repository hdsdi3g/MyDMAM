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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package models;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import play.data.validation.Required;
import play.db.jpa.GenericModel;

@Entity
public class ACLRole extends GenericModel {
	
	public static final String ADMIN_NAME = "administrator";
	public static final String GUEST_NAME = "guest";
	
	@Id
	@Required
	public String name;
	
	/**
	 * JSON Array
	 */
	@Lob
	public String privileges;
	
	/**
	 * JSON Array
	 */
	@Lob
	public String functionalities;
	
	@OneToMany(mappedBy = "role", cascade = CascadeType.PERSIST)
	public List<ACLGroup> groups;
	
	public ACLRole(String name) {
		this.name = name;
		this.privileges = "[]";
	}
	
	/**
	 * @return never null
	 */
	public List<String> getPrivilegesList() {
		if (privileges == null) {
			return new ArrayList<String>(1);
		}
		if (privileges.trim().equals("")) {
			return new ArrayList<String>(1);
		}
		JSONParser jp = new JSONParser();
		try {
			JSONArray ja = (JSONArray) jp.parse(privileges);
			if (ja.size() == 0) {
				return new ArrayList<String>(1);
			}
			
			ArrayList<String> result = new ArrayList<String>();
			for (Object o : ja) {
				result.add((String) o);
			}
			
			return result;
		} catch (Exception e) {
			Log2.log.error("Can't extract privileges from DB", e, new Log2Dump("raw privileges", privileges));
			return new ArrayList<String>(1);
		}
		
	}
	
	/**
	 * @return never null
	 */
	public List<String> getFunctionalitiesList() {
		if (functionalities == null) {
			return new ArrayList<String>(1);
		}
		if (functionalities.trim().equals("")) {
			return new ArrayList<String>(1);
		}
		JSONParser jp = new JSONParser();
		try {
			JSONArray ja = (JSONArray) jp.parse(functionalities);
			if (ja.size() == 0) {
				return new ArrayList<String>(1);
			}
			
			ArrayList<String> result = new ArrayList<String>();
			for (Object o : ja) {
				result.add((String) o);
			}
			
			return result;
		} catch (Exception e) {
			Log2.log.error("Can't extract functionalities from DB", e, new Log2Dump("raw functionalities", functionalities));
			return new ArrayList<String>(1);
		}
		
	}
	
}
