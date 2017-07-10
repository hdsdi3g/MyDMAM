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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.assetsxcross;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import com.avid.interplay.ws.assets.Assets;
import com.avid.interplay.ws.assets.AssetsFault;
import com.avid.interplay.ws.assets.AssetsPortType;
import com.avid.interplay.ws.assets.AttributeConditionType;
import com.avid.interplay.ws.assets.AttributeListType;
import com.avid.interplay.ws.assets.AttributeType;
import com.avid.interplay.ws.assets.ErrorListType;
import com.avid.interplay.ws.assets.ErrorType;
import com.avid.interplay.ws.assets.InterplayURIListType;
import com.avid.interplay.ws.assets.SearchGroupType;
import com.avid.interplay.ws.assets.SearchResponseType;
import com.avid.interplay.ws.assets.SearchType;
import com.avid.interplay.ws.assets.SetAttributesResponseType;
import com.avid.interplay.ws.assets.SetAttributesType;
import com.avid.interplay.ws.assets.UserCredentialsType;

import hd3gtv.configuration.Configuration;

public class InterplayAPI {
	
	public enum AttributeGroup {
		SYSTEM, USER
	}
	
	public enum Condition {
		/** The attribute value must be an exact match */
		EQUALS,
		/** The attribute value can be anything except an exact match */
		NOT_EQUALS,
		/** The search phrase must be contained somewhere in the attribute value */
		CONTAINS,
		/** The search phrase must not be contained anywhere in the attribute value */
		NOT_CONTAINS,
		/** Useful for date-based searches to find matches before the given date and time */
		LESS_THAN,
		/** Like LESS_THAN, but inclusive of the passed in date and time */
		LESS_THAN_OR_EQUAL_TO,
		/** Useful for date-based searches to find matches after the given date and time */
		GREATER_THAN,
		/** Like GREATER_THAN, but inclusive of the passed in date and time */
		GREATER_THAN_OR_EQUAL_TO,
	}
	
	private AssetsPortType assets;
	private UserCredentialsType credentialsHeader;
	private String workgoup;
	
	public InterplayAPI(String host, String user, String password, String workgoup) throws IOException {
		if (host == null) {
			throw new NullPointerException("\"host\" can't to be null");
		}
		if (user == null) {
			throw new NullPointerException("\"user\" can't to be null");
		}
		if (password == null) {
			throw new NullPointerException("\"password\" can't to be null");
		}
		this.workgoup = workgoup;
		if (workgoup == null) {
			throw new NullPointerException("\"workgoup\" can't to be null");
		}
		
		credentialsHeader = new UserCredentialsType();
		credentialsHeader.setUsername(user);
		credentialsHeader.setPassword(password);
		
		URL url = new URL("http", host, "/services/Assets?wsdl");
		QName qname = new QName("http://avid.com/interplay/ws/assets", "Assets");
		
		Service service = Assets.create(url, qname);
		assets = service.getPort(AssetsPortType.class);
	}
	
	public static InterplayAPI initFromConfiguration() throws IOException {
		if (Configuration.global.isElementKeyExists("interplay", "host") == false) {
			return null;
		}
		try {
			return new InterplayAPI(Configuration.global.getValue("interplay", "host", ""), Configuration.global.getValue("interplay", "user", ""), Configuration.global.getValue("interplay", "password", ""), Configuration.global.getValue("interplay", "workgroup", "AvidWorkgroup"));
		} catch (NullPointerException e) {
			return null;
		}
	}
	
	public SearchResponseType search(SearchType search) throws AssetsFault, IOException {
		SearchResponseType response = assets.search(search, credentialsHeader);
		checkError(response.getErrors());
		return response;
	}
	
