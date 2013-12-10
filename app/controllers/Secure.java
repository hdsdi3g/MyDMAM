/*
 * This file is part of MyDMAM, inspired by Play! Framework Secure Module
*/
package controllers;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;

import java.util.Date;

import play.Play;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.libs.Crypto;
import play.libs.Time;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;

/**
 * Imported from Play Secure Module
 */
public class Secure extends Controller {
	
	/**
	 * This method is called during the authentication process. This is where you check if
	 * the user is allowed to log in into the system. This is the actual authentication process
	 * against a third party system (most of the time a DB).
	 * @param username
	 * @param password
	 * @return true if the authentication process succeeded
	 */
	private static boolean authenticate(String username, String password) {
		// User user = User.find("byEmail", username).first();
		// return user != null && user.password.equals(password);
		
		return true;
	}
	
	/**
	 * This method checks that a profile is allowed to view this page/method. This method is called prior
	 * to the method's controller annotated with the @Check method.
	 * @param profile
	 * @return true if you are allowed to execute this controller method.
	 */
	private static boolean check(String profile) {
		/*Log2Dump dump = new Log2Dump();
		dump.add("profile", profile);
		dump.addAll(getUserSessionInformation());
		Log2.log.security("Check", dump);*/
		return true;
	}
	
	private static Log2Dump getUserSessionInformation() {
		Log2Dump dump = new Log2Dump();
		StringBuffer sb = new StringBuffer();
		if (request.isLoopback == false) {
			sb.append(request.remoteAddress);
		} else {
			sb.append("loopback");
		}
		sb.append(" ");
		sb.append(request.method);
		sb.append(" ");
		if (request.secure) {
			sb.append("https://");
		} else {
			sb.append("http://");
		}
		sb.append(request.host);
		sb.append(request.url);
		sb.append(request.querystring);
		
		if (request.isAjax()) {
			sb.append(" AJAX");
		}
		
		sb.append(" > ");
		sb.append(request.action);
		
		dump.add("request", sb);
		
		dump.addAll("session", session.all());
		return dump;
	}
	
	/**
	 * This method returns the current connected username
	 * @return
	 */
	public static String connected() {
		return session.get("username");
	}
	
	/**
	 * Indicate if a user is currently connected
	 * @return true if the user is connected
	 */
	public static boolean isConnected() {
		return session.contains("username");
	}
	
	/*
	 * ===================
	 * START OF CONTROLLER
	 * ===================
	 */
	
	@Before(unless = { "login", "authenticate", "logout" })
	static void checkAccess() throws Throwable {
		// Authent
		if (!session.contains("username")) {
			flash.put("url", "GET".equals(request.method) ? request.url : Play.ctxPath + "/"); // seems a good default
			login();
		}
		// Checks
		Check check = getActionAnnotation(Check.class);
		if (check != null) {
			check(check);
		}
		check = getControllerInheritedAnnotation(Check.class);
		if (check != null) {
			check(check);
		}
	}
	
	private static void check(Check check) throws Throwable {
		for (String profile : check.value()) {
			if (check(profile) == false) {
				Log2.log.security("Bad check right", getUserSessionInformation());
				forbidden();
			}
		}
	}
	
	public static void login() throws Throwable {
		Http.Cookie remember = request.cookies.get("rememberme");
		if (remember != null) {
			int firstIndex = remember.value.indexOf("-");
			int lastIndex = remember.value.lastIndexOf("-");
			if (lastIndex > firstIndex) {
				String sign = remember.value.substring(0, firstIndex);
				String restOfCookie = remember.value.substring(firstIndex + 1);
				String username = remember.value.substring(firstIndex + 1, lastIndex);
				String time = remember.value.substring(lastIndex + 1);
				Date expirationDate = new Date(Long.parseLong(time)); // surround with try/catch?
				Date now = new Date();
				if (expirationDate == null || expirationDate.before(now)) {
					logout();
				}
				if (Crypto.sign(restOfCookie).equals(sign)) {
					session.put("username", username);
					redirectToOriginalURL();
				}
			}
		}
		flash.keep("url");
		render();
	}
	
	public static void authenticate(@Required String username, String password, boolean remember) throws Throwable {
		// Check tokens
		Boolean allowed = false;
		allowed = authenticate(username, password);
		if (Validation.hasErrors() || !allowed) {
			flash.keep("url");
			flash.error("secure.error");
			params.flash();
			login();
		}
		// Mark user as connected
		session.put("username", username);
		// Remember if needed
		if (remember) {
			Date expiration = new Date();
			String duration = "30d"; // maybe make this override-able
			expiration.setTime(expiration.getTime() + Time.parseDuration(duration));
			response.setCookie("rememberme", Crypto.sign(username + "-" + expiration.getTime()) + "-" + username + "-" + expiration.getTime(), duration);
			
		}
		// Redirect to the original URL (or /)
		redirectToOriginalURL();
	}
	
	public static void logout() throws Throwable {
		try {
			Log2.log.security("User went tries to sign off", getUserSessionInformation());
			session.clear();
			response.removeCookie("rememberme");
		} catch (Exception e) {
			Log2.log.security("Error during sign off", e, getUserSessionInformation());
			throw e;
		}
		flash.success("secure.logout");
		login();
	}
	
	private static void redirectToOriginalURL() throws Throwable {
		Log2.log.security("User has a successful authentication", getUserSessionInformation());
		
		String url = flash.get("url");
		if (url == null) {
			url = Play.ctxPath + "/";
		}
		redirect(url);
	}
	
}
