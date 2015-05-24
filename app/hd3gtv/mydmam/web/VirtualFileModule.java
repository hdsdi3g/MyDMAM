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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.web;

import play.vfs.VirtualFile;

class VirtualFileModule {
	
	private VirtualFile vfile;
	private String module_name;
	
	VirtualFileModule(VirtualFile vfile, String module_name) {
		this.vfile = vfile;
		if (vfile == null) {
			throw new NullPointerException("\"vfile\" can't to be null");
		}
		this.module_name = module_name;
		if (module_name == null) {
			throw new NullPointerException("\"module_name\" can't to be null");
		}
	}
	
	String getModule_name() {
		return module_name;
	}
	
	VirtualFile getVfile() {
		return vfile;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof VirtualFileModule) {
			VirtualFileModule vf = (VirtualFileModule) other;
			if (vfile != null && vf.vfile != null) {
				return vfile.equals(vf.vfile);
			}
		}
		return super.equals(other);
	}
	
	@Override
	public int hashCode() {
		if (vfile != null) {
			return vfile.hashCode();
		}
		return super.hashCode();
	}
	
}
