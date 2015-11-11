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

public interface ExecprocessEvent {
	
	/**
	 * On start execution.
	 * @param working_directory maybe null
	 */
	public void onStart(String commandline, File working_directory);
	
	/**
	 * On normal end exec.
	 */
	public void onEnd(int exitvalue, long execution_duration);
	
	/**
	 * On manual kill process.
	 */
	public void onKill(long execution_duration);
	
	public void onError(IOException ioe);
	
	public void onError(InterruptedException ie);
	
	public void onStdout(String message);
	
	public void onStderr(String message);
	
}
