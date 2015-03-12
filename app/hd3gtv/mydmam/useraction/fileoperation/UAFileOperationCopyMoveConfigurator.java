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
package hd3gtv.mydmam.useraction.fileoperation;

import hd3gtv.mydmam.db.orm.annotations.TypeNavigatorInputSelection;
import hd3gtv.mydmam.useraction.fileoperation.CopyMove.FileExistsPolicy;

import java.io.Serializable;

public class UAFileOperationCopyMoveConfigurator implements Serializable {
	
	public enum Action {
		COPY, MOVE
	}
	
	public Action action;
	
	@TypeNavigatorInputSelection(canselectdirs = true, canselectstorages = true, canselectfiles = false)
	public String destination;
	
	public FileExistsPolicy fileexistspolicy;
	
}
