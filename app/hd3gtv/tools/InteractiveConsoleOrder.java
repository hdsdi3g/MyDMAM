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
 * Copyright (C) hdsdi3g for hd3g.tv 5 sept. 2017
 * 
*/
package hd3gtv.tools;

import java.io.PrintStream;
import java.util.function.BiConsumer;

@FunctionalInterface
public interface InteractiveConsoleOrder {
	
	/**
	 * @param procedure callbacked param maybe null. Display with the PrintStream
	 */
	void addConsoleOrder(String order, String name, String description, Class<?> creator, BiConsumer<String, PrintStream> procedure);
	
}
