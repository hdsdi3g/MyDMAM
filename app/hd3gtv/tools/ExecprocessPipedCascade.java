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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class ExecprocessPipedCascade {
	
	private ArrayList<Execprocess> list_process;
	private ArrayList<Pipe> pipes;
	private File working_directory;
	private static final Logger log = Logger.getLogger(ExecprocessPipedCascade.class);
	
	/**
	 * Pipe process1.out -> in.process2.out -> in.process3
	 * Log all errors at end.
	 */
	public ExecprocessPipedCascade() {
		list_process = new ArrayList<>(2);
		pipes = new ArrayList<>(1);
	}
	
	public void setWorkingDirectory(File working_directory) throws IOException {
		CopyMove.checkExistsCanRead(working_directory);
		CopyMove.checkIsDirectory(working_directory);
		this.working_directory = working_directory;
		list_process.forEach(v -> {
			try {
				v.setWorkingDirectory(working_directory);
			} catch (Exception e) {
				log.warn("Can't set working_directory " + working_directory);
			}
		});
	}
	
	public void add(File execname, ArrayList<String> params) {
		Execprocess new_item = new Execprocess(execname, params, new Event(execname.getName() + "/" + list_process.size()));
		if (working_directory != null) {
			try {
				new_item.setWorkingDirectory(working_directory);
			} catch (IOException e) {
				log.warn("Can't set working_directory " + working_directory);
			}
		}
		new_item.setDaemon(true);
		new_item.setName("Execproces for " + execname.getName() + ", pipe #" + list_process.size());
		new_item.setPipe_cascade(this);
		
		if (list_process.isEmpty() != true) {
			pipes.add(new Pipe(list_process.get(list_process.size() - 1).getExec_name(), execname.getName(), pipes.size()));
		}
		
		list_process.add(new_item);
	}
	
	/**
	 * Non blocking
	 */
	public void startAll() {
		for (int pos = list_process.size() - 1; pos > -1; pos--) {
			list_process.get(pos).start();
			/*try {
				while (pipes.get(pipes.size() - 1).isAlive() == false) {
					Thread.sleep(10);
				}
			} catch (InterruptedException e) {
			}*/
		}
		
		pipes.forEach(p -> {
			try {
				while (p.next_process_std_in == null | p.previous_process_std_out == null) {
					Thread.sleep(10);
				}
			} catch (InterruptedException e) {
			}
		});
		
		pipes.forEach(p -> {
			p.start();
		});
	}
	
	public void kill() {
		list_process.forEach(v -> {
			v.kill();
		});
	}
	
	/**
	 * Blocking
	 * @return true if all process return 0.
	 */
	public boolean waitExec() {
		list_process.forEach(v -> {
			try {
				while (v.isAlive()) {
					Thread.sleep(10);
				}
			} catch (Exception e) {
			}
		});
		
		int exit_value;
		for (int pos = 0; pos < list_process.size(); pos++) {
			exit_value = list_process.get(pos).getExitvalue();
			if (exit_value != 0) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Blocking
	 * @return true if all process return 0.
	 */
	public boolean waitExec(final StoppableProcessing stoppable) {
		
		list_process.forEach(v -> {
			try {
				while (v.isAlive() && (stoppable.isWantToStopCurrentProcessing() == false)) {
					Thread.sleep(10);
				}
			} catch (Exception e) {
			}
		});
		
		if (stoppable.isWantToStopCurrentProcessing()) {
			list_process.forEach(v -> {
				v.kill();
			});
		}
		
		int exit_value;
		for (int pos = 0; pos < list_process.size(); pos++) {
			exit_value = list_process.get(pos).getExitvalue();
			if (exit_value != 0) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * @return true if ref is not the first
	 */
	boolean hasSourceDatas(Execprocess ref) {
		return list_process.indexOf(ref) > 0;
	}
	
	/**
	 * @return true if ref is not the last
	 */
	boolean hasDestDatas(Execprocess ref) {
		return (list_process.indexOf(ref) + 1 != list_process.size());
	}
	
	void connectSourceDatas(Execprocess ref, OutputStream process_std_in) {
		log.debug("Set pipe next_process_std_in by " + ref.getExec_name());
		pipes.get(list_process.indexOf(ref) - 1).next_process_std_in = process_std_in;
	}
	
	void connectDestDatas(Execprocess ref, InputStream process_std_out) {
		log.debug("Set pipe previous_process_std_out by " + ref.getExec_name());
		pipes.get(list_process.indexOf(ref)).previous_process_std_out = process_std_out;
	}
	
	private class Pipe extends Thread {
		
		private OutputStream next_process_std_in;
		private InputStream previous_process_std_out;
		
		public Pipe(String previous_process, String next_process_name, int pipe_num) {
			setDaemon(true);
			setName("Pipe process for " + previous_process + " > " + next_process_name + ", #" + pipe_num);
		}
		
		public void run() {
			log.debug("Start " + getName());
			
			try {
				IOUtils.copyLarge(previous_process_std_out, next_process_std_in);
			} catch (IOException e) {
				if (e.getMessage().equalsIgnoreCase("Broken pipe")) {
				} else {
					log.error("With streams", e);
				}
			}
			try {
				next_process_std_in.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			log.trace("End " + getName());
		}
		
	}
	
	private class Event implements ExecprocessEvent {
		
		String name;
		
		public Event(String name) {
			this.name = name;
			if (name == null) {
				throw new NullPointerException("\"name\" can't to be null");
			}
		}
		
		public void onStart(String commandline, File working_directory) {
			log.info("Start process " + name + ": " + commandline);
		}
		
		public void onEnd(int exitvalue, long execution_duration) {
			log.debug("End process " + name + ", return " + exitvalue + " during " + (double) execution_duration / 1000d + " sec");
		}
		
		public void onKill(long execution_duration) {
			log.debug("Kill process " + name + ", after " + (double) execution_duration / 1000d + " sec");
		}
		
		public void onError(IOException ioe) {
			log.error("Process " + name + " has an error ", ioe);
		}
		
		public void onError(InterruptedException ie) {
			log.warn("Process " + name + " has an error ", ie);
		}
		
		public void onStdout(String message) {
			log.debug("Process " + name + " stdout: " + message);
		}
		
		public void onStderr(String message) {
			if (log.isTraceEnabled()) {
				System.err.println("[" + name + "]" + message);
			}
		}
		
	}
	
}
