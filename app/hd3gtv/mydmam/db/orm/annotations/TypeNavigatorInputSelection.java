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
package hd3gtv.mydmam.db.orm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used in Useraction with navigator.inputselect.js
 * Always return a SourcePathIndexerElement key !
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface TypeNavigatorInputSelection {
	
	/**
	 * @return true by default
	 */
	boolean canselectfiles() default true;
	
	/**
	 * @return true by default
	 */
	boolean canselectdirs() default true;
	
	/**
	 * @return true by default
	 */
	boolean canselectstorages() default true;
	
	/**
	 * It will be translated with i18n.
	 */
	String placeholderlabel() default "";
	
}
