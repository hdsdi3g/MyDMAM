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
package hd3gtv.mydmam.dareport;

import java.util.ArrayList;
import java.util.List;

public class DARMails {
	// TODO Accumulate all reports for this day to one summary
	// TODO Send a report to the current requested user (an admin).
	
	private List<DARReport> reports;
	
	public DARMails() {
		// TODO get day
		// TODO get admin users
		// TODO get reports users and prepare special mails
		reports = new ArrayList<DARReport>();
	}
	
	// TODO get vars from DARReport, make mails, send mails
	
}
