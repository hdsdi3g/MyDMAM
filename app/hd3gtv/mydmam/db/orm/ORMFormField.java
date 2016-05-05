/*
 * This file is part of MyDMAM
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/

package hd3gtv.mydmam.db.orm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.persistence.Transient;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import hd3gtv.mydmam.db.orm.annotations.ExcludeForView;
import hd3gtv.mydmam.db.orm.annotations.HiddenCompactView;
import hd3gtv.mydmam.db.orm.annotations.PointerTo;
import hd3gtv.mydmam.db.orm.annotations.PublishedMethod;
import hd3gtv.mydmam.db.orm.annotations.ReadOnly;
import hd3gtv.mydmam.db.orm.annotations.TypeEmail;
import hd3gtv.mydmam.db.orm.annotations.TypeLongText;
import hd3gtv.mydmam.db.orm.annotations.TypeNavigatorInputSelection;
import hd3gtv.mydmam.db.orm.annotations.TypePassword;

/**
 * @deprecated
 */
public class ORMFormField {
	
	private static Gson gson = new Gson();
	public static final Type TYPE_AL_FIELDS = new TypeToken<ArrayList<ORMFormField>>() {
	}.getType();
	
	ORMFormField() {
	}
	
	public String name;
	public String type = "unknown";
	public boolean hidden;
	public boolean readonly;
	public String class_referer = null;
	public Object options = null;
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(name);
		sb.append(" (");
		sb.append(type);
		sb.append(") ");
		sb.append("\tRO:");
		sb.append(readonly);
		if (class_referer != null) {
			sb.append(" => ");
			sb.append(class_referer);
		}
		return sb.toString();
	}
	
	public static JsonElement getJsonFields(List<ORMFormField> fields) {
		return gson.toJsonTree(fields, TYPE_AL_FIELDS);
	}
	
	/**
	 * Get usable fields for an user view.
	 */
	public static List<ORMFormField> getFields(Class<?> class_model) throws SecurityException, NoSuchFieldException {
		List<ORMFormField> result = new ArrayList<ORMFormField>();
		Field[] fields = class_model.getFields();
		
		Field field;
		for (int pos = 0; pos < fields.length; pos++) {
			field = fields[pos];
			ORMFormField ormformfield = new ORMFormField();
			
			if (field.isAnnotationPresent(Transient.class)) continue;
			if (field.isAnnotationPresent(ExcludeForView.class)) continue;
			
			int mod = field.getModifiers();
			if ((mod & Modifier.PROTECTED) != 0) continue;
			if ((mod & Modifier.PRIVATE) != 0) continue;
			if ((mod & Modifier.ABSTRACT) != 0) continue;
			if ((mod & Modifier.STATIC) != 0) continue;
			if ((mod & Modifier.FINAL) != 0) continue;
			if ((mod & Modifier.TRANSIENT) != 0) continue;
			if ((mod & Modifier.INTERFACE) != 0) continue;
			
			ormformfield.name = field.getName();
			
			if (ormformfield.name.equals("key")) {
				continue;
			}
			
			if (field.isAnnotationPresent(PointerTo.class)) {
				ormformfield.class_referer = field.getAnnotation(PointerTo.class).value().toLowerCase();
				if (CharSequence.class.isAssignableFrom(field.getType())) {
					ormformfield.type = "pointer";
				} else if (String[].class.isAssignableFrom(field.getType())) {
					ormformfield.type = "pointerlist";
				} else {
					continue;
				}
			} else {
				if (CharSequence.class.isAssignableFrom(field.getType())) {
					ormformfield.type = "text";
					if (field.isAnnotationPresent(TypeLongText.class)) {
						ormformfield.type = "longtext";
					} else if (field.isAnnotationPresent(TypePassword.class)) {
						ormformfield.type = "password";
					} else if (field.isAnnotationPresent(TypeEmail.class)) {
						ormformfield.type = "email";
					}
				}
				if (Number.class.isAssignableFrom(field.getType()) || field.getType().equals(double.class) || field.getType().equals(int.class) || field.getType().equals(long.class)) {
					ormformfield.type = "number";
				} else if (Boolean.class.isAssignableFrom(field.getType()) || field.getType().equals(boolean.class)) {
					ormformfield.type = "boolean";
				} else if (Date.class.isAssignableFrom(field.getType())) {
					ormformfield.type = "date";
				} else if (field.isAnnotationPresent(TypeNavigatorInputSelection.class)) {
					ormformfield.type = "navigatorinputselection";
					TypeNavigatorInputSelection field_conf = field.getAnnotation(TypeNavigatorInputSelection.class);
					HashMap<String, Object> conf = new HashMap<String, Object>(4);
					conf.put("canselectdirs", field_conf.canselectdirs());
					conf.put("canselectfiles", field_conf.canselectfiles());
					conf.put("canselectstorages", field_conf.canselectstorages());
					if (field_conf.placeholderlabel() != null) {
						if (field_conf.placeholderlabel().equals("") == false) {
							conf.put("placeholder", field_conf.placeholderlabel());
						}
					}
					ormformfield.options = conf;
				} else if (field.getType().isEnum()) {
					ormformfield.type = "enum";
					HashMap<String, Object> conf = new HashMap<String, Object>(4);
					conf.put("multiple", false);
					ormformfield.options = conf;
					ormformfield.class_referer = field.getType().getName();
				}
			}
			
			ormformfield.readonly = field.isAnnotationPresent(ReadOnly.class);
			ormformfield.hidden = field.isAnnotationPresent(HiddenCompactView.class);
			result.add(ormformfield);
		}
		
		Method[] methods = class_model.getMethods();
		
		Method method;
		for (int pos = 0; pos < methods.length; pos++) {
			method = methods[pos];
			if (method.isAnnotationPresent(PublishedMethod.class) == false) {
				continue;
			}
			ORMFormField ormformfield = new ORMFormField();
			ormformfield.name = method.getName();
			ormformfield.type = "method";
			result.add(ormformfield);
		}
		
		return result;
	}
	
	public static List<ORMFormField> removeFields(List<ORMFormField> fields, String... fieldname) {
		if (fields == null) {
			return new ArrayList<ORMFormField>(1);
		}
		if (fields.isEmpty()) {
			return new ArrayList<ORMFormField>(1);
		}
		List<ORMFormField> result = new ArrayList<ORMFormField>(fields.size());
		
		ORMFormField field;
		boolean add;
		for (int pos_f = 0; pos_f < fields.size(); pos_f++) {
			field = fields.get(pos_f);
			add = true;
			for (int pos_fn = 0; pos_fn < fieldname.length; pos_fn++) {
				if (field.name.equalsIgnoreCase(fieldname[pos_fn])) {
					add = false;
					break;
				}
			}
			if (add) {
				result.add(field);
			}
		}
		return result;
	}
	
	/**
	 * @return ClassName -> key -> shortName
	 */
	public static HashMap<String, HashMap<String, String>> getFieldPointerTable(ModelClassResolver classresolver, List<ORMFormField> fields) throws Exception {
		HashMap<String, HashMap<String, String>> map = new HashMap<String, HashMap<String, String>>();
		
		String class_referer;
		for (int pos = 0; pos < fields.size(); pos++) {
			if (fields.get(pos).class_referer == null) {
				/**
				 * No class to add
				 */
				continue;
			}
			if (map.get(fields.get(pos).class_referer) != null) {
				/**
				 * Not need to add class (it's done)
				 */
				continue;
			}
			class_referer = fields.get(pos).class_referer;
			HashMap<String, String> element_map = new HashMap<String, String>();
			
			Class<?> entityclass = classresolver.loadModelClass(class_referer);
			CrudOrmEngine<CrudOrmModel> engine = CrudOrmEngine.get(entityclass);
			List<CrudOrmModel> referer_elements = engine.list();
			
			for (int pos_re = 0; pos_re < referer_elements.size(); pos_re++) {
				element_map.put(referer_elements.get(pos_re).key, referer_elements.get(pos_re).shortName());
			}
			
			map.put(class_referer.toLowerCase(), element_map);
		}
		return map;
	}
	
	/**
	 * Get only elements pointed in objects
	 * @return ClassName -> key -> shortName
	 */
	public static HashMap<String, HashMap<String, String>> getFieldPointerTable(ModelClassResolver classresolver, List<ORMFormField> fields, List<CrudOrmModel> objects,
			Class<? extends CrudOrmModel> entityclass) throws Exception {
		HashMap<String, HashMap<String, String>> map = new HashMap<String, HashMap<String, String>>();
		
		if (objects == null) {
			return map;
		}
		
		/**
		 * Bypass function if no need to go deeper.
		 */
		boolean can_continue = false;
		for (int pos = 0; pos < fields.size(); pos++) {
			if (fields.get(pos).class_referer != null) {
				can_continue = true;
				break;
			}
		}
		if (can_continue == false) {
			return map;
		}
		
		CrudOrmModel currentobj;
		String fieldname;
		HashMap<String, String> current_pointer_table;
		String key;
		String[] key_list;
		String class_referer;
		String type;
		CrudOrmEngine<CrudOrmModel> engine;
		CrudOrmModel referer_element;
		Class<? extends CrudOrmModel> entity_pointer_class;
		
		for (int posobj = 0; posobj < objects.size(); posobj++) {
			currentobj = objects.get(posobj);
			
			for (int pos = 0; pos < fields.size(); pos++) {
				class_referer = fields.get(pos).class_referer;
				if (class_referer == null) {
					/**
					 * No class to add
					 */
					continue;
				}
				
				current_pointer_table = map.get(class_referer);
				if (current_pointer_table == null) {
					current_pointer_table = new HashMap<String, String>();
					map.put(class_referer, current_pointer_table);
				}
				
				entity_pointer_class = classresolver.loadModelClass(class_referer);
				engine = CrudOrmEngine.get(entity_pointer_class);
				
				fieldname = fields.get(pos).name;
				type = fields.get(pos).type;
				
				key = null;
				key_list = null;
				
				if (type.equals("pointer")) {
					key = (String) entityclass.getField(fieldname).get(currentobj);
					if (key != null) {
						if (current_pointer_table.get(key) != null) {
							continue;
						}
						referer_element = engine.read(key);
						if (referer_element != null) {
							current_pointer_table.put(key, referer_element.shortName());
						}
					}
				} else if (type.equals("pointerlist")) {
					key_list = (String[]) entityclass.getField(fieldname).get(currentobj);
					if (key_list != null) {
						for (int pos_kl = 0; pos_kl < key_list.length; pos_kl++) {
							key = key_list[pos_kl];
							if (current_pointer_table.get(key) != null) {
								continue;
							}
							referer_element = engine.read(key);
							if (referer_element != null) {
								current_pointer_table.put(key, referer_element.shortName());
							}
						}
					}
				}
				
			}
		}
		
		return map;
	}
	
}
