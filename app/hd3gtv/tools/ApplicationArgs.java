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
 * Copyright (C) hdsdi3g for hd3g.tv 2009-2013
 * 
*/
package hd3gtv.tools;

import java.util.ArrayList;

/**
 * @author hdsdi3g
 * @version 1.0
 */
public class ApplicationArgs {
	
	private ArrayList<ApplicationArgsParam> registerparams;
	private ArrayList<String> registeractions;
	private String paramtoken = "-"; //$NON-NLS-1$
	private String keytoken = "="; //$NON-NLS-1$
	private String lastaction;
	
	/**
	 * Some examples :
	 * action0 action1 action2
	 * -simpleparam
	 * -param value
	 * -paramarray value0:value1:value2
	 * -param=name value
	 * lastaction
	 */
	public ApplicationArgs(String[] args) {
		registerparams = new ArrayList<ApplicationArgsParam>();
		registeractions = new ArrayList<String>();
		boolean classicparammode = false; // for switch between "app action action action" and "app -param -param"
		
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				if (args[i] != null) {
					String arg = args[i];
					if (arg.startsWith(paramtoken)) {
						classicparammode = true;
						// is a parameter
						if ((i + 1) < args.length) {
							// there is something behind ...
							if ((args[i + 1] != null) && args[i + 1].startsWith(paramtoken)) {
								// ... is a parameter, we ignore it
								registerparams.add(new ApplicationArgsParam(arg));
							} else {
								// ... it is a value, lets take it
								registerparams.add(new ApplicationArgsParam(arg, args[i + 1]));
							}
						} else {
							// there is nothing left behind (last parameter)
							registerparams.add(new ApplicationArgsParam(arg));
						}
					} else {
						if ((i + 1) == args.length) {
							// the last
							lastaction = arg;
							registeractions.add(arg);
						} else {
							if (classicparammode == false) {
								registeractions.add(arg);
							}
						}
					}
				}
			}
		}
	}
	
	public String getFirstAction() {
		if (registeractions.size() > 0) {
			return registeractions.get(0);
		}
		return null;
	}
	
	public String getLastAction() {
		return lastaction;
	}
	
	public ArrayList<String> getActions() {
		return registeractions;
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
