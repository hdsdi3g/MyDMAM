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
package hd3gtv.mydmam.transcode;

import hd3gtv.tools.ExecprocessEvent;

public abstract class ExecprocessTranscodeEvent implements ExecprocessEvent {
	
	protected String jobref;
	
	public void setJobRef(String jobref) {
		this.jobref = jobref;
	}
	
	public abstract String getLast_message();
	
}
