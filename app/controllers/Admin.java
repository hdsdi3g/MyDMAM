/*
 * This file is part of MyDMAM, inspired by Play CRUD Module
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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package controllers;

import hd3gtv.mydmam.web.CrudOrmEngine;
import hd3gtv.mydmam.web.CrudOrmModel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.persistence.Transient;

import play.Play;
import play.data.binding.Binder;
import play.data.binding.ParamNode;
import play.data.binding.ParamNode.RemovedNode;
import play.data.validation.Email;
import play.data.validation.MaxSize;
import play.data.validation.Password;
import play.data.validation.Validation;
import play.exceptions.TemplateNotFoundException;
import play.i18n.Messages;
import play.mvc.Controller;

public class Admin extends Controller {
	
	private static Class<? extends CrudOrmModel> loadModelClass(String name) throws ClassNotFoundException {
		if (name == null) {
			return null;
		}
		Class<?> reference = Play.classloader.loadClass("models." + name.substring(0, 1).toUpperCase() + name.substring(1));
		if (reference.getAnnotation(AuthorisedForAdminController.class) == null) {
			return null;
		}
		
		try {
			return (Class<? extends CrudOrmModel>) reference;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static CrudOrmEngine<CrudOrmModel> getEngine(Class<?> entityclass) throws Exception {
		Constructor<?> constructor = entityclass.getDeclaredConstructor();
		constructor.setAccessible(true);
		CrudOrmModel object = (CrudOrmModel) constructor.newInstance();
		return new CrudOrmEngine<CrudOrmModel>(object);
	}
	
	public static void index(String objtype) throws Exception {
		Class<? extends CrudOrmModel> entityclass = loadModelClass(objtype);
		notFoundIfNull(entityclass);
		
		String type = entityclass.getSimpleName().toLowerCase();
		
		List<ORMFormField> fields = getFields(entityclass);
		
		CrudOrmEngine<CrudOrmModel> engine = getEngine(entityclass);
		// engine.truncate();
		List<CrudOrmModel> caller = engine.list();
		
		HashMap<String, HashMap<String, String>> fieldspointers = getFieldPointerTable(fields, caller, entityclass);
		
		render("CRUD/list.html", type, caller, fields, fieldspointers);
	}
	
	public static void blank(String objtype) throws Exception {
		Class<?> entityclass = loadModelClass(objtype);
		notFoundIfNull(entityclass);
		
		String type = entityclass.getSimpleName().toLowerCase();
		
		List<ORMFormField> fields = getFields(entityclass);
		
		HashMap<String, HashMap<String, String>> fieldspointers = getFieldPointerTable(fields);
		
		render("CRUD/blank.html", type, fields, entityclass, fieldspointers);
	}
	
	public static void edit(String objtype, String key) throws Exception {
		Class<?> entityclass = loadModelClass(objtype);
		notFoundIfNull(entityclass);
		
		String type = entityclass.getSimpleName().toLowerCase();
		
		CrudOrmEngine<CrudOrmModel> engine = getEngine(entityclass);
		
		CrudOrmModel object = engine.read(key);
		
		List<ORMFormField> fields = getFields(entityclass);
		
		HashMap<String, HashMap<String, String>> fieldspointers = getFieldPointerTable(fields);
		
		render("CRUD/edit.html", type, fields, entityclass, object, fieldspointers);
	}
	
	public static void delete(String objtype, String key) throws Exception {
		Class<?> entityclass = loadModelClass(objtype);
		notFoundIfNull(entityclass);
		
		String type = entityclass.getSimpleName().toLowerCase();
		
		CrudOrmEngine<CrudOrmModel> engine = getEngine(entityclass);
		
		try {
			engine.delete(key);
			flash.success(Messages.get("crud.deleted", type));
			flash.put("deleteunlocked", "true");
		} catch (Exception e) {
			flash.error(Messages.get("crud.delete.error", type));
		}
		
		redirect("Admin.index", objtype);
	}
	
	public static void action(String objtype, String key, String action) throws Exception {
		Class<? extends CrudOrmModel> entityclass = loadModelClass(objtype);
		notFoundIfNull(entityclass);
		
		CrudOrmEngine<CrudOrmModel> engine = getEngine(entityclass);
		
		Method method = entityclass.getMethod(action);
		if (method == null) {
			notFound();
		}
		if (method.isAnnotationPresent(Published.class) == false) {
			notFound();
		}
		CrudOrmModel element = engine.read(key);
		if (element == null) {
			notFound();
		}
		
		try {
			method.invoke(element);
			flash.success("Ok");
		} catch (Exception e) {
			flash.error("Error: " + e.getMessage());
		}
		redirect("Admin.index", objtype);
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface HiddenCompactView {
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface ReadOnly {
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Exclude {
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface AuthorisedForAdminController {
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface PointerTo {
		String value();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface Published {
	}
	
	public static class ORMFormField {
		public String name;
		public String type = "unknown";
		public boolean hidden;
		public boolean readonly;
		public String class_referer = null;
		
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
	}
	
	/**
	 * @return ClassName -> key -> shortName
	 */
	private static HashMap<String, HashMap<String, String>> getFieldPointerTable(List<ORMFormField> fields) throws Exception {
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
			
			Class<?> entityclass = loadModelClass(class_referer);
			CrudOrmEngine<CrudOrmModel> engine = getEngine(entityclass);
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
	private static HashMap<String, HashMap<String, String>> getFieldPointerTable(List<ORMFormField> fields, List<CrudOrmModel> objects, Class<? extends CrudOrmModel> entityclass) throws Exception {
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
				
				entity_pointer_class = loadModelClass(class_referer);
				engine = getEngine(entity_pointer_class);
				
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
	
	private static List<ORMFormField> getFields(Class<?> class_model) throws SecurityException, NoSuchFieldException {
		List<ORMFormField> result = new ArrayList<ORMFormField>();
		Field[] fields = class_model.getFields();
		
		Field field;
		for (int pos = 0; pos < fields.length; pos++) {
			field = fields[pos];
			ORMFormField ormformfield = new ORMFormField();
			
			if (field.isAnnotationPresent(Transient.class)) continue;
			if (field.isAnnotationPresent(Exclude.class)) continue;
			
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
					if (field.isAnnotationPresent(MaxSize.class)) {
						int maxSize = field.getAnnotation(MaxSize.class).value();
						if (maxSize > 100) {
							ormformfield.type = "longtext";
						}
					}
					if (field.isAnnotationPresent(Password.class)) {
						ormformfield.type = "password";
					}
					if (field.isAnnotationPresent(Email.class)) {
						ormformfield.type = "email";
					}
				}
				if (Number.class.isAssignableFrom(field.getType()) || field.getType().equals(double.class) || field.getType().equals(int.class) || field.getType().equals(long.class)) {
					ormformfield.type = "number";
				}
				if (Boolean.class.isAssignableFrom(field.getType()) || field.getType().equals(boolean.class)) {
					ormformfield.type = "boolean";
				}
				if (Date.class.isAssignableFrom(field.getType())) {
					ormformfield.type = "date";
				}
				if (field.getType().isEnum()) {
					ormformfield.type = "enum";
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
			if (method.isAnnotationPresent(Published.class) == false) {
				continue;
			}
			ORMFormField ormformfield = new ORMFormField();
			ormformfield.name = method.getName();
			ormformfield.type = "method";
			result.add(ormformfield);
		}
		
		return result;
	}
	
	public static void create(String objtype) throws Exception {
		Class<?> entityclass = loadModelClass(objtype);
		notFoundIfNull(entityclass);
		String type = entityclass.getSimpleName().toLowerCase();
		
		/**
		 * Protect to ingest readonly fields
		 */
		ParamNode object_param_nodes = params.getRootParamNode().getChild("object");
		notFoundIfNull(object_param_nodes);
		List<ORMFormField> fields = getFields(entityclass);
		List<RemovedNode> rn = new ArrayList<ParamNode.RemovedNode>();
		for (int pos = 0; pos < fields.size(); pos++) {
			if (fields.get(pos).readonly) {
				object_param_nodes.removeChild(fields.get(pos).name, rn);
			}
		}
		
		CrudOrmEngine<CrudOrmModel> engine = getEngine(entityclass);
		CrudOrmModel object = engine.create();
		
		Binder.bindBean(params.getRootParamNode(), "object", object);
		
		validation.valid(object);
		
		if (Validation.hasErrors()) {
			HashMap<String, HashMap<String, String>> fieldspointers = getFieldPointerTable(fields);
			
			renderArgs.put("error", Messages.get("crud.hasErrors"));
			try {
				render("CRUD/blank.html", type, fields, entityclass, object, fieldspointers);
			} catch (TemplateNotFoundException e) {
				e.printStackTrace();
				render("CRUD/blank.html", type, fields, entityclass, fieldspointers);
			}
		}
		
		engine.saveInternalElement();
		
		flash.success(Messages.get("crud.created", type));
		if (params.get("_save") != null) {
			redirect("Admin.index", objtype);
		}
		if (params.get("_saveAndAddAnother") != null) {
			redirect("Admin.blank", objtype);
		}
	}
	
	public static void update(String objtype, String key) throws Exception {
		Class<?> entityclass = loadModelClass(objtype);
		notFoundIfNull(entityclass);
		String type = entityclass.getSimpleName().toLowerCase();
		
		/**
		 * Protect to ingest readonly fields
		 */
		ParamNode object_param_nodes = params.getRootParamNode().getChild("object");
		notFoundIfNull(object_param_nodes);
		List<ORMFormField> fields = getFields(entityclass);
		List<RemovedNode> rn = new ArrayList<ParamNode.RemovedNode>();
		for (int pos = 0; pos < fields.size(); pos++) {
			if (fields.get(pos).readonly) {
				object_param_nodes.removeChild(fields.get(pos).name, rn);
			}
		}
		
		CrudOrmEngine<CrudOrmModel> engine = getEngine(entityclass);
		CrudOrmModel reference = engine.read(key);
		if (reference == null) {
			notFound();
		}
		CrudOrmModel object = engine.create();
		object.key = key;
		object.createdate = reference.createdate;
		
		Binder.bindBean(params.getRootParamNode(), "object", object);
		
		validation.valid(object);
		if (Validation.hasErrors()) {
			HashMap<String, HashMap<String, String>> fieldspointers = getFieldPointerTable(fields);
			
			renderArgs.put("error", Messages.get("crud.hasErrors"));
			try {
				render("CRUD/edit.html", type, fields, entityclass, object, fieldspointers);
			} catch (TemplateNotFoundException e) {
				e.printStackTrace();
				render("CRUD/show.html", type, object, fieldspointers);
			}
		}
		engine.saveInternalElement();
		
		flash.success(Messages.get("crud.saved", type));
		redirect("Admin.index", objtype);
	}
}
