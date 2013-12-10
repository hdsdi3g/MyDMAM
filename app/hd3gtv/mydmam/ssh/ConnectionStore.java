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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.ssh;

import hd3gtv.configuration.SyntaxConfigurationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.yaml.snakeyaml.Yaml;

import com.jcraft.jsch.JSchException;

class ConnectionStore {
	
	private File store_file;
	private LinkedHashMap document;
	private Ssh ssh;
	
	ConnectionStore(File configuration_path, Ssh ssh) throws IOException {
		if (configuration_path == null) {
			throw new NullPointerException("\"configuration_path\" can't to be null");
		}
		this.ssh = ssh;
		
		store_file = new File(configuration_path + File.separator + "store.yml");
		document = new LinkedHashMap();
		
		if (store_file.exists()) {
			FileInputStream fileinputstream = new FileInputStream(store_file);
			Yaml yaml = new Yaml();
			Object data = yaml.load(fileinputstream);
			if ((data instanceof LinkedHashMap<?, ?>) == false) {
				fileinputstream.close();
				throw new SyntaxConfigurationException("No Map at root document.");
			}
			document = (LinkedHashMap<String, ?>) data;
			fileinputstream.close();
		}
	}
	
	private void save() throws IOException {
		FileWriter fw = new FileWriter(store_file, false);
		Yaml yaml = new Yaml();
		yaml.dump(document, fw);
		fw.close();
	}
	
	void addConnection(String host, int port, String username, String password, String connection_name) throws IOException {
		LinkedHashMap element = new LinkedHashMap();
		element.put("host", host);
		element.put("port", port);
		element.put("username", username);
		element.put("password", password);
		document.put(connection_name, element);
		save();
	}
	
	void addConnection(String host, int port, String username, String connection_name) throws IOException {
		LinkedHashMap element = new LinkedHashMap();
		element.put("host", host);
		element.put("port", port);
		element.put("username", username);
		document.put(connection_name, element);
		save();
	}
	
	boolean exists(String connection_name) {
		return document.containsKey(connection_name);
	}
	
	Remote getConnection(String connection_name) throws JSchException {
		if (exists(connection_name) == false) {
			return null;
		}
		LinkedHashMap<String, ?> data = (LinkedHashMap<String, ?>) document.get(connection_name);
		if (data.containsKey("password")) {
			return new Remote(ssh.getSession((String) data.get("host"), (Integer) data.get("port"), (String) data.get("username"), (String) data.get("password")), connection_name);
		} else {
			return new Remote(ssh.getSession((String) data.get("host"), (Integer) data.get("port"), (String) data.get("username")), connection_name);
		}
	}
}