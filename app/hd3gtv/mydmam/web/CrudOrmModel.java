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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.web;

import hd3gtv.mydmam.db.orm.OrmModel;

import java.util.Date;

import controllers.Admin.HiddenCompactView;
import controllers.Admin.ReadOnly;

public abstract class CrudOrmModel extends OrmModel {
	
	@ReadOnly
	@HiddenCompactView
	public Date createdate;
	
	@ReadOnly
	@HiddenCompactView
	public Date updatedate;
	
	/**
	 * Static call
	 */
	protected abstract String getCF_Name();
	
	/**
	 * Static call
	 */
	protected abstract Class<? extends CrudOrmModel> getClassInstance();
	
	public String shortName() {
		return key;
	}
	
	public void onAfterSave() {
	}
	
}
