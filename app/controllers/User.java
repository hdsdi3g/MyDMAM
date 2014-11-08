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

package controllers;

import hd3gtv.mydmam.db.orm.CrudOrmEngine;
import hd3gtv.mydmam.db.orm.CrudOrmModel;
import hd3gtv.mydmam.db.orm.ModelClassResolver;
import hd3gtv.mydmam.db.orm.ORMFormField;
import hd3gtv.mydmam.db.orm.annotations.PublishedMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import models.UserProfile;
import play.data.binding.Binder;
import play.data.binding.ParamNode;
import play.data.binding.ParamNode.RemovedNode;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.jobs.JobsPlugin;
import play.mvc.Controller;
import play.mvc.With;

@With(Secure.class)
public class User extends Controller {
	
	private static class UserModelClassResolver implements ModelClassResolver {
		public Class<? extends CrudOrmModel> loadModelClass(String name) throws ClassNotFoundException {
			if (name == null) {
				return null;
			}
			if (name.equalsIgnoreCase("userprofile")) {
				return UserProfile.class;
			} else {
				throw new ClassNotFoundException(name);
			}
		}
	}
	
	private static UserModelClassResolver usermodelclassresolver = new UserModelClassResolver();
	
	public static void index() throws Exception {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("userprofile.usereditor.pagename");
		
		String username = Secure.connected();
		String key = UserProfile.prepareKey(username);
		
		Class<? extends CrudOrmModel> entityclass = UserProfile.class;
		
		String type = entityclass.getSimpleName().toLowerCase();
		
		UserProfile userprofile = new UserProfile();
		CrudOrmEngine<UserProfile> engine = new CrudOrmEngine<UserProfile>(userprofile);
		
		if (engine.exists(key)) {
			userprofile = engine.read(key);
		} else {
			userprofile = engine.create();
			userprofile.key = key;
			engine.saveInternalElement();
		}
		
		List<ORMFormField> fields = ORMFormField.removeFields(ORMFormField.getFields(entityclass), "createdate", "updatedate");
		
		HashMap<String, HashMap<String, String>> fieldspointers = ORMFormField.getFieldPointerTable(usermodelclassresolver, fields);
		
		Object object = userprofile;
		
		render(title, type, fields, entityclass, object, fieldspointers);
	}
	
	public static void update() throws Exception {
		String username = Secure.connected();
		String key = UserProfile.prepareKey(username);
		
		Class<? extends CrudOrmModel> entityclass = UserProfile.class;
		String type = entityclass.getSimpleName().toLowerCase();
		
		/**
		 * Protect to ingest readonly fields
		 */
		ParamNode object_param_nodes = params.getRootParamNode().getChild("object");
		notFoundIfNull(object_param_nodes);
		List<ORMFormField> fields = ORMFormField.removeFields(ORMFormField.getFields(entityclass), "createdate", "updatedate");
		List<RemovedNode> rn = new ArrayList<ParamNode.RemovedNode>();
		for (int pos = 0; pos < fields.size(); pos++) {
			if (fields.get(pos).readonly) {
				object_param_nodes.removeChild(fields.get(pos).name, rn);
			}
		}
		
		CrudOrmEngine<CrudOrmModel> engine = CrudOrmEngine.get(entityclass);
		CrudOrmModel object = engine.read(key);
		if (object == null) {
			notFound();
		}
		
		Binder.bindBean(params.getRootParamNode(), "object", object);
		
		validation.valid(object);
		
		HashMap<String, HashMap<String, String>> fieldspointers = ORMFormField.getFieldPointerTable(usermodelclassresolver, fields);
		
		if (Validation.hasErrors()) {
			renderArgs.put("error", Messages.get("crud.hasErrors"));
			render("User/index.html", type, fields, entityclass, object, fieldspointers);
		}
		engine.saveInternalElement();
		
		flash.success(Admin.getIsSavedFlashMessage(type));
		redirect("User.index");
	}
	
	private static class AsyncAction implements Callable<Boolean> {
		
		private CrudOrmModel element;
		private Method method;
		
		public AsyncAction(CrudOrmModel element, Method method) {
			this.element = element;
			if (element == null) {
				throw new NullPointerException("\"element\" can't to be null");
			}
			this.method = method;
			if (method == null) {
				throw new NullPointerException("\"method\" can't to be null");
			}
		}
		
		public Boolean call() throws Exception {
			method.invoke(element);
			return true;
		}
	}
	
	public static void action(String objtype, String key, String targetmethod) throws Exception {
		String username = Secure.connected();
		String real_key = UserProfile.prepareKey(username);
		
		if (key.equalsIgnoreCase(real_key) == false) {
			forbidden();
		}
		
		Class<? extends CrudOrmModel> entityclass = UserProfile.class;
		// String type = entityclass.getSimpleName().toLowerCase();
		
		CrudOrmEngine<CrudOrmModel> engine = CrudOrmEngine.get(entityclass);
		
		Method method = entityclass.getMethod(targetmethod);
		if (method == null) {
			notFound();
		}
		if (method.isAnnotationPresent(PublishedMethod.class) == false) {
			notFound();
		}
		CrudOrmModel element = engine.read(real_key);
		if (element == null) {
			notFound();
		}
		
		JobsPlugin.executor.submit(new AsyncAction(element, method));
		flash.success(Admin.getActionFlashMessage(objtype, targetmethod, null));
		
		/*try {
			method.invoke(element);
			flash.success(Admin.getActionFlashMessage(objtype, targetmethod, null));
		} catch (InvocationTargetException e) {
			Log2.log.error("Error during remove invoke", e.getTargetException());
			flash.error(Admin.getActionFlashMessage(objtype, targetmethod, e.getTargetException()));
		} catch (Exception e) {
			Log2.log.error("Error during invoke", e);
			flash.error(Admin.getActionFlashMessage(objtype, targetmethod, e));
		}*/
		redirect("User.index", objtype);
	}
	
	static UserProfile getUserProfile() throws Exception {
		String username = Secure.connected();
		String key = UserProfile.prepareKey(username);
		CrudOrmEngine<UserProfile> engine = UserProfile.getORMEngine(key);
		return engine.getInternalElement();
	}
	
}
