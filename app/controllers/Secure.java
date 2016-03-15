/*
 * This file is part of MyDMAM, inspired by Play! Framework Secure Module
*/
package controllers;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.accesscontrol.AccessControl;
import hd3gtv.mydmam.auth.AuthenticationBackend;
import hd3gtv.mydmam.auth.AuthenticationUser;
import hd3gtv.mydmam.auth.Authenticator;
import hd3gtv.mydmam.auth.InvalidAuthenticatorUserException;
import models.ACLGroup;
import models.ACLUser;
import models.UserProfile;
import play.Play;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.i18n.Lang;
import play.jobs.JobsPlugin;
import play.libs.Crypto;
import play.libs.Time;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;

/**
 * Imported from Play Secure Module
 */
public class Secure extends Controller {
	
	private static Gson gson = new Gson();
	private static Type type_alstring = new TypeToken<ArrayList<String>>() {
	}.getType();
	
	private volatile static HashMap<String, Long> users_pending_privileges_change = new HashMap<String, Long>();
	
	private static final long ttl_duration = (Configuration.global.getValue("auth", "privileges_ttl", 60) * 60000);// 1 hour by default.
	
	private static boolean isSessionHasThisPrivilege(String... privileges_to_check) {
		if (privileges_to_check == null) {
			return false;
		}
		if (privileges_to_check.length == 0) {
			return false;
		}
		ArrayList<String> privileges = getSessionPrivileges();
		if (privileges.isEmpty()) {
			return false;
		}
		for (int pos = 0; pos < privileges_to_check.length; pos++) {
			if (privileges.contains(privileges_to_check[pos])) {
				return true;
			}
		}
		return false;
	}
	
	static void changePrivilegesForUser(String username) {
		users_pending_privileges_change.put(username, System.currentTimeMillis() + ttl_duration);
	}
	
	public static ArrayList<String> getSessionPrivileges() {
		String username = session.get("username");
		if (users_pending_privileges_change.containsKey(username)) {
			users_pending_privileges_change.remove(username);
			setPrivilegesInSession(((ACLUser) ACLUser.findById(username)).group.role.privileges);
			return getSessionPrivileges();
		}
		
		String raw_privileges = Crypto.decryptAES(session.get("privileges"));
		ArrayList<String> privileges = gson.fromJson(raw_privileges, type_alstring);
		if (privileges.isEmpty()) {
			return privileges;
		}
		for (int pos = privileges.size() - 1; pos > -1; pos--) {
			if (privileges.get(pos).endsWith("-rnd")) {
				privileges.remove(pos);
			} else if (privileges.get(pos).startsWith("ttl-")) {
				long ttl_date = Long.valueOf(privileges.get(pos).substring(4));
				if (System.currentTimeMillis() > ttl_date) {
					setPrivilegesInSession(((ACLUser) ACLUser.findById(username)).group.role.privileges);
					return getSessionPrivileges();
				}
				privileges.remove(pos);
			}
		}
		return privileges;
	}
	
	private static void setPrivilegesInSession(String database_privileges) {
		ArrayList<String> privileges = gson.fromJson(database_privileges, type_alstring);
		if (privileges == null) {
			privileges = new ArrayList<String>(1);
		}
		privileges.add(String.valueOf(new Random().nextLong()) + "-rnd");
		
		long ttl_date = System.currentTimeMillis() + ttl_duration;
		privileges.add("ttl-" + String.valueOf(ttl_date));
		
		session.put("privileges", Crypto.encryptAES(gson.toJson(privileges)));
		// Log2.log.debug("Update privileges in session", new Log2Dump("raw json", gson.toJson(privileges)));
	}
	
	private static void getSessionPrivilegesListToDump(StringBuilder sb) {
		sb.append(" ");
		String session_privileges = session.get("privileges");
		if (session_privileges == null) {
			sb.append("privileges: (null)");
			return;
		}
		
		String raw_privileges = Crypto.decryptAES(session_privileges);
		ArrayList<String> privileges = gson.fromJson(raw_privileges, type_alstring);
		if (privileges.isEmpty()) {
			sb.append("privileges: (empty)");
			return;
		}
		long ttl_date = 0;
		for (int pos = privileges.size() - 1; pos > -1; pos--) {
			if (privileges.get(pos).endsWith("-rnd")) {
				privileges.remove(pos);
			} else if (privileges.get(pos).startsWith("ttl-")) {
				ttl_date = Long.valueOf(privileges.get(pos).substring(4));
				privileges.remove(pos);
			}
		}
		sb.append("privileges: ");
		for (int pos = 0; pos < privileges.size(); pos++) {
			sb.append(privileges.get(pos));
			if (pos + 1 < privileges.size()) {
				sb.append(", ");
			}
		}
		
		if (ttl_date > 0) {
			sb.append("privileges ttl: ");
			sb.append(Loggers.dateLog(ttl_date));
		}
	}
	
	/**
	 * This method checks that a profile is allowed to view this page/method.
	 */
	private static void check(Check check) throws Throwable {
		String[] chech_values = check.value();
		
		if (isSessionHasThisPrivilege(chech_values)) {
			return;
		}
		
		StringBuilder sb = new StringBuilder();
		for (int pos = 0; pos < chech_values.length; pos++) {
			sb.append(" chech: ");
			sb.append(chech_values[pos]);
		}
		Loggers.Play.error("Check failed: hack tentative. " + getUserSessionInformation() + sb.toString());
		forbidden();
	}
	
