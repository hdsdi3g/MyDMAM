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

import java.io.IOException;

/**
 * @author hdsdi3g
 * @version 1.0
 */
public interface ExecprocessEvent {
	
	/**
	 * L'execution commence
	 */
	public void onStart();
	
	/**
	 * L'execution se termine
	 */
	public void onEnd();
	
	/**
	 * On a tuer le processus.
	 */
	public void onKill();
	
	/**
	 * On a renconte une erreur
	 */
	public void onError(IOException ioe);
	
	/**
	 * On a renconte une erreur
	 */
	public void onError(InterruptedException ie);
	
	/**
	 * Le process donne des donnes sur sa sortie standart
	 */
	public void onStdout(String message);
	
	/**
	 * Le process donne des donnes sur sa sortie erreur
	 */
	public void onStderr(String message);
	
}
