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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.useraction.dummy;

import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.useraction.UACapability;
import hd3gtv.mydmam.useraction.UAConfigurator;
import hd3gtv.mydmam.useraction.UAFinisherConfiguration;
import hd3gtv.mydmam.useraction.UAFunctionality;
import hd3gtv.mydmam.useraction.UAJobProcess;
import hd3gtv.mydmam.useraction.UARange;

import java.util.HashMap;

public class UADummy extends UAFunctionality {
	
	public String getSection() {
		return "Dummy section";
	}
	
	public String getVendor() {
		return "Internal MyDMAM";
	}
	
	public String getLongName() {
		return "Dummy User Action";
	}
	
	public String getDescription() {
		return "Dummy User Action for tests and debugging";
	}
	
	public UAConfigurator createEmptyConfiguration() {
		return UAConfigurator.create(new UADummyConfigurator());
	}
	
	public boolean hasOneClickDefault() {
		return false;
	}
	
	public UAFinisherConfiguration getFinisherForOneClick() {
		return null;
	}
	
	public UARange getRangeForOneClick() {
		return null;
	}
	
	public UAConfigurator createOneClickDefaultUserConfiguration() {
		return null;
	}
	
	public UAJobProcess createProcess() {
		return new UADummyProcess();
	}
	
	public UACapability createCapability(HashMap<String, ConfigurationItem> internal_configuration) {
		return new UADummyCapability(internal_configuration);
	}
	
}
