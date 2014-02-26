/*
 * This file is part of MyDMAM, inspired by Play! Framework Secure Module
*/
package controllers;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.auth.AuthenticationBackend;
import hd3gtv.mydmam.auth.AuthenticationUser;
import hd3gtv.mydmam.auth.Authenticator;
import hd3gtv.mydmam.auth.InvalidAuthenticatorUserException;

import java.util.Date;
import java.util.List;
import java.util.Random;

import models.ACLGroup;
import models.ACLUser;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
	 * This method checks that a profile is allowed to view this page/method.
	 */
	private static void check(Check check) throws Throwable {
		String[] chech_values = check.value();
		
		JSONParser jp = new JSONParser();
		try {
			JSONArray ja = (JSONArray) jp.parse(Crypto.decryptAES(session.get("privileges")));
			for (Object o : ja) {
				for (int pos = 0; pos < chech_values.length; pos++) {
					if (chech_values[pos].equalsIgnoreCase((String) o)) {
						return;
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		Log2Dump dump = new Log2Dump();
		dump.addAll(getUserSessionInformation());
		for (int pos = 0; pos < chech_values.length; pos++) {
			dump.add("chech", chech_values[pos]);
		}
		Log2.log.security("Check failed: hack tentative", new Exception(""), dump);
		forbidden();
	}
	
	public static boolean checkview(String privilege) {
		if (privilege.equals("")) {
			return true;
		}
		
		String privileges = Crypto.decryptAES(session.get("privileges"));
		if (privileges == null) {
			return false;
		}
		if (privileges.trim().equals("")) {
			return false;
		}
		JSONParser jp = new JSONParser();
		try {
			JSONArray ja = (JSONArray) jp.parse(privileges);
			if (ja.size() == 0) {
				return false;
			}
			for (Object o : ja) {
				if (privilege.equalsIgnoreCase((String) o)) {
					return true;
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return false;
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
		
		dump.add("username", session.get("username"));
		dump.add("longname", session.get("longname"));
		dump.add("privileges", Crypto.decryptAES(session.get("privileges")));
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
		
		Check check = getActionAnnotation(Check.class);
		if (check != null) {
			check(check);
		}
		check = getControllerInheritedAnnotation(Check.class);
		if (check != null) {
			check(check);
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
		
		boolean force_select_domain = AuthenticationBackend.isForce_select_domain();
		List<String> authenticators_domains = AuthenticationBackend.getAuthenticators_domains();
		
		render(force_select_domain, authenticators_domains);
	}
	
	public static void authenticate(@Required String username, @Required String password, String domainidx, boolean remember) throws Throwable {
		if (Validation.hasErrors()) {
			flash.keep("url");
			flash.error("secure.error");
			params.flash();
			login();
			return;
		}
		
		AuthenticationUser authuser = null;
		
		try {
			if (AuthenticationBackend.isForce_select_domain()) {
				Authenticator authenticator = null;
				try {
					authenticator = AuthenticationBackend.getAuthenticators().get(Integer.valueOf(domainidx));
				} catch (Exception e) {
				}
				authuser = AuthenticationBackend.authenticate(authenticator, username, password);
			} else {
				authuser = AuthenticationBackend.authenticate(username, password);
			}
		} catch (InvalidAuthenticatorUserException e) {
			Log2Dump dump = getUserSessionInformation();
			dump.add("username", username);
			dump.add("domainidx", domainidx);
			dump.add("cause", e.getMessage());
			Log2.log.security("Can't login", dump);
		}
		
		if (authuser == null) {
			flash.keep("url");
			flash.error("secure.error");
			params.flash();
			login();
			return;
		}
		
		username = authuser.getLogin();
		ACLUser acluser = ACLUser.findById(username);
		
		if (acluser == null) {
			ACLGroup group_guest = ACLGroup.findById(ACLGroup.NEWUSERS_NAME);
			if (group_guest == null) {
				flash.keep("url");
				flash.error("secure.error");
				params.flash();
				login();
				return;
			}
			acluser = new ACLUser(group_guest, authuser.getSourceName(), username, authuser.getFullName());
			acluser.save();
		}
		
		if (acluser.fullname.equals(authuser.getFullName()) == false) {
			acluser.fullname = authuser.getFullName();
			acluser.save();
		}
		
		session.put("username", acluser.login);
		session.put("longname", acluser.fullname);
		
		JSONArray ja = new JSONArray();
		try {
			JSONParser jp = new JSONParser();
			ja = (JSONArray) jp.parse(acluser.group.role.privileges);
			Random r = new Random();
			StringBuffer sb = new StringBuffer();
			sb.append(r.nextLong());
			sb.append("-rnd");
			ja.add(sb.toString());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		session.put("privileges", Crypto.encryptAES(ja.toJSONString()));
		
		if (remember) {
			Date expiration = new Date();
			String duration = "30d";
			expiration.setTime(expiration.getTime() + Time.parseDuration(duration));
			response.setCookie("rememberme", Crypto.sign(username + "-" + expiration.getTime()) + "-" + username + "-" + expiration.getTime(), duration);
		}
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
