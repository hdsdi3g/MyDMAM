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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

@SuppressWarnings("nls")
/**
 * @author hdsdi3g
 * @version 1.1 Add OutputStreamHandler
 */
public class ExecprocessGettext {
	
	private Execprocess runprocess;
	private StringBuffer resultstdout;
	private StringBuffer resultstderr;
	public static final String LINESEPARATOR = System.getProperty("line.separator");
	private Event event;
	private boolean endlinewidthnewline;
	private int maxexectime;
	private boolean killed;
	private Exception lasterror;
	private boolean exitcodemusttobe0;
	
	public ExecprocessGettext(File executable, ArrayList<String> param) {
		event = new Event();
		runprocess = new Execprocess(executable, param, event);
		resultstdout = new StringBuffer();
		resultstderr = new StringBuffer();
		endlinewidthnewline = false;
		maxexectime = 3600 * 24 * 365;
		killed = false;
		lasterror = null;
		exitcodemusttobe0 = true;
	}
	
	public boolean isEndlinewidthnewline() {
		return endlinewidthnewline;
	}
	
	public void setEndlinewidthnewline(boolean endlinewidthnewline) {
		this.endlinewidthnewline = endlinewidthnewline;
	}
	
	public boolean isKilled() {
		return killed;
	}
	
	public void setOutputstreamhandler(ExecprocessOutputstream outputstreamhandler) {
		runprocess.setOutputstreamhandler(outputstreamhandler);
	}
	
	/**
	 * @param exitcodemusttobe0, true par defaut.
	 */
	public void setExitcodemusttobe0(boolean exitcodemusttobe0) {
		this.exitcodemusttobe0 = exitcodemusttobe0;
	}
	
	/**
	 * Definir un temps d'execution maximum avant le lancement
	 * @param maxexectime en secondes
	 */
	public void setMaxexectime(int maxexectime) {
		this.maxexectime = maxexectime;
	}
	
	/**
	 * Lance le process
	 * @throws IOException en cas d'erreur, de code de retour different de 0, ou si le temps d'execution est depasse.
	 */
	public void start() throws IOException, ExecprocessBadExecutionException {
		try {
			if (maxexectime > 0) {
				runprocess.start();
				/** sync */
				long maxexectime_ms = maxexectime * 1000;
				while ((runprocess.isAlive()) | (runprocess.getUptime() < 0)) {
					if (runprocess.getUptime() > maxexectime_ms) {
						runprocess.kill();
						throw new IOException("Max execution time reached (" + String.valueOf(maxexectime) + " sec)");
					} else {
						Thread.sleep(100);
					}
				}
			} else {
				runprocess.run();
				/** async */
			}
			if ((runprocess.getExitvalue() != 0) && exitcodemusttobe0) {
				throw new ExecprocessBadExecutionException(runprocess.getName(), runprocess.getCommandline(), runprocess.getExitvalue());
			}
			if (lasterror != null) {
				throw lasterror;
			}
		} catch (Exception e) {
			if (e instanceof IOException) {
				throw (IOException) e;
			} else {
				throw new IOException(e);
			}
		}
	}
	
	public StringBuffer getResultstdout() {
		return resultstdout;
	}
	
	public StringBuffer getResultstderr() {
		return resultstderr;
	}
	
	public Execprocess getRunprocess() {
		return runprocess;
	}
	
	/** ********************************************** */
	private class Event implements ExecprocessEvent {
		
		public void onEnd() {
		}
		
		public void onStart() {
		}
		
		public void onKill() {
			killed = true;
		}
		
		public void onError(InterruptedException ie) {
			if (lasterror != null) {
				lasterror.printStackTrace();
			}
			lasterror = ie;
		}
		
		public void onError(IOException ioe) {
			if (lasterror != null) {
				lasterror.printStackTrace();
			}
			lasterror = ioe;
		}
		
		public void onStderr(String message) {
			resultstderr.append(message);
			if (endlinewidthnewline) {
				resultstderr.append(LINESEPARATOR);
			}
		}
		
		public void onStdout(String message) {
			resultstdout.append(message);
			if (endlinewidthnewline) {
				resultstdout.append(LINESEPARATOR);
			}
		}
		
	}
	
}
