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
	 * TODO Useraction capability
	 * - limit to files, to directories (hard-coded)
	 * - Accept/ban Storageindexes root (hard-coded)
	 * - Storageindexes white/black list (declared in YAML, specific for an Useraction)
	 * - Usergroups white/black list (declared in website by Admin Configuration)
	 * - Force to have a local storage bridge (hard-coded)
	 * TODO Useraction scope ?
	 * TODO Useraction worker: generic Useraction executor
	 * TODO Useraction worker informations:
	 * - Useraction category (filesystem, transcoding, metadata/pvw, content check, download/delivery, user metadata...)
	 * - Provider (MyDMAM Internal, ...)
	 * - name
	 * - longname
	 * - instance ref (autogenerate uuid).
	 * - description
	 * - Internal and local configuration in YAML
	 * - External and global configuration in website (one configuration by worker type): Admin Configuration
	 * - Capability
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
	 * TODO Useraction publisher popup
	 * - direct display button
	 * - retrieve Capabilities and Availabilities from database, and display the correct popup content relative to the creator
	 * - each action link will be targeted to an Useraction creation modal
	 * TODO Useraction creation page/modal
	 * - display current basket, or an anonymous basket with the only one item requested (file, dir, recursive dir)
	 * - select and add an Useraction by Category, and by Long name, following the actual Availabilities.
	 * - add creator configuration form fields, following the Creator declaration.
	 * - add Useraction Range selection
	 * - add basket action after creation (for "by basket" creation): truncate after start, truncate by the finisher, or don't touch.
	 * - add Notification options
	 * TODO Useraction supervision
	 * - display Capabilities and Availabilities table
	 * TODO Useraction finisher, one by Useraction created task (and required by this). It can be, after the job is ended, the refresh original selected Storage index items.
	 * - Add Explorer option: simple refresh for a directory (only add new elements) or a forced refresh (recursive with delete).
	 * - Add user basket remove item option.
	 * TODO Useraction admin configuration
	 * - Usergroups white/black list
	 * - Useraction specific params (and published by ORM/form)
	 */
}
