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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import com.avid.interplay.ws.assets.AssetCategoriesType;
import com.avid.interplay.ws.assets.Assets;
import com.avid.interplay.ws.assets.AssetsFault;
import com.avid.interplay.ws.assets.AssetsPortType;
import com.avid.interplay.ws.assets.AttributeConditionType;
import com.avid.interplay.ws.assets.AttributeListType;
import com.avid.interplay.ws.assets.AttributeType;
import com.avid.interplay.ws.assets.CategoriesListType;
import com.avid.interplay.ws.assets.CreateFolderResponseType;
import com.avid.interplay.ws.assets.CreateFolderType;
import com.avid.interplay.ws.assets.DeleteAssetsResponseType;
import com.avid.interplay.ws.assets.DeleteAssetsType;
import com.avid.interplay.ws.assets.ErrorListType;
import com.avid.interplay.ws.assets.ErrorType;
import com.avid.interplay.ws.assets.FileLocationDetailsType;
import com.avid.interplay.ws.assets.FileLocationType;
import com.avid.interplay.ws.assets.FindLinksResponseType;
import com.avid.interplay.ws.assets.FindLinksType;
import com.avid.interplay.ws.assets.FindRelativesResponseType;
import com.avid.interplay.ws.assets.FindRelativesType;
import com.avid.interplay.ws.assets.GetAttributesResponseType;
import com.avid.interplay.ws.assets.GetAttributesType;
import com.avid.interplay.ws.assets.GetCategoriesResponseType;
import com.avid.interplay.ws.assets.GetCategoriesType;
import com.avid.interplay.ws.assets.GetFileDetailsResponseType;
import com.avid.interplay.ws.assets.GetFileDetailsType;
import com.avid.interplay.ws.assets.GetResolutionsResponseType;
import com.avid.interplay.ws.assets.GetResolutionsType;
import com.avid.interplay.ws.assets.InterplayURIListType;
import com.avid.interplay.ws.assets.LinkToMOBResponseType;
import com.avid.interplay.ws.assets.LinkToMOBType;
import com.avid.interplay.ws.assets.MediaDetailsType;
import com.avid.interplay.ws.assets.SearchGroupType;
import com.avid.interplay.ws.assets.SearchResponseType;
import com.avid.interplay.ws.assets.SearchType;
import com.avid.interplay.ws.assets.SetAttributesResponseType;
import com.avid.interplay.ws.assets.SetAttributesType;
import com.avid.interplay.ws.assets.SetCategoriesResponseType;
import com.avid.interplay.ws.assets.SetCategoriesType;
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
	
	public enum MediaStatus {
		online, offline;
	}
	
	public enum AssetType {
		sequence, masterclip, motioneffect, renderedeffect, subclip;
	}
	
	private AssetsPortType assets;
	private UserCredentialsType credentialsHeader;
	private String workgoup;
	private String mydmam_id_in_interplay;
	private transient List<String> declared_workgoup_categories;
	
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
	
	public InterplayAPI setMydmamIDinInterplay(String mydmam_id_in_interplay) {
		this.mydmam_id_in_interplay = mydmam_id_in_interplay;
		if (mydmam_id_in_interplay == null) {
			throw new NullPointerException("\"mydmam_id_in_interplay\" can't to be null");
		}
		return this;
	}
	
	public static InterplayAPI initFromConfiguration() throws IOException {
		if (Configuration.global.isElementKeyExists("interplay", "host") == false) {
			return null;
		}
		try {
			return new InterplayAPI(Configuration.global.getValue("interplay", "host", ""), Configuration.global.getValue("interplay", "user", ""), Configuration.global.getValue("interplay", "password", ""), Configuration.global.getValue("interplay", "workgroup", "AvidWorkgroup")).setMydmamIDinInterplay(Configuration.global.getValue("interplay", "mydmam_id_in_interplay", "Video ID"));
		} catch (NullPointerException e) {
			return null;
		}
	}
	
	public String getMydmamIDinInterplay() {
		return mydmam_id_in_interplay;
	}
	
	public String createURLInterplayMobid(String mob_id) {
		return "interplay://" + workgoup + "?mobid=" + mob_id.toLowerCase();
	}
	
	public String createURLInterplayPath(String path) {
		if (path.startsWith("/") == false) {
			path = "/" + path;
		}
		return "interplay://" + workgoup + path;
	}
	
	/**
	 * Remove media files... but not references in Interplay... "Manual" purge with Access is better !
	 */
	@Deprecated
	public List<MediaDetailsType> deleteMediasOnly(String... path_list) throws AssetsFault, IOException {
		/*String mob_id = "00000-000";
		InterplayAPI interplay = InterplayAPI.initFromConfiguration();
		String interplay_mob_uri = interplay.createURLInterplayMobid(mob_id);
		
		Map<String, List<AttributeType>> attr = interplay.getAttributes(interplay_mob_uri);
		Map<String, String> attr_map = InterplayAPI.getSimpleAttributeMap(attr.get(interplay_mob_uri));
		
		String interplay_path = attr_map.get("Path");
		interplay.deleteMediasOnly(interplay_path);
		
		List<String> g_r = interplay.getResolutions(interplay_mob_uri);
		System.out.println(MyDMAM.gson_kit.getGsonPretty().toJson(g_r));
		System.out.println(MyDMAM.gson_kit.getGsonPretty().toJson(interplay.getFileDetailsByURI(interplay_mob_uri)));
		// System.out.println(MyDMAM.gson_kit.getGsonPretty().toJson(InterplayAPI.getSimpleAttributeMap(attr.get(interplay_mob_uri))));
		
		String source_id = attr_map.get("Source ID");
		System.out.println(source_id);*/
		
		if (path_list == null) {
			throw new NullPointerException("\"path_list\" can't to be null");
		}
		if (path_list.length == 0) {
			throw new IndexOutOfBoundsException("\"path_list\" can't to be empty");
		}
		
		InterplayURIListType uri_list = new InterplayURIListType();
		Arrays.asList(path_list).forEach(path -> {
			if (path != null) {
				uri_list.getInterplayURI().add(createURLInterplayPath(path));
			}
		});
		
		DeleteAssetsType body = new DeleteAssetsType();
		body.setDeleteMedia(true);
		body.setDeleteMetadata(false);
		// body.setSimulation(false);
		body.setInterplayURIs(uri_list);
		
		// ResolutionListType value = new ResolutionListType();
		// value.getResolution().add(e)
		// body.setResolutions(value);
		
		DeleteAssetsResponseType response = assets.deleteAssets(body, credentialsHeader);
		checkError(response.getErrors());
		return response.getDeletedMedia().getMediaDetails();
	}
	
	/**
	 * @return like ["PCM", "XDCAM-HD 50mbps 1080i 50"]
	 */
	public List<String> getResolutions(String... interplay_uri_list) throws AssetsFault, IOException {
		GetResolutionsType body = new GetResolutionsType();
		body.setOnlineResolutionsOnly(false);
		body.setInterplayURIs(getInterplayURIList(interplay_uri_list));
		body.setReportDetails(false);
		GetResolutionsResponseType response = assets.getResolutions(body, credentialsHeader);
		checkError(response.getErrors());
		return response.getSummary().getResolution();
	}
	
	public Map<String, List<FileLocationType>> getFileDetailsByURI(String... interplay_uri_list) throws AssetsFault, IOException {
		GetFileDetailsType body = new GetFileDetailsType();
		body.setInterplayURIs(getInterplayURIList(interplay_uri_list));
		GetFileDetailsResponseType response = assets.getFileDetails(body, credentialsHeader);
		checkError(response.getErrors());
		
		return response.getResults().getFileLocationDetails().stream().collect(Collectors.toMap(r -> {
			return ((FileLocationDetailsType) r).getInterplayURI();
		}, r -> {
			return ((FileLocationDetailsType) r).getFileLocations().getFileLocation();
		}));
	}
	
	public List<InterplayAsset> convertSearchResponseToAssetList(SearchResponseType response) {
		return response.getResults().getAssetDescription().stream().map(ad -> {
			return new InterplayAsset(this, ad.getInterplayURI(), ad.getAttributes().getAttribute());
		}).collect(Collectors.toList());
	}
	
	public List<InterplayAsset> getAttributes(String... interplay_uri_list) throws AssetsFault, IOException {
		return getAttributes(Arrays.asList(interplay_uri_list));
	}
	
	public List<InterplayAsset> getAttributes(Collection<String> interplay_uri_list) throws AssetsFault, IOException {
		GetAttributesType body = new GetAttributesType();
		// body.setAllAttributes(true);
		
		InterplayURIListType uri_list = new InterplayURIListType();
		interplay_uri_list.forEach(uri -> {
			if (uri != null) {
				uri_list.getInterplayURI().add(uri);
			}
		});
		
		body.setInterplayURIs(uri_list);
		GetAttributesResponseType response = assets.getAttributes(body, credentialsHeader);
		checkError(response.getErrors());
		
		return response.getResults().getAssetDescription().stream().map(ad -> {
			return new InterplayAsset(this, ad.getInterplayURI(), ad.getAttributes().getAttribute());
		}).collect(Collectors.toList());
	}
	
	private static InterplayURIListType getInterplayURIList(String... interplay_uri_list) {
		if (interplay_uri_list == null) {
			throw new NullPointerException("\"interplay_uri_list\" can't to be null");
		}
		if (interplay_uri_list.length == 0) {
			throw new IndexOutOfBoundsException("\"interplay_uri_list\" can't to be empty");
		}
		
		InterplayURIListType uri_list = new InterplayURIListType();
		Arrays.asList(interplay_uri_list).forEach(uri -> {
			if (uri != null) {
				uri_list.getInterplayURI().add(uri);
			}
		});
		return uri_list;
	}
	
	public void setAttributes(Collection<AttributeType> attributes, String... interplay_uri_list) throws AssetsFault, IOException {
		
		SetAttributesType set_attributes = new SetAttributesType();
		set_attributes.setInterplayURIs(getInterplayURIList(interplay_uri_list));
		
		AttributeListType attributelist = new AttributeListType();
		attributes.forEach(attr -> {
			attributelist.getAttribute().add(attr);
		});
		set_attributes.setAttributes(attributelist);
		
		SetAttributesResponseType response = assets.setAttributes(set_attributes, credentialsHeader);
		checkError(response.getErrors());
	}
	
	public List<InterplayAsset> getRelatives(String interplay_uri, Collection<AttributeType> attributes) throws AssetsFault, IOException {
		FindRelativesType find_relatives = new FindRelativesType();
		find_relatives.setInterplayURI(interplay_uri);
		
		AttributeListType ltype = new AttributeListType();
		attributes.forEach(attr -> {
			ltype.getAttribute().add(attr);
		});
		find_relatives.setReturnAttributes(ltype);
		
		FindRelativesResponseType response = assets.findRelatives(find_relatives, credentialsHeader);
		checkError(response.getErrors());
		
		return response.getResults().getAssetDescription().stream().map(ad -> {
			return new InterplayAsset(this, ad.getInterplayURI(), ad.getAttributes().getAttribute());
		}).collect(Collectors.toList());
	}
	
	/**
	 * @return list of interplay_uri
	 */
	public List<String> findLinks(String interplay_uri) throws AssetsFault, IOException {
		FindLinksType find_links = new FindLinksType();
		find_links.setInterplayURI(interplay_uri);
		FindLinksResponseType response = assets.findLinks(find_links, credentialsHeader);
		checkError(response.getErrors());
		return response.getResults().getInterplayURI();
	}
	
	/**
	 * @param new_path If parent folders do not yet exist, they will be created as well.
	 */
	public void createFolder(String new_path/*, String owner*/) throws AssetsFault, IOException {
		CreateFolderType create_folder = new CreateFolderType();
		// create_folder.setOwner(value);
		create_folder.setInterplayURI(createURLInterplayPath(new_path));
		
		CreateFolderResponseType response = assets.createFolder(create_folder, credentialsHeader);
		checkError(response.getErrors());
	}
	
	public void link(String uri_interplay, String new_path) throws AssetsFault, IOException {
		LinkToMOBType link = new LinkToMOBType();
		link.setInterplayMOBURI(uri_interplay);
		link.setInterplayPathURI(createURLInterplayPath(new_path));
		
		LinkToMOBResponseType response = assets.linkToMOB(link, credentialsHeader);
		checkError(response.getErrors());
	}
	
	/**
	 * @return URI -> [cat1, cat2]
	 */
	public Map<String, List<String>> getCategories(String... interplay_uri_list) throws AssetsFault, IOException {
		GetCategoriesType get_cat = new GetCategoriesType();
		get_cat.setInterplayURIs(getInterplayURIList(interplay_uri_list));
		
		GetCategoriesResponseType response = assets.getCategories(get_cat, credentialsHeader);
		checkError(response.getErrors());
		
		return response.getAssetCategories().getAssetCategories().stream().collect(Collectors.toMap(asset_cat -> {
			return ((AssetCategoriesType) asset_cat).getInterplayURI();
		}, asset_cat -> {
			return ((AssetCategoriesType) asset_cat).getCategories().getCategory();
		}));
	}
	
	/**
	 * @return [cat1, cat2]
	 */
	public List<String> getDeclaredWorkgoupCategories() throws AssetsFault, IOException {
		if (declared_workgoup_categories == null) {
			GetCategoriesType get_cat = new GetCategoriesType();
			get_cat.setWorkgroupURI(createURLInterplayPath("/"));
			GetCategoriesResponseType response = assets.getCategories(get_cat, credentialsHeader);
			checkError(response.getErrors());
			declared_workgoup_categories = response.getConfiguredCategories().getCategory();
		}
		
		return declared_workgoup_categories;
	}
	
	/**
	 * One-pass update
	 */
	public void setCategories(String interplay_uri, Collection<String> actual_categories, Collection<String> new_categories) throws AssetsFault, IOException {
		/** Check declared categories */
		if (getDeclaredWorkgoupCategories().containsAll(new_categories) == false) {
			String bad_cats = new_categories.stream().filter(cat -> {
				return declared_workgoup_categories.contains(cat) == false;
			}).collect(Collectors.joining(", "));
			
			throw new IOException("Unknown categorie(s) \"" + bad_cats + "\" in Interplay workgroup " + workgoup);
		}
		
		SetCategoriesType set_cat = new SetCategoriesType();
		
		CategoriesListType add_clt = new CategoriesListType();
		add_clt.getCategory().addAll(new_categories.stream().filter(new_cat -> {
			return actual_categories.contains(new_cat) == false;
		}).collect(Collectors.toList()));
		set_cat.setCategoriesToAdd(add_clt);
		
		CategoriesListType del_clt = new CategoriesListType();
		del_clt.getCategory().addAll(actual_categories.stream().filter(old_cat -> {
			return new_categories.contains(old_cat) == false;
		}).collect(Collectors.toList()));
		set_cat.setCategoriesToRemove(del_clt);
		
		set_cat.setInterplayURI(interplay_uri);
		
		SetCategoriesResponseType response = assets.setCategories(set_cat, credentialsHeader);
		checkError(response.getErrors());
	}
	
	/**
	 * Two-pass get&update
	 */
	public void setCategories(String interplay_uri, Collection<String> new_categories) throws AssetsFault, IOException {
		Collection<String> actual_categories = getCategories(interplay_uri).getOrDefault(interplay_uri, new ArrayList<>());
		setCategories(interplay_uri, actual_categories, new_categories);
	}
	
	static void checkError(ErrorListType errors) throws IOException {
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
	
	public SearchResponseType search(SearchType search) throws AssetsFault, IOException {
		SearchResponseType response = assets.search(search, credentialsHeader);
		checkError(response.getErrors());
		return response;
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
