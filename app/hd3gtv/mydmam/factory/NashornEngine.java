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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.factory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.io.IOUtils;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.CopyMove;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

public class NashornEngine {
	
	private ScriptEngine engine;
	private HashMap<String, Object> bindings;
	
	public NashornEngine() {
		// engine = new ScriptEngineManager().getEngineByName("nashorn");
		engine = new NashornScriptEngineFactory().getScriptEngine(classname -> {
			return false;
		});
		bindings = new HashMap<>();
		bindings.put("console", new JSToolkitConsole(Loggers.Factory));
		
		engine.setBindings(new SimpleBindings(bindings), ScriptContext.ENGINE_SCOPE);
	}
	
	/*
	    binds.put("gson_simple", MyDMAM.gson_kit.getGsonSimple());
		binds.put("content", "Hello world !");
		binds.put("uu", "3");
	*/
	public HashMap<String, Object> getBindings() {
		return bindings;
	}
	
	// ScriptObjectMirror eval = (ScriptObjectMirror) engine.eval("uu=\"5\"; gson_simple.toJson(content); [uu, \"6\"]");
	// System.out.println(eval.values().stream().findFirst().get() + " " + engine.get("uu"));
	public Object eval(File js_file) throws NullPointerException, IOException, ScriptException {
		CopyMove.checkExistsCanRead(js_file);
		CopyMove.checkIsFile(js_file);
		Loggers.Factory.debug("Load and eval JS file: " + js_file);
		
		InputStreamReader isr = new InputStreamReader(new FileInputStream(js_file), MyDMAM.UTF8);
		try {
			Object result = engine.eval(isr);
			IOUtils.closeQuietly(isr);
			return result;
		} catch (ScriptException e) {
			IOUtils.closeQuietly(isr);
			throw e;
		}
	}
	
}
