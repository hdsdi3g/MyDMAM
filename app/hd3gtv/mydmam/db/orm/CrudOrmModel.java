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

import hd3gtv.mydmam.db.orm.annotations.HiddenCompactView;
import hd3gtv.mydmam.db.orm.annotations.ReadOnly;

import java.util.Date;
import java.util.zip.CRC32;

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
	
	/**
	 * Key based (via CRC32)
	 */
	public int hashCode() {
		if (key != null) {
			CRC32 crc = new CRC32();
			crc.update(key.getBytes());
			return (int) crc.getValue();
		} else {
			return super.hashCode();
		}
	}
	
	/**
	 * Must same class instance, CF name and key
	 */
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if ((obj instanceof CrudOrmModel) == false) {
			return false;
		}
		CrudOrmModel candidate = (CrudOrmModel) obj;
		
		if (getClassInstance() != candidate.getClassInstance()) {
			return false;
		}
		if (candidate.getCF_Name() == null) {
			return false;
		}
		if (getCF_Name().equals(candidate.getCF_Name()) == false) {
			return false;
		}
		return key.equals(candidate.key);
	}
}
