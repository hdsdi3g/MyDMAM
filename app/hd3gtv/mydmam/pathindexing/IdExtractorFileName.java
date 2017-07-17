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
package hd3gtv.mydmam.pathindexing;

import hd3gtv.mydmam.factory.JSComment;
import hd3gtv.mydmam.factory.JSVarName;

public interface IdExtractorFileName {
	
	public static final String MODULE_NAME = "IdExtractorFileName";
	
	/**
	 * @param filename without path
	 */
	public String getId(@JSVarName("filename") String filename);
	
	/**
	 * @param filename without path
	 * @return true if S0000000 or 00000000
	 */
	@JSComment("If this return true, getIdFromFilename should not return null")
	public boolean isValidId(@JSVarName("filename") String filename);
	
}
