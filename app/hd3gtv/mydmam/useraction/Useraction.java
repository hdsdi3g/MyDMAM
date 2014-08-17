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

public class Useraction {
	
	/**
	 * TODO create ORM <-> JsonObjet
	 * TODO Useraction worker: generic (interface/abstract) Useraction executor
	 * - declare some functionalities
	 * - must compute Profiles names/cat based on this declared functionalities
	 * TODO Useraction range: 1 task for all items, or 1 task by item, or items by items for all tasks.
	 * TODO Useraction requirement: compute Useractions availabilities with the actual Useraction workers profiles and Storages access
	 * TODO Useraction availability: publish in database the full requirement computed list for the current probe, via isAlive
	 * TODO Useraction publisher in website
	 * - popup method for a basket in baskets list
	 * - special web page, "Useraction creation page", apply to the current basket
	 * - popup method for the whole and recursive directory in navigator
	 * - popup method for the current directory in navigator.
	 * - popup method for the current file in navigator.
	 * - nothing in search result
	 * TODO Useraction creator: list options to ask to user in website for create an Useraction. Specific for an Useraction. Declare a Finisher.
	 * TODO Useraction publisher popup menu
	 * - direct display button, async create sub menu content
	 * - retrieve Capabilities and Availabilities from database, and display the correct popup content relative to the creator
	 * - each action link will be targeted to an Useraction creation modal
	 * - or preconfigured one-click action
	 * TODO Useraction creation tasks page/modal in iframe
	 * - display current basket, or an anonymous basket with the only one item requested (file, dir, recursive dir)
	 * - select and add an Useraction by Category, and by Long name, following the actual Availabilities.
	 * - add creator configuration form fields, following the Creator declaration.
	 * - add Useraction Range selection
	 * - add basket action after creation (for "by basket" creation): truncate after start, truncate by the finisher, or don't touch.
	 * - add Notification options
	 * - on validation: create task(s) with Task context, finisher(s) and notification(s)
	 * TODO Useraction Task context, (de)serialize Json
	 * - content of Useraction Creation (Range, finisher)
	 * - element(s)
	 * TODO Useraction supervision
	 * - display Capabilities and Availabilities table
	 * - admin Usergroups white/black list
	 * - admin Useraction specific params (and published by ORM/form)
	 * TODO Useraction finisher, one by Useraction created task (and required by this). It can be, after the job is ended, the refresh original selected Storage index items.
	 * - Add Explorer option: simple refresh for a directory (only add new elements) or a forced refresh (recursive with delete).
	 * - Add user basket remove item option.
	 */
}
