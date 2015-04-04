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
package controllers;

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.web.JSXTransformer;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import org.mozilla.javascript.JavaScriptException;

import play.data.validation.Required;
import play.mvc.Controller;
import play.mvc.With;

@With(Secure.class)
public class AsyncJS extends Controller {
	
	// Secure.checkview()
	// TODO
	
	public static void index() throws Exception {
		// String js = JSXTransformer.global.transform("React.renderComponent(\n<h1>Hello, world!</h1>,\ndocument.getElementById('example')\n);");
	}
	
	public static void dynamicCompileJSX(@Required String ressource_name) {
		try {
			String jsx_compiled = JSXTransformer.getJSXContentFromURLList(ressource_name, true);
			response.setHeader("Content-Length", jsx_compiled.length() + "");
			response.setHeader("Content-Type", "text/javascript");
			renderText(jsx_compiled);
		} catch (Exception e) {
			if (e instanceof JavaScriptException) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PrintWriter pw = new PrintWriter(baos);
				pw.println("/** JSX ERROR */");
				pw.println("new function(){");
				pw.println("	$(document).ready(function() {");
				pw.println("		var message = {}");
				pw.println("		message.from = \"" + ressource_name + "\";");
				pw.println("		message.text = \"" + e.getMessage().replaceAll("\"", "'") + "\";");
				pw.println("		");
				pw.println("		");
				pw.println("		jsx_error_messages.push(message);");
				pw.println("	});");
				pw.println("}();");
				// e.printStackTrace();
				pw.close();
				
				String error_message = new String(baos.toByteArray());
				response.setHeader("Content-Length", error_message.length() + "");
				response.setHeader("Content-Type", "text/javascript");
				renderText(error_message);
			} else {
				Log2.log.error("JSX Transformer Error", e);
			}
		}
	}
	/*
	http://facebook.github.io/react/docs/getting-started.html
	 *
	if (Play.mode == Mode.PROD) {
	} else {
	}
		 
	Play, 2 modes pour JS
	- debug JS
	- prod

	React
	3 JS:
	- min prod = prod
	- debug Devel = devel
	- jsx transf = devel

	Jsx dans dossier à part non pub
	JS non compiles dans un dossier à part non pub
	Groovy/views déclare soit le ctrler avec les noms des jsx/JS en param, soit les liens publiques.
	+ les Lib en auto

	Check avec un module fait exprès.

	Mode devel:
	Appel tous les JS via un vrai ctrl Play
	Chaque JSX est compile in Fly et le JS est envoyé in Fly

	Mode prod:
	Au boot
	Compile les JSX
	Les optimises avec les autres JS
	Assemble tout dans un fichier unique.

	Todo: i18n avec React, avec les params.
	Todo: transformation JSX dynamique depuis groovy

	Ajax: une classe contrôleur unique, avec des helpers pour les Validations et les Gson, qui fourni une liste de points de comm a JS en se basant sur les droits du ctrl (Check).
	Surchargeable par module ?
	Via une déclaration de f via une interface qui fait tout en un (de/serial, throw).
	 * 
	 * */
}
