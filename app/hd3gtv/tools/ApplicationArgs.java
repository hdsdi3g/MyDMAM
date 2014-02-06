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
 * Copyright (C) hdsdi3g for hd3g.tv 2009-2014
 * 
*/
package hd3gtv.tools;

import java.util.ArrayList;

public class ApplicationArgs {
	
	private ArrayList<ApplicationArgsParam> registerparams;
	private String paramtoken = "-"; //$NON-NLS-1$
	private String keytoken = "="; //$NON-NLS-1$
	private String firstaction = null;
	
	/**
	 * Some examples :
	 * firstaction
	 * -simpleparam
	 * -param value
	 * -paramarray value0:value1:value2
	 * -param=name value
	 * -param value with spaces
	 */
	public ApplicationArgs(String[] args) {
		registerparams = new ArrayList<ApplicationArgsParam>();
		if (args == null) {
			return;
		}
		
		String paramname = null;
		StringBuffer paramcontent = new StringBuffer();
		
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			
			if (arg.startsWith(paramtoken)) {
				if (paramname != null) {
					if (paramcontent.length() > 0) {
						registerparams.add(new ApplicationArgsParam(paramname, paramcontent.toString()));
					} else {
						registerparams.add(new ApplicationArgsParam(paramname));
					}
				}
				
				paramcontent = new StringBuffer();
				paramname = arg;
				
			} else if (i == 0) {
				firstaction = arg;
				continue;
			} else {
				if (paramcontent.length() > 0) {
					paramcontent.append(" ");
				}
				paramcontent.append(arg);
			}
		}
		
		if (paramname != null) {
			if (paramcontent.length() > 0) {
				registerparams.add(new ApplicationArgsParam(paramname, paramcontent.toString()));
			} else {
				registerparams.add(new ApplicationArgsParam(paramname));
			}
		}
	}
	
	public String getFirstAction() {
		return firstaction;
	}
	
	private int getParampos(String name) {
		int result = -1;
		for (int i = 0; i < registerparams.size(); i++) {
			if (registerparams.get(i).name.equals(name)) {
				result = i;
			}
		}
		return result;
	}
	
	public boolean getParamExist(String name) {
		return (getParampos(name) > -1);
	}
	
	/**
	 * -param value
	 */
	public String getSimpleParamValue(String name) {
		int i = getParampos(name);
		if (i > -1) {
			ApplicationArgsParam p = registerparams.get(i);
			if (p.isSimpleParam()) {
				return null;
			} else {
				return p.value;
			}
		} else {
			return null;
		}
	}
	
	/**
	 * -param 5
	 */
	public int getSimpleIntegerParamValue(String name, int defaultvalue) {
		String value = getSimpleParamValue(name);
		if (value == null) {
			return defaultvalue;
		} else {
			return Integer.parseInt(value);
		}
	}
	
	/**
	 * Parameter like: -param value1 -param value2 -param value3
	 */
	public ArrayList<String> getMultipleParamsValue(String name) {
		ArrayList<String> result = new ArrayList<String>();
		for (int i = 0; i < registerparams.size(); i++) {
			if (registerparams.get(i).name.equals(name)) {
				ApplicationArgsParam p = registerparams.get(i);
				if (p.isSimpleParam() == false) {
					result.add(p.value);
				}
			}
		}
		return result;
	}
	
	/**
	 * Parameter like : -paramarray value:value2:value3
	 */
	public ArrayList<String> getParamArray(String name, String separator) {
		String values = getSimpleParamValue(name);
		if (values != null) {
			ArrayList<String> paramarray = new ArrayList<String>();
			String[] simplearray = values.split(separator);
			for (int i = 0; i < simplearray.length; i++) {
				paramarray.add(simplearray[i]);
			}
			return paramarray;
		} else {
			return null;
		}
	}
	
	/**
	 * -param=key value,
	 * baseparamname > "-param"
	 */
	public ArrayList<ApplicationArgsParam> getParamKey(String baseparamname) {
		ArrayList<ApplicationArgsParam> result = new ArrayList<ApplicationArgsParam>();
		String keyname;
		String keyvalue;
		for (int i = 0; i < registerparams.size(); i++) {
			boolean startok = registerparams.get(i).name.startsWith(baseparamname);
			if (startok) {
				boolean endok = registerparams.get(i).name.substring(baseparamname.length(), baseparamname.length() + 1).equals(keytoken);
				if (endok) {
					keyname = registerparams.get(i).name.substring(baseparamname.length() + 1);
					keyvalue = registerparams.get(i).value;
					result.add(new ApplicationArgsParam(keyname, keyvalue));
				}
			}
		}
		return result;
	}
	
	public boolean isEmpty() {
		return (registerparams.size() == 0);
	}
	
	public int getParamCount() {
		return registerparams.size();
	}
	
}
