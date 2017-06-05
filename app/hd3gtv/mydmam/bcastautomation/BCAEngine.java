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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.mydmam.bcastautomation;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface BCAEngine {
	
	public String getVendorName();
	
	public String getName();
	
	public String getVersion();
	
	public List<String> getValidFileExtension();
	
	public ScheduleFileStatus processScheduleFile(File schedule, BCAAutomationEventHandler hanlder) throws IOException;
	
}
