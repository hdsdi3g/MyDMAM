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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package ext;

import groovy.lang.Closure;
import hd3gtv.mydmam.web.JSXTransformer;
import hd3gtv.mydmam.web.JsCompile;

import java.io.PrintWriter;
import java.util.Map;

import play.templates.FastTags;
import play.templates.GroovyTemplate.ExecutableTemplate;
import play.templates.JavaExtensions;

/**
 * Called with #{jsxtransform} myJs(); #{/jsxtransform} and jscompile, this will never be cached, and will be executed, all time,
 * and even in Prod mode, else caching will cache all sub templates vars, inside this tag (and embedded dynamic content will be frozen).
 */
public class MydmamTags extends FastTags {
	
	public static void _jsxtransform(Map<?, ?> args, Closure<?> body, PrintWriter out, ExecutableTemplate template, int fromLine) {
		out.println("<script charset=\"UTF-8\" type=\"text/javascript\"><!--");
		String js_result = JSXTransformer.global.transform(JavaExtensions.toString(body), true, template.template.getName() + " (from line " + fromLine + ")");
		if (JsCompile.COMPILE_JS) {
			out.println(JsCompile.compileJSOnTheFly(js_result));
		} else {
			out.println(js_result);
		}
		out.println("--></script>");
	}
	
	public static void _jscompile(Map<?, ?> args, Closure<?> body, PrintWriter out, ExecutableTemplate template, int fromLine) {
		out.println("<script charset=\"UTF-8\" type=\"text/javascript\"><!--");
		if (JsCompile.COMPILE_JS) {
			out.println(JsCompile.compileJSOnTheFly(JavaExtensions.toString(body)));
		} else {
			out.println(JavaExtensions.toString(body));
		}
		out.println("--></script>");
	}
	
}
