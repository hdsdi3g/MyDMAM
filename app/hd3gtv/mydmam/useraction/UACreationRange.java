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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.useraction;

public enum UACreationRange {
	/**
	 * Notification(Item1, Item2) => Storage1[Finisher(Item1, Item2) => Task2(Item1, Item2) => Task1(Item1, Item2)], Storage2[...]
	 */
	ONE_USER_ACTION_BY_STORAGE_AND_BASKET,
	
	/**
	 * - Notification(Item1) => Finisher(Item1) => Task2(Item1) => Task1(Item1)
	 * - Notification(Item2) => Finisher(Item2) => Task2(Item2) => Task1(Item2)
	 */
	ONE_USER_ACTION_BY_BASKET_ITEM,
	
	/**
	 * Notification(Task2) => Storage2[...] => Storage1[Task2(Item1, Item2)+Finisher(Item1, Item2)]
	 * => Notification(Task1) => Storage2[...] => Storage1[Task1(Item1, Item2)+Finisher(Item1, Item2)]
	 */
	ONE_USER_ACTION_BY_FUNCTIONALITY;
	
}