	public void setAttributes(Collection<AttributeType> attributes, String... interplay_uri_list) throws AssetsFault, IOException {
		if (interplay_uri_list == null) {
			throw new NullPointerException("\"interplay_uri_list\" can't to be null");
		}
		if (interplay_uri_list.length == 0) {
			throw new IndexOutOfBoundsException("\"interplay_uri_list\" can't to be empty");
		}
		
		SetAttributesType set_attributes = new SetAttributesType();
		
		InterplayURIListType uri_list = new InterplayURIListType();
		Arrays.asList(interplay_uri_list).forEach(uri -> {
			if (uri != null) {
				uri_list.getInterplayURI().add(uri);
			}
		});
		set_attributes.setInterplayURIs(uri_list);
		
		AttributeListType attributelist = new AttributeListType();
		attributes.forEach(attr -> {
			attributelist.getAttribute().add(attr);
		});
		set_attributes.setAttributes(attributelist);
		
		SetAttributesResponseType response = assets.setAttributes(set_attributes, credentialsHeader);
		checkError(response.getErrors());
	}
	
	private void checkError(ErrorListType errors) throws IOException {
		if (errors == null) {
			return;
		}
		List<ErrorType> e_type = errors.getError();
		if (e_type == null) {
			return;
		}
		if (e_type.isEmpty()) {
			return;
		}
		/*e_type.forEach(error -> {
			error.getMessage()
		});*/
		throw new IOException(e_type.get(0).getMessage() + ". " + e_type.get(0).getDetails());
	}
	
	public SearchResponseType search(String interplay_path, int max_results, Collection<AttributeType> attributes, Collection<AttributeConditionType> conditions) throws AssetsFault, IOException {
		SearchType search_type = new SearchType();
		
		if (interplay_path.startsWith("/") == false) {
			interplay_path = "/" + interplay_path;
		}
		search_type.setInterplayPathURI("interplay://" + workgoup + interplay_path);
		search_type.setMaxResults(max_results);
		
		AttributeListType ltype = new AttributeListType();
		attributes.forEach(attr -> {
			ltype.getAttribute().add(attr);
		});
		search_type.setReturnAttributes(ltype);
		
		SearchGroupType search_group_type = new SearchGroupType();
		search_group_type.setOperator("AND");
		conditions.forEach(conf -> {
			search_group_type.getAttributeCondition().add(conf);
		});
		search_type.setSearchGroup(search_group_type);
		
		return search(search_type);
	}
	
	public static AttributeType createAttribute(AttributeGroup group, String name, String value) {
		if (group == null) {
			throw new NullPointerException("\"group\" can't to be null");
		}
		if (name == null) {
			throw new NullPointerException("\"name\" can't to be null");
		}
		if (value == null) {
			throw new NullPointerException("\"value\" can't to be null");
		}
		AttributeType attribute_type = new AttributeType();
		attribute_type.setGroup(group.name());
		attribute_type.setName(name);
		attribute_type.setValue(value);
		return attribute_type;
	}
	
	public static AttributeConditionType createAttributeCondition(Condition condition, AttributeGroup group, String name, String value) {
		AttributeConditionType attr_type = new AttributeConditionType();
		attr_type.setCondition(condition.name());
		attr_type.setAttribute(createAttribute(group, name, value));
		return attr_type;
	}
	
	public static String getAttributeValueFromList(AttributeListType attr_list, String attribute_name) {
		return attr_list.getAttribute().stream().filter(attr -> {
			return attr.getName().equalsIgnoreCase(attribute_name);
		}).map(attr -> {
			return attr.getValue();
		}).findFirst().orElse(null);
	}
	
	/*public static String toStringAttributeType(AttributeType attr) {
	return attr.getGroup() + ":" + attr.getName() + ": " + attr.getValue();
	}
	
	public static void toTableAttributeList(AttributeListType attr_list, TableList table, String... attribute_names) {
	List<String> attribute_name_list = new ArrayList<>();
	if (attribute_names != null) {
		if (attribute_names.length > 0) {
			attribute_name_list = Arrays.asList(attribute_names);
		}
	}
	
	Row row = table.createRow();
	
	if (attribute_name_list.isEmpty()) {
		attr_list.getAttribute().stream().map(attr -> {
			return attr.getValue();
		}).forEach(value -> {
			row.addCell(value);
		});
	} else {
		attribute_name_list.forEach(attr_name -> {
			attr_list.getAttribute().stream().filter(attr -> {
				return attr.getName().equalsIgnoreCase(attr_name);
			}).map(attr -> {
				return attr.getValue();
			}).findFirst().ifPresent(attr -> {
				row.addCell(attr);
			});
		});
	}
	}*/
}
