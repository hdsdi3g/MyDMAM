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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.pathindexing;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.MyDMAM;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;

import org.elasticsearch.client.Client;
import org.json.simple.parser.ParseException;

public class ImporterCDFinder extends Importer {
	
	private File filetoimport;
	private String poolname;
	
	public ImporterCDFinder(Client client, File filetoimport, String poolname) throws IOException, ParseException {
		super(client);
		this.filetoimport = filetoimport;
		if (filetoimport == null) {
			throw new NullPointerException("\"filetoimport\" can't to be null");
		}
		if (filetoimport.exists() == false) {
			throw new FileNotFoundException(filetoimport.getPath());
		}
		this.poolname = poolname;
	}
	
	protected String getName() {
		return poolname;
	}
	
	protected long doIndex(IndexingEvent elementpush) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(filetoimport))));
		
		String line;
		String[] cols;
		SourcePathIndexerElement element;
		Calendar date = Calendar.getInstance();
		int count = 0;
		String[] partpath;
		String current_rootpath = null;
		long last_valid_date = 0;
		while ((line = reader.readLine()) != null) {
			try {
				line = line.trim();
				if (line.equals("")) {
					continue;
				}
				cols = line.split("\t");
				if (cols.length != 6) {
					continue;
				}
				
				element = new SourcePathIndexerElement();
				
				element.currentpath = "/" + cols[1].replaceAll(":", "/");
				
				element.parentpath = element.currentpath.substring(0, element.currentpath.lastIndexOf("/"));
				if (element.parentpath.equals("")) {
					element.parentpath = "/";
				}
				
				if (cols[4].equals("dossier") | cols[4].equals("Dossier d'archive Zip")) {
					element.directory = true;
				} else {
					element.id = MyDMAM.getIdFromFilename(cols[0]);
					element.directory = false;
				}
				
				if (cols[3].equals("n.a.") == false) {
					try {
						date.set(Calendar.YEAR, Integer.valueOf(cols[3].substring(0, 4)));
						date.set(Calendar.MONTH, Integer.valueOf(cols[3].substring(5, 7)) - 1);
						date.set(Calendar.DAY_OF_MONTH, Integer.valueOf(cols[3].substring(8, 10)));
						date.set(Calendar.HOUR_OF_DAY, Integer.valueOf(cols[3].substring(11, 13)));
						date.set(Calendar.MINUTE, Integer.valueOf(cols[3].substring(14, 16)));
						date.set(Calendar.SECOND, Integer.valueOf(cols[3].substring(17, 19)));
						date.set(Calendar.MILLISECOND, 0);
						element.date = date.getTimeInMillis();
						last_valid_date = element.date;
					} catch (Exception e) {
						Log2Dump dump = new Log2Dump();
						dump.add("cols[1]", cols[1]);
						dump.add("cols[3]", cols[3]);
						Log2.log.error("CDFinder date analyser", e, dump);
					}
				}
				
				element.storagename = poolname;
				element.dateindex = System.currentTimeMillis();
				
				elementpush.onFoundElement(element);
				
				partpath = element.currentpath.split("/");
				if (partpath.length == 3) {
					if (current_rootpath == null) {
						current_rootpath = "/" + partpath[1];
						element = new SourcePathIndexerElement();
						element.currentpath = current_rootpath;
						element.dateindex = System.currentTimeMillis();
						element.directory = true;
						element.parentpath = "/";
						element.storagename = poolname;
						element.date = last_valid_date;
						elementpush.onFoundElement(element);
					} else {
						if (current_rootpath.equals("/" + partpath[1]) == false) {
							current_rootpath = "/" + partpath[1];
							element = new SourcePathIndexerElement();
							element.currentpath = current_rootpath;
							element.dateindex = System.currentTimeMillis();
							element.directory = true;
							element.parentpath = "/";
							element.storagename = poolname;
							element.date = last_valid_date;
							elementpush.onFoundElement(element);
						}
					}
				}
				
			} catch (Exception e) {
				reader.close();
				throw new IOException("Can't process found element", e.getCause());
			}
			count++;
		}
		
		reader.close();
		
		try {
			elementpush.onFoundElement(SourcePathIndexerElement.prepareStorageElement(poolname));
		} catch (Exception e) {
			Log2.log.error("Can't push to ES root storage", e);
		}
		
		return count;
	}
	
	protected long getTTL() {
		return 0;
	}
	
}
