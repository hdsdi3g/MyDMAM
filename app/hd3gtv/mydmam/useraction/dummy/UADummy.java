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

import hd3gtv.mydmam.useraction.UACapability;
import hd3gtv.mydmam.useraction.UAConfigurator;
import hd3gtv.mydmam.useraction.UAFinisherConfiguration;
import hd3gtv.mydmam.useraction.UAFunctionality;
import hd3gtv.mydmam.useraction.UAJobProcess;
import hd3gtv.mydmam.useraction.UARange;

public class UADummy extends UAFunctionality {
	
	@Override
	public UAJobProcess createProcess() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getSection() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getVendor() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Class<? extends UAFunctionality> getReferenceClass() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getLongName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public UAConfigurator createEmptyConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean hasOneClickDefault() {
		return false;
	}
	
	@Override
	public UAFinisherConfiguration getFinisherForOneClick() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public UARange getRangeForOneClick() {
		return null;
	}
	
	public UAConfigurator createOneClickDefaultUserConfiguration() {
		return null;
	}
	
	public Class<? extends UACapability> getCapabilityClass() {
		return UADummyCapability.class;
	}
	
}
