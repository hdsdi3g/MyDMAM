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

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class Remote implements Log2Dumpable {
	
	private Session session;
	private String command;
	private String connection_name;
	private ChannelExec channel;
	
	Remote(Session session, String connection_name) {
		this.session = session;
		this.connection_name = connection_name;
	}
	
	/**
	 * No thread safe
	 */
	public void execute(String command, RemoteObserver observer) throws IOException, JSchException {
		if (command == null) {
			return;
		}
		this.command = command;
		
		channel = (ChannelExec) session.openChannel("exec");
		channel.setEnv("LANG", "UTF-8");
		channel.setCommand(command);
		// channel.setInputStream(null);
		// channel.setOutputStream(System.out);
		// FileOutputStream fos=new FileOutputStream("/tmp/stderr");
		// ((ChannelExec)channel).setErrStream(fos);
		
		Stream stream_out = new Stream(channel.getInputStream(), channel, false, observer);
		Stream stream_err = new Stream(channel.getErrStream(), channel, true, observer);
		
		channel.connect();
		
		stream_out.start();
		stream_err.start();
		
		while (channel.isClosed() == false) {
			try {
				Thread.sleep(100);
			} catch (Exception ee) {
			}
		}
		
		observer.endRemoteExec(channel.getExitStatus());
		channel.disconnect();
	}
	
	public void close() {
		session.disconnect();
	}
	
	public void kill() throws Exception {
		if (channel == null) {
			return;
		}
		Log2.log.info("Kill ssh process", this);
		channel.sendSignal("KILL");
	}
	
	private class Stream extends Thread {
		
		InputStream stream;
		boolean stderr;
		RemoteObserver observer;
		ChannelExec channel;
		
		public Stream(InputStream stream, ChannelExec channel, boolean stderr, RemoteObserver observer) {
			this.stream = stream;
			this.channel = channel;
			this.stderr = stderr;
			this.observer = observer;
			
			setDaemon(true);
			if (stderr) {
				setName("ssh-" + System.currentTimeMillis() + "-stderr");
			} else {
				setName("ssh-" + System.currentTimeMillis() + "-stdout");
			}
		}
		
		public void run() {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(stream)); // , "UTF-8" ???
				try {
					String line = "";
					while (channel.isClosed() == false) {
						while (((line = reader.readLine()) != null)) {
							if (stderr) {
								if (observer.stderrRemoteExec(line) == false) {
									channel.sendSignal("KILL");
								}
							} else {
								if (observer.stdoutRemoteExec(line) == false) {
									channel.sendSignal("KILL");
								}
							}
						}
					}
				} catch (Exception e) {
					if (e.getMessage().equalsIgnoreCase("Bad file descriptor")) {
						return;
					}
					if (e.getMessage().equalsIgnoreCase("Stream closed")) {
						return;
					}
					observer.onErrorExec(e);
				} finally {
					reader.close();
				}
			} catch (IOException ioe) {
				observer.onErrorExec(ioe);
			}
		}
		
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("connection_name", connection_name);
		dump.add("ssh-user", session.getUserName());
		dump.add("ssh-host", session.getHost());
		dump.add("command", command);
		return dump;
	}
	
}
