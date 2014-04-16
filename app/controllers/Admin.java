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

import hd3gtv.mydmam.db.orm.CrudOrmEngine;
import hd3gtv.mydmam.db.orm.CrudOrmModel;
import hd3gtv.mydmam.db.orm.ModelClassResolver;
import hd3gtv.mydmam.db.orm.ORMFormField;
import hd3gtv.mydmam.db.orm.annotations.AuthorisedForAdminController;
import hd3gtv.mydmam.db.orm.annotations.PublishedMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import play.Play;
import play.data.binding.Binder;
import play.data.binding.ParamNode;
import play.data.binding.ParamNode.RemovedNode;
import play.data.validation.Validation;
import play.exceptions.TemplateNotFoundException;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;

@With(Secure.class)
public class Admin extends Controller {
	
	private static class PlayModelClassResolver implements ModelClassResolver {
		public Class<? extends CrudOrmModel> loadModelClass(String name) throws ClassNotFoundException {
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
	}
	
	private static PlayModelClassResolver playmodelclassresolver = new PlayModelClassResolver();
	
	@Check("adminCrud")
	public static void index(String objtype) throws Exception {
		Class<? extends CrudOrmModel> entityclass = playmodelclassresolver.loadModelClass(objtype);
		notFoundIfNull(entityclass);
		
		String type = entityclass.getSimpleName().toLowerCase();
		
		List<ORMFormField> fields = ORMFormField.getFields(entityclass);
		
		CrudOrmEngine<CrudOrmModel> engine = CrudOrmEngine.get(entityclass);
		// engine.truncate();
		List<CrudOrmModel> caller = engine.list();
		
		HashMap<String, HashMap<String, String>> fieldspointers = ORMFormField.getFieldPointerTable(playmodelclassresolver, fields, caller, entityclass);
		
		render("CRUD/list.html", type, caller, fields, fieldspointers);
	}
	
	@Check("adminCrud")
	public static void blank(String objtype) throws Exception {
		Class<?> entityclass = playmodelclassresolver.loadModelClass(objtype);
		notFoundIfNull(entityclass);
		
		String type = entityclass.getSimpleName().toLowerCase();
		
		List<ORMFormField> fields = ORMFormField.getFields(entityclass);
		
		HashMap<String, HashMap<String, String>> fieldspointers = ORMFormField.getFieldPointerTable(playmodelclassresolver, fields);
		
		render("CRUD/blank.html", type, fields, entityclass, fieldspointers);
	}
	
	@Check("adminCrud")
	public static void edit(String objtype, String key) throws Exception {
		Class<?> entityclass = playmodelclassresolver.loadModelClass(objtype);
		notFoundIfNull(entityclass);
		
		String type = entityclass.getSimpleName().toLowerCase();
		
		CrudOrmEngine<CrudOrmModel> engine = CrudOrmEngine.get(entityclass);
		
		CrudOrmModel object = engine.read(key);
		
		List<ORMFormField> fields = ORMFormField.getFields(entityclass);
		
		HashMap<String, HashMap<String, String>> fieldspointers = ORMFormField.getFieldPointerTable(playmodelclassresolver, fields);
		
		render("CRUD/edit.html", type, fields, entityclass, object, fieldspointers);
	}
	
	@Check("adminCrud")
	public static void delete(String objtype, String key) throws Exception {
		Class<?> entityclass = playmodelclassresolver.loadModelClass(objtype);
		notFoundIfNull(entityclass);
		
		String type = entityclass.getSimpleName().toLowerCase();
		
		CrudOrmEngine<CrudOrmModel> engine = CrudOrmEngine.get(entityclass);
		
		try {
			engine.delete(key);
			flash.success(Messages.get("crud.deleted", type));
			flash.put("deleteunlocked", "true");
		} catch (Exception e) {
			flash.error(Messages.get("crud.delete.error", type));
		}
		
		redirect("Admin.index", objtype);
	}
	
	@Check("adminCrud")
	public static void action(String objtype, String key, String action) throws Exception {
		Class<? extends CrudOrmModel> entityclass = playmodelclassresolver.loadModelClass(objtype);
		notFoundIfNull(entityclass);
		
		CrudOrmEngine<CrudOrmModel> engine = CrudOrmEngine.get(entityclass);
		
		Method method = entityclass.getMethod(action);
		if (method == null) {
			notFound();
		}
		if (method.isAnnotationPresent(PublishedMethod.class) == false) {
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
	
	@Check("adminCrud")
	public static void create(String objtype) throws Exception {
		Class<?> entityclass = playmodelclassresolver.loadModelClass(objtype);
		notFoundIfNull(entityclass);
		String type = entityclass.getSimpleName().toLowerCase();
		
		/**
		 * Protect to ingest readonly fields
		 */
		ParamNode object_param_nodes = params.getRootParamNode().getChild("object");
		notFoundIfNull(object_param_nodes);
		List<ORMFormField> fields = ORMFormField.getFields(entityclass);
		List<RemovedNode> rn = new ArrayList<ParamNode.RemovedNode>();
		for (int pos = 0; pos < fields.size(); pos++) {
			if (fields.get(pos).readonly) {
				object_param_nodes.removeChild(fields.get(pos).name, rn);
			}
		}
		
		CrudOrmEngine<CrudOrmModel> engine = CrudOrmEngine.get(entityclass);
		CrudOrmModel object = engine.create();
		
		Binder.bindBean(params.getRootParamNode(), "object", object);
		
		validation.valid(object);
		
		if (Validation.hasErrors()) {
			HashMap<String, HashMap<String, String>> fieldspointers = ORMFormField.getFieldPointerTable(playmodelclassresolver, fields);
			
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
	
	@Check("adminCrud")
	public static void update(String objtype, String key) throws Exception {
		Class<?> entityclass = playmodelclassresolver.loadModelClass(objtype);
		notFoundIfNull(entityclass);
		String type = entityclass.getSimpleName().toLowerCase();
		
		/**
		 * Protect to ingest readonly fields
		 */
		ParamNode object_param_nodes = params.getRootParamNode().getChild("object");
		notFoundIfNull(object_param_nodes);
		List<ORMFormField> fields = ORMFormField.getFields(entityclass);
		List<RemovedNode> rn = new ArrayList<ParamNode.RemovedNode>();
		for (int pos = 0; pos < fields.size(); pos++) {
			if (fields.get(pos).readonly) {
				object_param_nodes.removeChild(fields.get(pos).name, rn);
			}
		}
		
		CrudOrmEngine<CrudOrmModel> engine = CrudOrmEngine.get(entityclass);
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
			HashMap<String, HashMap<String, String>> fieldspointers = ORMFormField.getFieldPointerTable(playmodelclassresolver, fields);
			
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
