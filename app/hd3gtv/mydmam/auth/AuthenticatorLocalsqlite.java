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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/

package hd3gtv.mydmam.auth;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.cli.CliModule;
import hd3gtv.tools.ApplicationArgs;
import hd3gtv.tools.BCrypt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.Security;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;

public class AuthenticatorLocalsqlite implements Authenticator, CliModule {
	
	private File dbfile;
	
	private IvParameterSpec salt;
	private SecretKey skeySpec;
	
	static {
		Security.addProvider(new BouncyCastleProvider());
	}
	
	public static void doInternalSecurityAutotest() throws Exception {
		System.out.println("Do BCrypt test...");
		
		// BCryptTest.main(null); //XXX
		
		System.out.println("Do MyDMAM AuthenticatorLocalsqlite test...");
		
		File db = new File("autotest.db");
		db.delete();
		
		AuthenticatorLocalsqlite authsql = new AuthenticatorLocalsqlite(db, "fakepassword");
		authsql.createUser("foo", "foopasswd", "Foo Name", true);
		authsql.createUser("bar", "barpasswd", "Bar Long Name", true);
		authsql.createUser("bannedusr", "bannedpasswd", "Banned Long Name", false);
		
		AuthenticationUser user = authsql.getUser("noname", "foopasswd");
		Assert.assertNull("Unknow user", user);
		
		try {
			user = authsql.getUser("foo", "badpassword");
			Assert.fail("Password must to be invalid");
		} catch (InvalidAuthenticatorUserException e) {
		}
		
		user = authsql.getUser("foo", "foopasswd");
		Assert.assertNotNull("Good user and passwod", user);
		Assert.assertEquals("Valid user name", user.getFullName(), "Foo Name");
		Assert.assertEquals("Valid user login", user.getLogin(), "foo");
		Assert.assertEquals("Valid user source name", user.getSourceName(), "sqlite:" + db.getPath());
		
		// TODO ...
		
		db.delete();
	}
	
	/**
	 * @param master_password_key extended password storage : AES(BCrypt(user_password, 12), SHA512(master_password_key))
	 */
	public AuthenticatorLocalsqlite(File dbfile, String master_password_key) throws IOException {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256", "BC");
			byte[] key = md.digest(master_password_key.getBytes("UTF-8"));
			
			skeySpec = new SecretKeySpec(key, "AES");
			salt = new IvParameterSpec(key, 0, 16);
		} catch (Exception e) {
			throw new IOException("Can't init digest password", e);
		}
		
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			throw new IOException("Can't load sqlite lib", e);
		}
		
		if (dbfile == null) {
			throw new NullPointerException("\"dbfile\" can't to be null");
		}
		if (dbfile.exists()) {
			if (dbfile.isFile() == false) {
				throw new IOException(dbfile.getPath() + " is not a file");
			}
			if (dbfile.canRead() == false) {
				throw new IOException("Can't read " + dbfile.getPath());
			}
		} else {
			try {
				Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbfile.getPath());
				Statement statement = connection.createStatement();
				
				StringBuffer query = new StringBuffer();
				query.append("CREATE TABLE IF NOT EXISTS users ");
				query.append("(");
				query.append("login TEXT PRIMARY KEY NOT NULL, ");
				query.append("password BLOB NOT NULL, ");
				query.append("name TEXT NOT NULL, ");
				query.append("created DATE NOT NULL, ");
				query.append("updated DATE NOT NULL, ");
				query.append("enabled BOOLEAN NOT NULL");
				query.append(")");
				
				statement.executeUpdate(query.toString());
				statement.close();
				connection.close();
			} catch (SQLException e) {
				throw new IOException("Can't open sqlite database", e);
			}
		}
		this.dbfile = dbfile;
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("local file", dbfile);
		return dump;
	}
	
	private byte[] getHashedPassword(String clear_password) {
		try {
			byte[] hashed = (BCrypt.hashpw(clear_password, BCrypt.gensalt(10))).getBytes("UTF-8");
			
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, salt);
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(cipher.update(hashed));
			baos.write(cipher.doFinal());
			
			return baos.toByteArray();
		} catch (Exception e) {
			Log2.log.error("Can't prepare password", e);
		}
		return null;
	}
	
	private boolean checkPassword(String candidate_password, byte[] raw_password) {
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, salt);
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(cipher.update(raw_password));
			baos.write(cipher.doFinal());
			
			return BCrypt.checkpw(candidate_password, new String(baos.toByteArray()));
		} catch (Exception e) {
			Log2.log.error("Can't extract hashed password", e);
		}
		return false;
	}
	
	public void createUser(String username, String password, String longname, boolean enabled) throws SQLException {
		Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbfile.getPath());
		
		PreparedStatement pstatement = connection.prepareStatement("INSERT INTO users (login, password, name, created, updated, enabled) values (?,?,?,?,?,?);");
		pstatement.setString(1, username);
		pstatement.setBytes(2, getHashedPassword(password));
		pstatement.setString(3, longname);
		pstatement.setDate(4, new Date(System.currentTimeMillis()));
		pstatement.setDate(5, new Date(System.currentTimeMillis()));
		pstatement.setBoolean(6, enabled);
		
		pstatement.executeUpdate();
		connection.close();
	}
	
	public AuthenticationUser getUser(final String username, String password) throws NullPointerException, IOException, InvalidAuthenticatorUserException {
		try {
			Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbfile.getPath());
			PreparedStatement pstatement = connection.prepareStatement("SELECT password, name FROM users WHERE login = ? AND enabled = 1");
			pstatement.setString(1, username);
			
			ResultSet res = pstatement.executeQuery();
			while (res.next()) {
				if (checkPassword(password, res.getBytes("password"))) {
					final String name = res.getString("name");
					connection.close();
					
					return new AuthenticationUser() {
						public Log2Dump getLog2Dump() {
							Log2Dump dump = new Log2Dump();
							dump.add("username", username);
							dump.add("name", name);
							dump.add("dbfile", dbfile);
							return dump;
						}
						
						public String getSourceName() {
							return "sqlite:" + dbfile.getPath();
						}
						
						public String getLogin() {
							return username;
						}
						
						public String getFullName() {
							return name;
						}
					};
				} else {
					throw new InvalidAuthenticatorUserException("User exists in database, but password is invalid", null);
				}
			}
			connection.close();
		} catch (SQLException e) {
			throw new IOException(e);
		}
		return null;
	}
	
	public void deleteUser(String username) throws SQLException {
		// TODO
	}
	
	public boolean disableUser(String username) throws SQLException {
		// TODO
		return false;
	}
	
	public boolean enableUser(String username) throws SQLException {
		// TODO
		return false;
	}
	
	public boolean changeUserPassword(String username, String password) throws SQLException {
		// TODO
		return false;
	}
	
	public boolean changeUserLongname(String username, String longname) throws SQLException {
		// TODO
		return false;
	}
	
	public List<String> getUserList(boolean mustenabled) throws SQLException {
		// TODO
		return null;
	}
	
	public Log2Dump getUserInformations(String username) throws SQLException {
		// TODO
		return null;
	}
	
	public String getCliModuleName() {
		return "localauth";
	}
	
	public String getCliModuleShortDescr() {
		return "Operate on authenticator local sqlite database";
	}
	
	@Override
	public void execCliModule(ApplicationArgs args) throws Exception {
		// TODO Auto-generated method stub
		// TODO password ??
	}
	
	@Override
	public void showFullCliModuleHelp() {
		// TODO Auto-generated method stub
		
	}
	
}
