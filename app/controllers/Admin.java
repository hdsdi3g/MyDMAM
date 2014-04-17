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

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.db.orm.CrudOrmEngine;
import hd3gtv.mydmam.db.orm.CrudOrmModel;
import hd3gtv.mydmam.db.orm.ModelClassResolver;
import hd3gtv.mydmam.db.orm.ORMFormField;
import hd3gtv.mydmam.db.orm.annotations.AuthorisedForAdminController;
import hd3gtv.mydmam.db.orm.annotations.PublishedMethod;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import play.Play;
import play.data.binding.Binder;
import play.data.binding.ParamNode;
import play.data.binding.ParamNode.RemovedNode;
import play.data.validation.Validation;
import play.exceptions.TemplateNotFoundException;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;
import play.vfs.VirtualFile;

@With(Secure.class)
public class Admin extends Controller {
	
	private static class PlayModelClassResolver implements ModelClassResolver {
		public Class<? extends CrudOrmModel> loadModelClass(String name) throws ClassNotFoundException {
			if (name == null) {
				return null;
			}
			
			if (all_crud_models.containsKey(name) == false) {
				return null;
			}
			
			Class<?> reference = Play.classloader.loadClass(all_crud_models.get(name));
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
		
		/**
		 * simplename -> Package.LongName
		 */
		private static HashMap<String, String> all_crud_models;
		
		static {
			all_crud_models = new HashMap<String, String>();
			try {
				List<String> classes_to_test = new ArrayList<String>();
				
				/**
				 * Play modules
				 */
				File[] module_app_content;
				for (Map.Entry<String, VirtualFile> entry : Play.modules.entrySet()) {
					File module_dir = entry.getValue().getRealFile();
					
					module_app_content = (new File(module_dir.getAbsolutePath() + File.separator + "app" + File.separator + "models")).listFiles(new FilenameFilter() {
						public boolean accept(File arg0, String arg1) {
							return arg1.endsWith(".java");
						}
					});
					if (module_app_content != null) {
						for (int pos = 0; pos < module_app_content.length; pos++) {
							classes_to_test.add(module_app_content[pos].getName());
						}
					}
				}
				
				/**
				 * Classpath modules
				 */
				ArrayList<String> classpathelements = new ArrayList<String>();
				
				String[] classpathelementsstr = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
				
				for (int i = 0; i < classpathelementsstr.length; i++) {
					classpathelements.add(classpathelementsstr[i]);
				}
				
				classpathelements.add((new File("app/models")).getPath());
				
				JarFile jfile;
				JarEntry element;
				FilenameFilter filenamefilter = new FilenameFilter() {
					public boolean accept(File arg0, String arg1) {
						return arg0.getName().equals("models") & (arg1.endsWith(".class") | arg1.endsWith(".java"));
					}
				};
				
				for (int i = 0; i < classpathelements.size(); i++) {
					if (classpathelements.get(i).endsWith(".jar")) {
						try {
							jfile = new JarFile(classpathelements.get(i));
							for (Enumeration<JarEntry> entries = jfile.entries(); entries.hasMoreElements();) {
								element = entries.nextElement();
								if (element.getName().endsWith(".class") & element.getName().startsWith("models")) {
									classes_to_test.add(element.getName());
								}
							}
							jfile.close();
						} catch (IOException e) {
							Log2.log.error("Can't load/open jar file " + classpathelements.get(i), e);
						}
					} else {
						File directoryclass = new File(classpathelements.get(i));
						if (directoryclass.exists() && directoryclass.isDirectory()) {
							File[] list = directoryclass.listFiles(filenamefilter);
							for (int j = 0; j < list.length; j++) {
								classes_to_test.add(list[j].getName());
							}
						}
					}
				}
				
				Class candidate;
				for (int pos_classes = 0; pos_classes < classes_to_test.size(); pos_classes++) {
					try {
						String classname = classes_to_test.get(pos_classes);
						if (classname.endsWith(".java")) {
							classname = classname.substring(0, classname.length() - (".java".length()));
						} else if (classname.endsWith(".class")) {
							classname = classname.substring(0, classname.length() - (".class".length()));
						}
						
						candidate = Class.forName("models." + classname);
						
						if (CrudOrmModel.class.isAssignableFrom(candidate) == false) {
							continue;
						}
						
						all_crud_models.put(classname.toLowerCase(), candidate.getCanonicalName());
					} catch (ClassNotFoundException e) {
						Log2.log.error("Class not found " + classes_to_test.get(pos_classes), e);
					}
				}
			} catch (Exception e) {
				Log2.log.error("Can't load modules", e);
			}
		}
		
	}
	
	private static PlayModelClassResolver playmodelclassresolver = new PlayModelClassResolver();
	
	static String getActionFlashMessage(String objtype, String targetmethod, Throwable e) {
		String message;
		if (e == null) {
			message = Messages.all(play.i18n.Lang.get()).getProperty("crud.field." + objtype + "." + targetmethod + ".done");
		} else {
			message = Messages.all(play.i18n.Lang.get()).getProperty("crud.field." + objtype + "." + targetmethod + ".error");
		}
		if (message == null) {
			if (e == null) {
				message = Messages.all(play.i18n.Lang.get()).getProperty("crud.done");
			} else {
				message = Messages.all(play.i18n.Lang.get()).getProperty("crud.error");
			}
			Log2Dump dump = new Log2Dump();
			dump.add("Done", "crud.field." + objtype + "." + targetmethod + ".done");
			dump.add("Error", "crud.field." + objtype + "." + targetmethod + ".error");
			Log2.log.info("Don't forget to add missing message items", dump);
		}
		
		if (e != null) {
			message = message + " (" + e.getMessage() + ")";
		}
		return message;
	}
	
	static String getIsSavedFlashMessage(String objtype) {
		String message = Messages.all(play.i18n.Lang.get()).getProperty("crud.field." + objtype + ".issaved");
		if (message == null) {
			message = Messages.all(play.i18n.Lang.get()).getProperty("crud.done");
			Log2Dump dump = new Log2Dump();
			dump.add("Done", "crud.field." + objtype + ".issaved");
			Log2.log.info("Don't forget to add missing message items", dump);
		}
		return message;
	}
	
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
			flash.success(getActionFlashMessage(objtype, action, null));
		} catch (InvocationTargetException e) {
			Log2.log.error("Error during remove invoke", e.getTargetException());
			flash.error(getActionFlashMessage(objtype, action, e.getTargetException()));
		} catch (Exception e) {
			Log2.log.error("Error during invoke", e);
			flash.error(getActionFlashMessage(objtype, action, e));
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