	/**
	 * @return true if privilege is empty
	 */
	public static boolean checkview(String privilege) {
		if (privilege.equals("")) {
			return true;
		}
		return isSessionHasThisPrivilege(privilege);
	}
	
	/**
	 * OR test
	 * @return true if no privileges in list
	 */
	public static boolean checkview(List<String> privileges) {
		if (privileges == null) {
			return true;
		}
		if (privileges.isEmpty()) {
			return true;
		}
		return isSessionHasThisPrivilege(privileges.toArray(new String[privileges.size()]));
	}
	
	private static String getUserSessionInformation() {
		StringBuilder sb = new StringBuilder();
		sb.append("request: ");
		
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
		
		sb.append(" username: ");
		sb.append(session.get("username"));
		sb.append(" longname: ");
		sb.append(session.get("longname"));
		getSessionPrivilegesListToDump(sb);
		
		return sb.toString();
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
			String url = "GET".equals(request.method) ? request.url : Play.ctxPath;
			if (url.endsWith("/")) {
				flash.put("url", url);
			} else {
				flash.put("url", url + "/");
			}
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
	
	private static void rejectUser() throws Throwable {
		flash.keep("url");
		flash.error("secure.error");
		params.flash();
		login();
	}
	
	public static void authenticate(@Required String username, @Required String password, String domainidx, boolean remember) throws Throwable {
		String remote_address = request.remoteAddress;
		
		if (Validation.hasErrors()) {
			rejectUser();
			return;
		}
		
		if (AccessControl.validThisIP(remote_address) == false) {
			Loggers.Play.warn("Refuse IP addr for user username: " + username + ", domainidx: " + domainidx + ", remote_address: " + remote_address);
			rejectUser();
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
			Loggers.Play.error("Can't login username: " + username + ", domainidx: " + domainidx + ", cause: " + e.getMessage() + " " + getUserSessionInformation());
		}
		
		if (authuser == null) {
			AccessControl.failedAttempt(remote_address, username);
			rejectUser();
		}
		
		username = authuser.getLogin();
		ACLUser acluser = ACLUser.findById(username);
		
		if (acluser == null) {
			ACLGroup group_guest = ACLGroup.findById(ACLGroup.NEWUSERS_NAME);
			if (group_guest == null) {
				rejectUser();
			}
			acluser = new ACLUser(group_guest, authuser.getSourceName(), username, authuser.getFullName());
		}
		
		AccessControl.releaseIP(remote_address);
		
		if (acluser.locked_account) {
			Loggers.Play.error("Locked account for user: " + username + ", domainidx: " + domainidx + " " + getUserSessionInformation());
			rejectUser();
		}
		
		if (acluser.fullname.equals(authuser.getFullName()) == false) {
			acluser.fullname = authuser.getFullName();
			acluser.lasteditdate = new Date();
		}
		
		acluser.lastloginipsource = remote_address;
		acluser.lastlogindate = new Date();
		acluser.save();
		
		/**
		 * Async db update : don't slowdown login/auth with this.
		 */
		JobsPlugin.executor.submit(new UserProfile.AsyncSave(authuser, Lang.getLocale().getLanguage()));
		
		session.put("username", acluser.login);
		session.put("longname", acluser.fullname);
		
		setPrivilegesInSession(acluser.group.role.privileges);
		
		if (remember) {
			Date expiration = new Date();
			String duration = "30d";
			expiration.setTime(expiration.getTime() + Time.parseDuration(duration));
			response.setCookie("rememberme", Crypto.sign(username + "-" + expiration.getTime()) + "-" + username + "-" + expiration.getTime(), duration);
		}
		
		/**
		 * Purge users_pending_privileges_change map.
		 */
		if (users_pending_privileges_change.containsKey(username)) {
			users_pending_privileges_change.remove(username);
		}
		if (users_pending_privileges_change.isEmpty() == false) {
			ArrayList<String> item_to_remove = new ArrayList<String>(users_pending_privileges_change.size());
			for (Map.Entry<String, Long> entry : users_pending_privileges_change.entrySet()) {
				if (System.currentTimeMillis() > entry.getValue()) {
					item_to_remove.add(entry.getKey());
				}
			}
			for (int pos = 0; pos < item_to_remove.size(); pos++) {
				users_pending_privileges_change.remove(item_to_remove.get(pos));
			}
		}
		
		redirectToOriginalURL();
	}
	
	public static void logout() throws Throwable {
		try {
			Loggers.Play.error("User went tries to sign off: " + getUserSessionInformation());
			session.clear();
			response.removeCookie("rememberme");
		} catch (Exception e) {
			Loggers.Play.error("Error during sign off: " + getUserSessionInformation());
			throw e;
		}
		flash.success("secure.logout");
		login();
	}
	
	private static void redirectToOriginalURL() throws Throwable {
		Loggers.Play.info("User has a successful authentication: " + getUserSessionInformation());
		
		String url = flash.get("url");
		if (url == null) {
			url = Play.ctxPath + "/";
		}
		redirect(url);
	}
	
	/**
	 * @return public addr/host name/"loopback"
	 */
	public static String getRequestAddress() {
		if (Controller.request.isLoopback == false) {
			return Controller.request.remoteAddress;
		} else {
			return "loopback";
		}
	}
}
