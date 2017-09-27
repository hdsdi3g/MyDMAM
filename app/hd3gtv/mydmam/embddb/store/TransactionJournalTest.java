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
 * Copyright (C) hdsdi3g for hd3g.tv 27 sept. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.File;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.After;

import junit.framework.TestCase;

public class TransactionJournalTest extends TestCase {
	
	private TransactionJournal journal;
	private File file;
	
	public TransactionJournalTest() throws Exception {
		UUID uuid = UUID.randomUUID();
		file = new File(System.getProperty("user.home") + File.separator + "mydmam-test-transactionjournal-" + uuid);
		FileUtils.forceMkdir(file);
		FileUtils.forceDeleteOnExit(file);
		
		journal = new TransactionJournal(file, uuid);
	}
	
	@After
	public void onAfterEachTest() throws Exception {
		// TODO
	}
	
	// TODO create Unit tests
}
