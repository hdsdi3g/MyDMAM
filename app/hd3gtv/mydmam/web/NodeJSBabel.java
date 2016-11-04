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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.mydmam.web;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.CopyMove;
import hd3gtv.tools.ExecBinaryPath;
import hd3gtv.tools.ExecprocessGettext;
import hd3gtv.tools.ExecprocessOutputstreamHandler;

public class NodeJSBabel {
	
	public static void main(String[] args) throws Exception {// XXX
		NodeJSBabel b = new NodeJSBabel();
		System.out.println(b.operate("//TEST\r\nprocess.exit( 0 ) ;\r\nvar dd = <Hello />;\r\n", Operation.TRANSFORM));
		System.out.println(b.operate("console.log();\r\ntest(\"aaa\");\r\n//TEST\r\n", Operation.REDUCE));
	}
	
	public static final Logger log = Loggers.NodeJSBabel;
	public static final String MIN_NODEJS_VERSION = "6.9";
	public static final String MIN_NPM_VERSION = "3.10";
	public static final String MIN_BABELCORE_VERSION = "6.18.2";
	public static final String BABEL_VERSION_JS_FILE = "_babel-show-version.js";
	
	public enum Operation {
		TRANSFORM, REDUCE, TRANSFORM_REDUCE;
	}
	
	private File babel_js;
	private File node_executable;
	
	public NodeJSBabel() throws Exception {
		node_executable = ExecBinaryPath.get("node");
		
		babel_js = new File(MyDMAM.APP_ROOT_PLAY_DIRECTORY.getAbsolutePath() + File.separator + "node_modules" + File.separator + "babel-cli" + File.separator + "bin" + File.separator + "babel.js");
		try {
			CopyMove.checkExistsCanRead(babel_js);
		} catch (Exception e) {
			log.debug("Can't found babel JS", e);
			
			checkNodeVersion();
			checkNPMVersion();
			setupBabel();
			checkAndUpdateBabelVersion(true);
		}
		
		if (Configuration.global.getValueBoolean("play", "no_check_babel_at_start") == false) {
			checkAndUpdateBabelVersion(false);
		}
	}
	
	private void checkAndUpdateBabelVersion(boolean dontsetupiferror) throws IOException {
		try {
			File show_version_js = new File(babel_js.getParentFile().getAbsolutePath() + File.separator + BABEL_VERSION_JS_FILE);
			
			if (show_version_js.exists() == false) {
				FileUtils.writeLines(show_version_js, Arrays.asList("require(\"../lib/babel-node\");", "console.log(require(\"babel-core\").version);", "process.exit(0);"), false);
			}
			
			ExecprocessGettext exec = new ExecprocessGettext(node_executable, Arrays.asList(show_version_js.getAbsolutePath()));
			exec.setWorkingDirectory(MyDMAM.APP_ROOT_PLAY_DIRECTORY);
			exec.setEndlinewidthnewline(false);
			exec.start();
			
			String babel_version = exec.getResultstdout().toString().trim();
			
			if (MyDMAM.versionCompare(babel_version, MIN_BABELCORE_VERSION) < 0) {
				if (dontsetupiferror) {
					log.error("Current installed Babel version is to low (" + babel_version + " < " + MIN_BABELCORE_VERSION + " ) for MyDMAM.");
					return;
				} else {
					log.warn("Current installed Babel version is to low (" + babel_version + " < " + MIN_BABELCORE_VERSION + " ) for MyDMAM: update Babel setup.");
					FileUtils.forceDelete(new File(MyDMAM.APP_ROOT_PLAY_DIRECTORY.getAbsolutePath() + File.separator + "node_modules"));
					setupBabel();
					checkAndUpdateBabelVersion(true);
				}
			} else {
				log.debug("Use Babel core version " + babel_version);
			}
		} catch (IOException e) {
			log.warn("Can't execute Babel core");
			checkNodeVersion();
			checkNPMVersion();
			throw e;
		}
	}
	
	private void setupBabel() throws IOException {
		log.info("Install Babel in " + MyDMAM.APP_ROOT_PLAY_DIRECTORY);
		
		List<String> params = Arrays.asList("install", "--save-dev", "babel-cli", "babel-preset-react", "babel-plugin-transform-remove-console", "babili");
		File npm_executable = ExecBinaryPath.get("npm");
		ExecprocessGettext exec = new ExecprocessGettext(npm_executable, params);
		exec.setWorkingDirectory(MyDMAM.APP_ROOT_PLAY_DIRECTORY);
		exec.setEndlinewidthnewline(true);
		exec.start();
		
		log.debug("Result stdout: " + exec.getResultstdout().toString().trim());
		log.debug("Result stderr: " + exec.getResultstderr().toString().trim());
	}
	
	private void checkNodeVersion() throws IOException {
		List<String> params = Arrays.asList("-v");
		ExecprocessGettext exec = new ExecprocessGettext(node_executable, params);
		exec.start();
		String node_version = exec.getResultstdout().toString().trim();
		
		if (MyDMAM.versionCompare(node_version, MIN_NODEJS_VERSION) < 0) {
			throw new IOException(
					"Current installed NodeJS version is to low (" + node_version + "). Please update it before use (>= " + MIN_NODEJS_VERSION + ") from " + node_executable.getAbsolutePath());
		}
		
		log.debug("Use NodeJS version " + node_version);
	}
	
	private void checkNPMVersion() throws IOException {
		List<String> params = Arrays.asList("-v");
		File npm_executable = ExecBinaryPath.get("npm");
		ExecprocessGettext exec = new ExecprocessGettext(npm_executable, params);
		exec.start();
		String npm_version = exec.getResultstdout().toString().trim();
		
		if (MyDMAM.versionCompare(npm_version, MIN_NPM_VERSION) < 0) {
			throw new IOException("Current installed npm version is to low (" + npm_version + "). Please update it before use (>= " + MIN_NPM_VERSION + ") from " + npm_executable.getAbsolutePath());
		}
		
		log.debug("Use npm version " + npm_version);
	}
	
	private ExecprocessGettext babelExec(List<String> babel_params) throws IOException {
		ArrayList<String> params = new ArrayList<>(Arrays.asList(babel_js.getAbsolutePath()));
		params.addAll(babel_params);
		ExecprocessGettext exec = new ExecprocessGettext(node_executable, params);
		exec.setWorkingDirectory(MyDMAM.APP_ROOT_PLAY_DIRECTORY);
		exec.setEndlinewidthnewline(true);
		return exec;
	}
	
	private static void getCmdLineByOperation(Operation operation, ArrayList<String> action) {
		if (operation == Operation.TRANSFORM) {
			action.addAll(Arrays.asList("--presets", "react"));
		} else if (operation == Operation.REDUCE) {
			action.addAll(Arrays.asList("--presets=babili", "--no-babelrc", "--plugins=transform-remove-console"));
		} else if (operation == Operation.TRANSFORM_REDUCE) {
			action.addAll(Arrays.asList("--presets", "react,babili", "--no-babelrc"));
		}
		// Arrays.asList("--compact=true", "--minified=true", "--comments=true")
	}
	
	public String operate(String source, Operation operation) throws IOException, BabelException {
		ArrayList<String> action = new ArrayList<>();
		getCmdLineByOperation(operation, action);
		
		ExecprocessGettext exec = babelExec(action);
		exec.setOutputstreamhandler(new ExecprocessOutputstreamHandler(IOUtils.toInputStream(source)));
		try {
			exec.start();
		} catch (Exception e) {
			throw BabelException.create(exec.getResultstderr().toString());
		}
		
		return exec.getResultstdout().toString();
	}
	
}
