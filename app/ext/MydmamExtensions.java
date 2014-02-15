/*
 * This file is part of MyDMAM
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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package ext;

import hd3gtv.tools.Timecode;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Date;

import play.i18n.Messages;
import play.templates.JavaExtensions;

public class MydmamExtensions extends JavaExtensions {
	
	public static String toBooleanIcon(Boolean status) {
		if (status) {
			return "<i class=\"icon-ok\"></i>";
		} else {
			return "<i class=\"icon-remove\"></i>";
		}
	}
	
	public static String formatDate(Date date) {
		if (date == null) {
			return "";
		}
		StringBuffer sb = new StringBuffer();
		sb.append(DateFormat.getDateInstance(DateFormat.FULL).format(date));
		sb.append(" ");
		sb.append(DateFormat.getTimeInstance(DateFormat.DEFAULT).format(date));
		return sb.toString();
	}
	
	public static String formatDate(Number date) {
		if (date == null) {
			return "";
		}
		return formatDate(new Date(date.longValue()));
	}
	
	public static String formatDateWoTime(Number date) {
		if (date == null) {
			return "";
		}
		Date currentdate = new Date(date.longValue());
		return DateFormat.getDateInstance(DateFormat.FULL).format(currentdate);
	}
	
	public static String formatDuration(Number duration_sec) {
		if (duration_sec == null) {
			return "";
		}
		if (duration_sec.intValue() == 0) {
			return "";
		}
		Timecode tc = new Timecode(duration_sec.floatValue(), 100);
		return tc.toString().substring(0, 8);
	}
	
	public static String formatFileSize(Number sizeinbytes) {
		if (sizeinbytes == null) {
			return "";
		}
		long bytes = sizeinbytes.longValue();
		if (bytes < 1000l) return bytes + " " + Messages.all(play.i18n.Lang.get()).getProperty("filesizeunitlong");
		
		int exp = (int) (Math.log(bytes) / Math.log(1000));
		StringBuffer result = new StringBuffer();
		result.append("<abbr title=\"");
		
		DecimalFormat dec = new DecimalFormat();
		DecimalFormatSymbols decFS = new DecimalFormatSymbols();
		decFS.setGroupingSeparator(' ');
		dec.setDecimalFormatSymbols(decFS);
		result.append(dec.format(bytes));
		
		result.append(" ");
		result.append(Messages.all(play.i18n.Lang.get()).getProperty("filesizeunitlong"));
		result.append("\">");
		result.append(String.format("%.2f", bytes / Math.pow(1000, exp)));
		result.append(" ");
		result.append(("kMGTPE").charAt(exp - 1));
		result.append(Messages.all(play.i18n.Lang.get()).getProperty("filesizeunitshort"));
		result.append("</abbr>");
		
		return result.toString();
	}
	
	public static String fromCamelCaseToSpaces(String value) {
		if (value == null) {
			return "";
		}
		if (value.length() == 0) {
			return "";
		}
		StringBuffer sb = new StringBuffer();
		
		for (int pos = 0; pos < value.length(); pos++) {
			String str = value.substring(pos, pos + 1);
			if (str.equals(str.toUpperCase())) {
				sb.append(" ");
			}
			sb.append(str.toLowerCase());
		}
		
		return sb.toString();
	}
}
