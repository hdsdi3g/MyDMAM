package hd3gtv.mydmam.cli;

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.auth.AuthenticationUser;
import hd3gtv.mydmam.auth.AuthenticatorLocalsqlite;
import hd3gtv.tools.ApplicationArgs;

import java.io.File;
import java.util.List;

public class CliModuleAuthenticatorLocal implements CliModule {
	
	public String getCliModuleName() {
		return "localauth";
	}
	
	public String getCliModuleShortDescr() {
		return "Operate on authenticator local sqlite database";
	}
	
	private String getPassword(String text) {
		System.out.print(text);
		String password = new String(System.console().readPassword());
		if (password.trim().equals("")) {
			System.err.println("You don't set a password, cancel operation");
			System.exit(1);
			return null;
		}
		return password;
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		if (args.getParamExist("-autotest")) {
			AuthenticatorLocalsqlite.doInternalSecurityAutotest();
			return;
		}
		
		String filename = args.getSimpleParamValue("-f");
		if (filename == null) {
			showFullCliModuleHelp();
			System.err.println();
			System.err.println("You don't set filename !");
			System.err.println("Cancel operation");
			return;
		}
		
		String master_password_key = args.getSimpleParamValue("-key");
		if (master_password_key == null) {
			showFullCliModuleHelp();
			System.err.println();
			System.err.println("You don't set master password key !");
			System.err.println("Cancel operation");
			return;
		}
		
		AuthenticatorLocalsqlite db = new AuthenticatorLocalsqlite(new File(filename), master_password_key);
		
		String username = args.getSimpleParamValue("-u");
		if (username != null) {
			username = username.toLowerCase().trim();
		}
		
		String longname = args.getSimpleParamValue("-name");
		
		if (args.getParamExist("-add")) {
			boolean disabled = args.getParamExist("-disable");
			String password1 = getPassword("Enter password: ");
			String password2 = getPassword("Enter the same password: ");
			if (password1.equals(password2) == false) {
				System.err.println("You don't set the same passwords !");
				System.err.println("Cancel operation");
				System.exit(1);
				return;
			}
			
			if (username == null) {
				showFullCliModuleHelp();
				System.err.println("You don't set user !");
				System.err.println("Cancel operation");
				System.exit(1);
				return;
			}
			
			if (longname == null) {
				longname = username;
			}
			db.createUser(username, password1, longname, (disabled == false));
			
			Log2.log.info("Add user", db.getUserInformations(username));
			return;
		}
		if (args.getParamExist("-login")) {
			if (username == null) {
				showFullCliModuleHelp();
				System.err.println("You don't set user !");
				System.err.println("Cancel operation");
				System.exit(1);
				return;
			}
			AuthenticationUser user = db.getUser(username, getPassword("Enter " + username + " password: "));
			if (user == null) {
				Log2.log.error("Can't found " + username + " in database", null);
			} else {
				Log2.log.info("Test login", user);
			}
			return;
		}
		if (args.getParamExist("-delete")) {
			if (username == null) {
				showFullCliModuleHelp();
				System.err.println("You don't set user !");
				System.err.println("Cancel operation");
				System.exit(1);
				return;
			}
			
			db.deleteUser(username);
			
			if (db.getCreateDate(username) > 0) {
				System.err.println("Can't delete user");
				System.exit(1);
			}
			return;
		}
		if (args.getParamExist("-disable")) {
			if (username == null) {
				showFullCliModuleHelp();
				System.err.println("You don't set user !");
				System.err.println("Cancel operation");
				System.exit(1);
				return;
			}
			
			db.disableUser(username);
			
			if (db.isEnabledUser(username)) {
				System.err.println("Can't disable user");
				System.exit(1);
			}
			return;
		}
		if (args.getParamExist("-enable")) {
			if (username == null) {
				showFullCliModuleHelp();
				System.err.println("You don't set user !");
				System.err.println("Cancel operation");
				System.exit(1);
				return;
			}
			
			db.enableUser(username);
			
			if (db.isEnabledUser(username) == false) {
				System.err.println("Can't disable user");
				System.exit(1);
			}
			return;
		}
		if (args.getParamExist("-info")) {
			if (username == null) {
				showFullCliModuleHelp();
				System.err.println("You don't set user !");
				System.err.println("Cancel operation");
				System.exit(1);
				return;
			}
			Log2.log.info("About user", db.getUserInformations(username));
			return;
		}
		if (args.getParamExist("-list")) {
			boolean disabled = args.getParamExist("-disable");
			List<String> list = db.getUserList((disabled == false));
			for (int pos = 0; pos < list.size(); pos++) {
				System.out.println(list.get(pos));
			}
			return;
		}
		
		if (args.getParamExist("-passwd")) {
			if (username == null) {
				showFullCliModuleHelp();
				System.err.println("You don't set user !");
				System.err.println("Cancel operation");
				System.exit(1);
				return;
			}
			
			String password1 = getPassword("Enter new password: ");
			String password2 = getPassword("Enter the same password: ");
			if (password1.equals(password2) == false) {
				System.err.println("You don't set the same passwords !");
				System.err.println("Cancel operation");
				System.exit(1);
				return;
			}
			
			db.changeUserPassword(username, password1, db.isEnabledUser(username));
			
			if (db.getUser(username, password1) != null) {
				System.out.println("Change password ok");
			} else {
				System.err.println("Can't change password !");
				System.exit(1);
			}
			
			return;
		}
		
		if (args.getParamExist("-rename")) {
			if (username == null) {
				showFullCliModuleHelp();
				System.err.println("You don't set user !");
				System.err.println("Cancel operation");
				System.exit(1);
				return;
			}
			
			db.changeUserLongname(username, longname);
			
			String newname = db.getUserLongname(username);
			
			if (newname == null) {
				System.err.println("User not found");
				System.exit(1);
			} else if (newname.equals(longname) == false) {
				System.err.println("User not found");
				System.exit(2);
			}
			
			Log2.log.info("Rename user", db.getUserInformations(username));
			return;
		}
		
		showFullCliModuleHelp();
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage: " + getCliModuleName() + " -f conf/auth.db -key masterkey");
		System.out.println("and");
		System.out.println(" -add -u user [-name \"Full Name\"] [-disable]");
		System.out.println("  create user, -name user long name, -disable disabled by default");
		System.out.println(" -login -u user");
		System.out.println("  test login");
		System.out.println(" -delete -u user");
		System.out.println(" -enable -u user");
		System.out.println(" -disable -u user");
		System.out.println(" -info -u user");
		System.out.println(" -list [-disable]");
		System.out.println("  show users list, -disable to show disabled users");
		System.out.println(" -passwd -u user");
		System.out.println("  change password for user");
		System.out.println(" -rename -u user -name \"Full Name\"");
		System.out.println("  change user long name");
		System.out.println("Or");
		System.out.println(" -autotest");
		System.out.println("  test bCrypt and auth sqlite internal functions");
	}
}
