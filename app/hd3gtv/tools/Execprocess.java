/*
 * This file is part of Java Tools by hdsdi3g'.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2009-2012
 * 
*/
package hd3gtv.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Locale;

@SuppressWarnings("nls")
/**
 * @author hdsdi3g
 * @version 1.01 set public constructor
 * 			1.1 Add OutputStreamHandler
 */
public class Execprocess extends Thread {
	
	public static final int STATE_READY = -1;
	public static final int STATE_RUNNIG = 0;
	public static final int STATE_END = 1;
	public static final int STATE_KILL = 2;
	
	private long starttime;
	
	private ArrayList<String> processinfo;
	private int exitvalue;
	private int status;
	
	private ProcessBuilder pb;
	private Process process;
	
	private ExecprocessEvent events;
	private ExecprocessStringresult stdout;
	private ExecprocessStringresult stderr;
	
	private String commandline;
	
	private ExecprocessOutputstream outputstreamhandler;
	
	public Execprocess(String execname, ArrayList<String> param, ExecprocessEvent events) {
		processinfo = new ArrayList<String>();
		processinfo.add(execname);
		if (param != null) {
			processinfo.addAll(param);
		}
		this.events = events;
		exitvalue = -1;
		status = STATE_READY;
		starttime = -1;
		setName("Execprocess-" + String.valueOf(System.currentTimeMillis()));
		
		StringBuffer cmdline = new StringBuffer();
		cmdline.append(execname);
		for (int i = 0; i < param.size(); i++) {
			cmdline.append(" ");
			cmdline.append(param.get(i));
		}
		commandline = cmdline.toString();
		
	}
	
	public Execprocess(String execname, ArrayList<String> param) {
		this(execname, param, null);
	}
	
	public String getCommandline() {
		return commandline;
	}
	
	public void setOutputstreamhandler(ExecprocessOutputstream outputstreamhandler) {
		this.outputstreamhandler = outputstreamhandler;
	}
	
	public void run() {
		starttime = System.currentTimeMillis();
		
		if (events != null) {
			events.onStart();
		}
		
		exitvalue = -1;
		status = STATE_RUNNIG;
		
		pb = new ProcessBuilder(processinfo);
		pb.environment().put("LANG", Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry() + "." + Charset.forName("UTF-8"));
		
		try {
			process = pb.start();
		} catch (IOException ioe) {
			if (events != null) {
				events.onError(ioe);
			} else {
				ioe.printStackTrace();
			}
		}
		
		if (process != null) {
			
			if (outputstreamhandler != null) {
				ExecprocessOutputStream eos = new ExecprocessOutputStream();
				eos.outputstreamhandler = outputstreamhandler;
				eos.processoutputstream = process.getOutputStream();
				eos.start();
			}
			if (events != null) {
				stdout = new ExecprocessStringresult(this, process.getInputStream(), false, events);
				stdout.start();
				stderr = new ExecprocessStringresult(this, process.getErrorStream(), true, events);
				stderr.start();
			}
			
			try {
				process.waitFor();
			} catch (InterruptedException ie) {
				if (events != null) {
					events.onError(ie);
				}
			}
			
			exitvalue = process.exitValue();
			
		} else {
			status = STATE_END;
		}
		if (events != null) {
			events.onEnd();
		}
	}
	
	public int getExitvalue() {
		return exitvalue;
	}
	
	public synchronized int getStatus() {
		return status;
	}
	
	public synchronized void kill() {
		if (status == STATE_RUNNIG) {
			status = STATE_KILL;
			process.destroy();
			if (events != null) {
				events.onKill();
			}
		}
	}
	
	public synchronized void stdout(String line) {
		if (events != null) {
			events.onStdout(line);
		}
	}
	
	public synchronized void stderr(String line) {
		if (events != null) {
			events.onStderr(line);
		}
	}
	
	/**
	 * @return La date de lancement en unixtime.
	 */
	public synchronized long getStarttime() {
		return starttime;
	}
	
	/**
	 * @return Le nombre de ms depuis le debut du lancement, ou -1 si le thread n'est pas partit.
	 */
	public synchronized long getUptime() {
		if (starttime < 0) {
			return -1;
		} else {
			return System.currentTimeMillis() - starttime;
		}
	}
	
	/** ************************************************************************ */
	private class ExecprocessStringresult extends Thread {
		
		private InputStream stream;
		
		private Execprocess runner;
		private boolean stderr;
		private ExecprocessEvent events;
		
		public ExecprocessStringresult(Execprocess runner, InputStream stream, boolean stderr, ExecprocessEvent events) {
			this.stream = stream;
			setDaemon(false);
			this.runner = runner;
			this.stderr = stderr;
			this.events = events;
			if (stderr) {
				setName(runner.getName() + "-STDERR");
			} else {
				setName(runner.getName() + "-STDOUT");
			}
		}
		
		public void run() {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
				try {
					String line = "";
					while (((line = reader.readLine()) != null)) {
						if (stderr) {
							runner.stderr(line);
						} else {
							runner.stdout(line);
						}
					}
				} catch (IOException ioe) {
					if (ioe.getMessage().equalsIgnoreCase("Bad file descriptor")) {
						return;
					}
					if (ioe.getMessage().equalsIgnoreCase("Stream closed")) {
						return;
					}
					if (events != null) {
						events.onError(ioe);
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					reader.close();
				}
			} catch (IOException ioe) {
				if (events != null) {
					events.onError(ioe);
				}
			}
		}
		
	}
	
	/** ************************************************************************ */
	private class ExecprocessOutputStream extends Thread {
		ExecprocessOutputstream outputstreamhandler;
		OutputStream processoutputstream;
		
		public void run() {
			try {
				outputstreamhandler.onStartProcess(processoutputstream);
			} catch (IOException e) {
				e.printStackTrace();
				if (e.getMessage().equalsIgnoreCase("Stream closed") == false) {
					if (events != null) {
						events.onError(e);
					}
				}
			}
		}
	}
	
}
