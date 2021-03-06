/*
 * This file is part of MyDMAM, inspired by Play! Framework Secure Module
*/
package controllers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.accesscontrol.AccessControl;
import hd3gtv.mydmam.auth.UserNG;
import hd3gtv.mydmam.web.DisconnectedUser;
import play.cache.Cache;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.i18n.Lang;
import play.libs.Crypto;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Scope.Session;

/**
 * Imported from Play Secure Module
 */
public class Secure extends Controller {
	
	public static HashSet<String> getSessionPrivileges() throws DisconnectedUser {
		String username = connected();
		if (username == null) {
			throw new DisconnectedUser("(No set)");
		}
		
		@SuppressWarnings("unchecked")
		HashSet<String> privileges = Cache.get("user:" + username + ":privileges", HashSet.class);
		
		if (privileges != null) {
			Cache.set("user:" + username + ":privileges", privileges, MyDMAM.getPlayBootstrapper().getSessionTTL());
			return privileges;
		}
		
		Session.current().clear();
		
		throw new DisconnectedUser(username);
	}
	
	/**
	 * This method checks that a profile is allowed to view this page/method.
	 */
	private static void check(Check check) {
		String[] privileges_to_check = check.value();
		
		if (privileges_to_check == null) {
			Loggers.Play.error("Check failed: hack tentative or disconnected user. " + getUserSessionInformation());
			forbidden();
			return;
		}
		
		if (privileges_to_check.length == 0) {
			Loggers.Play.error("Check failed: hack tentative or disconnected user. " + getUserSessionInformation());
			forbidden();
			return;
		}
		
		HashSet<String> privileges;
		try {
			privileges = getSessionPrivileges();
		} catch (DisconnectedUser e) {
			Loggers.Play.debug("User was disconnected. " + e.getMessage());
			login();
			return;
		}
		if (privileges.isEmpty()) {
			Loggers.Play.error("Check failed: hack tentative or disconnected user. " + getUserSessionInformation());
			forbidden();
			return;
		}
		
		for (int pos = 0; pos < privileges_to_check.length; pos++) {
			if (privileges.contains(privileges_to_check[pos])) {
				return;
			}
		}
		
		StringBuilder sb = new StringBuilder();
		for (int pos = 0; pos < privileges_to_check.length; pos++) {
			sb.append(" chech: ");
			sb.append(privileges_to_check[pos]);
		}
		
		Loggers.Play.error("Check failed: hack tentative or disconnected user. " + getUserSessionInformation() + sb.toString());
		forbidden();
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
		
		String username = connected();
		if (username != null) {
			sb.append(" username: ");
			sb.append(username);
			sb.append(", privileges: ");
			try {
				sb.append(getSessionPrivileges());
			} catch (DisconnectedUser e) {
				sb.append(e.getMessage());
			}
		}
		
		return sb.toString();
	}
	
	/**
	 * This method returns the current connected username
	 * @return null, if no user
	 */
	public static String connected() {
		String raw_user = Session.current().get("username");
		if (raw_user == null) {
			return null;
		}
		return Crypto.decryptAES(raw_user);
	}
	
	/**
	 * Indicate if a user is currently connected
	 * @return true if the user is connected
	 */
	public static boolean isConnected() {
		return Session.current().contains("username");
	}
	
	/*
	 * ===================
	 * START OF CONTROLLER
	 * ===================
	 */
	
	@Before(unless = { "login", "authenticate", "logout" })
	static void checkAccess() throws Throwable {
		if (isConnected() == false) {
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
	
	public static void login() {
		boolean force_select_domain = MyDMAM.getPlayBootstrapper().getAuth().isForceSelectDomain();
		List<String> authenticators_domains = MyDMAM.getPlayBootstrapper().getAuth().getDeclaredDomainList();
		
		render(force_select_domain, authenticators_domains);
	}
	
	private static void rejectUser() throws Throwable {
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
		
		UserNG authuser = null;
		
		if (MyDMAM.getPlayBootstrapper().getAuth().isForceSelectDomain()) {
			String domain_name = null;
			
			try {
				domain_name = MyDMAM.getPlayBootstrapper().getAuth().getDeclaredDomainList().get(Integer.valueOf(domainidx));
			} catch (Exception e) {
			}
			authuser = MyDMAM.getPlayBootstrapper().getAuth().authenticateWithThisDomain(remote_address, username.trim().toLowerCase(), password, domain_name, Lang.getLocale().getLanguage());
		} else {
			authuser = MyDMAM.getPlayBootstrapper().getAuth().authenticate(remote_address, username.trim().toLowerCase(), password, Lang.getLocale().getLanguage());
		}
		
		if (authuser == null) {
			Loggers.Play.error("Can't login username: " + username + ", domainidx: " + domainidx + ", " + getUserSessionInformation());
			AccessControl.failedAttempt(remote_address, username);
			rejectUser();
		}
		
		username = authuser.getKey();
		
		AccessControl.releaseIP(remote_address);
		
		Session.current().put("username", Crypto.encryptAES(username));
		
		Cache.set("user:" + username + ":privileges", authuser.getUser_groups_roles_privileges(), MyDMAM.getPlayBootstrapper().getSessionTTL());
		
		String long_name = authuser.getFullname();
		if (long_name == null) {
			long_name = authuser.getName();
		}
		
		Loggers.Play.info(long_name + " has a successful authentication, with privileges: " + getSessionPrivileges().toString() + ". User key: " + username);
		
		redirect("Application.index");
	}
	
	public static void logout() throws Throwable {
		try {
			String username = connected();
			if (username != null) {
				Cache.delete("user:" + username + ":privileges");
			}
			Session.current().clear();
			
			UserNG user = MyDMAM.getPlayBootstrapper().getAuth().getByUserKey(username);
			if (user == null) {
				Loggers.Play.info("User " + username + " went tries to sign off.");
			} else {
				String long_name = user.getFullname();
				if (long_name == null) {
					long_name = user.getName();
				}
				Loggers.Play.info(long_name + " went tries to sign off.");
			}
			
		} catch (Exception e) {
			Loggers.Play.error("Error during sign off: " + getUserSessionInformation());
			throw e;
		}
		flash.success("secure.logout");
		login();
	}
	
	/**
	 * @return public addr/host name/"loopback"
	 */
	public static String getRequestAddress() {
		if (Request.current().isLoopback == false) {
			try {
				if (InetAddress.getByName(Request.current().remoteAddress).isLoopbackAddress()) {
					return "loopback";
				}
			} catch (UnknownHostException e) {
			}
			return Request.current().remoteAddress;
		} else {
			return "loopback";
		}
	}
}
