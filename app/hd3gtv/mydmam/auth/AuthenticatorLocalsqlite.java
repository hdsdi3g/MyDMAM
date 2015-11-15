/*
/ * This file is part of MyDMAM.
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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import hd3gtv.mydmam.Loggers;
import hd3gtv.tools.BCryptTest;

public class AuthenticatorLocalsqlite implements Authenticator {
	
	private File dbfile;
	private Password password;
	
	public static void doInternalSecurityAutotest() throws Exception {
		System.out.println("Do BCrypt test...");
		
		BCryptTest.main(null);
		System.out.println("Do MyDMAM AuthenticatorLocalsqlite test...");
		
		File db = new File("autotest.db");
		db.delete();
		AuthenticatorLocalsqlite authsql = new AuthenticatorLocalsqlite(db, "fakepassword");
		
		try {
			long startup = System.currentTimeMillis();
			
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
			
			Assert.assertTrue(authsql.getCreateDate("foo") > startup);
			
			long lastupdate = authsql.getLastUpdateDate("foo");
			Assert.assertTrue(lastupdate > startup);
			Assert.assertTrue(authsql.getLastUpdateDate("noneuser") == -1);
			
			lastupdate = authsql.getLastUpdateDate("bannedusr");
			
			Assert.assertFalse("Banned", authsql.isEnabledUser("bannedusr"));
			authsql.enableUser("bannedusr");
			Assert.assertTrue("Not banned", authsql.isEnabledUser("bannedusr"));
			authsql.disableUser("bannedusr");
			Assert.assertFalse("Back to banned", authsql.isEnabledUser("bannedusr"));
			
			Assert.assertTrue(authsql.getLastUpdateDate("bannedusr") > lastupdate);
			
			List<String> list = authsql.getUserList(false);
			Assert.assertEquals(3, list.size());
			list = authsql.getUserList(true);
			Assert.assertEquals(2, list.size());
			
			authsql.deleteUser("bannedusr");
			
			list = authsql.getUserList(false);
			Assert.assertEquals(2, list.size());
			list = authsql.getUserList(true);
			Assert.assertEquals(2, list.size());
			
			user = authsql.getUser("bannedusr", "");
			Assert.assertNull("Unknow user", user);
			
			authsql.changeUserPassword("bar", "newpassword0", false);
			Assert.assertFalse("Banned", authsql.isEnabledUser("bar"));
			authsql.changeUserPassword("bar", "newpassword", true);
			Assert.assertTrue("Not banned", authsql.isEnabledUser("bar"));
			user = authsql.getUser("bar", "newpassword");
			Assert.assertNotNull("Bad password", user);
			
			try {
				authsql.createUser("bar", "otherpassword", "I can not exist twice", true);
				Assert.fail("The user is twice");
			} catch (SQLException e) {
				Assert.assertEquals("Bad error cause", "[SQLITE_CONSTRAINT]  Abort due to constraint violation (column login is not unique)", e.getMessage());
			}
			
			user = authsql.getUser("bar", "newpassword");
			Assert.assertNotNull(user);
			
			Assert.assertEquals("Bar Long Name", authsql.getUserLongname("bar"));
			authsql.changeUserLongname("bar", "New Bar Long Name");
			Assert.assertEquals("New Bar Long Name", authsql.getUserLongname("bar"));
			
			db.delete();
			System.out.println("Done");
		} catch (AssertionError ae) {
			ae.printStackTrace();
			System.out.println("FAIL");
		}
		
	}
	
	/**
	 * @param master_password_key extended password storage : AES(BCrypt(user_password, 10), SHA-256(master_password_key))
	 */
	public AuthenticatorLocalsqlite(File dbfile, String master_password_key) throws IOException {
		try {
			password = new Password(master_password_key);
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
		
		try {
			Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbfile.getPath());
			connection.close();
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}
	
	private Connection createConnection() throws SQLException {
		return DriverManager.getConnection("jdbc:sqlite:" + dbfile.getPath());
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("local file: ");
		sb.append(dbfile);
		return sb.toString();
	}
	
	public File getDbfile() {
		return dbfile;
	}
	
	public String getMaster_password_key() {
		return password.getMaster_password_key();
	}
	
	public void createUser(String username, String password, String longname, boolean enabled) throws SQLException {
		Connection connection = createConnection();
		PreparedStatement pstatement = connection.prepareStatement("INSERT INTO users (login, password, name, created, updated, enabled) values (?,?,?,?,?,?);");
		pstatement.setString(1, username);
		pstatement.setBytes(2, this.password.getHashedPassword(password));
		pstatement.setString(3, longname);
		pstatement.setDate(4, new Date(System.currentTimeMillis()));
		pstatement.setDate(5, new Date(System.currentTimeMillis()));
		pstatement.setBoolean(6, enabled);
		pstatement.executeUpdate();
		connection.close();
	}
	
	public AuthenticationUser getUser(final String username, String password) throws NullPointerException, IOException, InvalidAuthenticatorUserException {
		try {
			Connection connection = createConnection();
			PreparedStatement pstatement = connection.prepareStatement("SELECT password, name FROM users WHERE login = ? AND enabled = 1");
			pstatement.setString(1, username);
			
			ResultSet res = pstatement.executeQuery();
			while (res.next()) {
				if (this.password.checkPassword(password, res.getBytes("password"))) {
					final String name = res.getString("name");
					connection.close();
					
					return new AuthenticationUser() {
						
						public String getSourceName() {
							return "sqlite:" + dbfile.getPath();
						}
						
						public String getLogin() {
							return username;
						}
						
						public String getFullName() {
							return name;
						}
						
						public String getMail() {
							return null;
						}
					};
				} else {
					connection.close();
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
		Connection connection = createConnection();
		PreparedStatement pstatement = connection.prepareStatement("DELETE FROM users WHERE login = ?");
		pstatement.setString(1, username);
		pstatement.executeUpdate();
		connection.close();
	}
	
	/**
	 * @return -1 if user is unknow
	 */
	public long getCreateDate(String username) throws SQLException {
		Connection connection = createConnection();
		PreparedStatement pstatement = connection.prepareStatement("SELECT created FROM users WHERE login = ?");
		pstatement.setString(1, username);
		ResultSet res = pstatement.executeQuery();
		while (res.next()) {
			long date = res.getDate("created").getTime();
			connection.close();
			return date;
		}
		connection.close();
		return -1;
	}
	
	/**
	 * @return -1 if user is unknow
	 */
	public long getLastUpdateDate(String username) throws SQLException {
		Connection connection = createConnection();
		PreparedStatement pstatement = connection.prepareStatement("SELECT updated FROM users WHERE login = ?");
		pstatement.setString(1, username);
		ResultSet res = pstatement.executeQuery();
		while (res.next()) {
			long date = res.getDate("updated").getTime();
			connection.close();
			return date;
			
		}
		connection.close();
		return -1;
	}
	
	public void disableUser(String username) throws SQLException {
		Connection connection = createConnection();
		PreparedStatement pstatement = connection.prepareStatement("UPDATE users SET updated = ?, enabled = ?  WHERE login = ?");
		pstatement.setDate(1, new Date(System.currentTimeMillis()));
		pstatement.setBoolean(2, false);
		pstatement.setString(3, username);
		pstatement.executeUpdate();
		connection.close();
	}
	
	public void enableUser(String username) throws SQLException {
		Connection connection = createConnection();
		PreparedStatement pstatement = connection.prepareStatement("UPDATE users SET updated = ?, enabled = ?  WHERE login = ?");
		pstatement.setDate(1, new Date(System.currentTimeMillis()));
		pstatement.setBoolean(2, true);
		pstatement.setString(3, username);
		pstatement.executeUpdate();
		connection.close();
	}
	
	public boolean isEnabledUser(String username) throws SQLException {
		Connection connection = createConnection();
		PreparedStatement pstatement = connection.prepareStatement("SELECT enabled FROM users WHERE login = ?");
		pstatement.setString(1, username);
		
		ResultSet res = pstatement.executeQuery();
		while (res.next()) {
			boolean enabled = res.getBoolean("enabled");
			connection.close();
			return enabled;
		}
		connection.close();
		return false;
	}
	
	public boolean isUserExists(String username) throws SQLException {
		Connection connection = createConnection();
		PreparedStatement pstatement = connection.prepareStatement("SELECT COUNT(login) AS user_exists FROM users WHERE login = ?");
		pstatement.setString(1, username);
		
		ResultSet res = pstatement.executeQuery();
		while (res.next()) {
			int ue = res.getInt("user_exists");
			connection.close();
			return ue == 1;
		}
		connection.close();
		return false;
	}
	
	public void changeUserPassword(String username, String password, boolean enabled) throws SQLException {
		Connection connection = createConnection();
		PreparedStatement pstatement = connection.prepareStatement("UPDATE users SET updated = ?, password = ?, enabled = ?  WHERE login = ?");
		pstatement.setDate(1, new Date(System.currentTimeMillis()));
		pstatement.setBytes(2, this.password.getHashedPassword(password));
		pstatement.setBoolean(3, enabled);
		pstatement.setString(4, username);
		pstatement.executeUpdate();
		connection.close();
	}
	
	public void changeUserLongname(String username, String longname) throws SQLException {
		Connection connection = createConnection();
		PreparedStatement pstatement = connection.prepareStatement("UPDATE users SET updated = ?, name = ?  WHERE login = ?");
		pstatement.setDate(1, new Date(System.currentTimeMillis()));
		pstatement.setString(2, longname);
		pstatement.setString(3, username);
		pstatement.executeUpdate();
		connection.close();
	}
	
	/**
	 * @return null if user is unknow
	 */
	public String getUserLongname(String username) throws SQLException {
		Connection connection = createConnection();
		PreparedStatement pstatement = connection.prepareStatement("SELECT name FROM users WHERE login = ?");
		pstatement.setString(1, username);
		ResultSet res = pstatement.executeQuery();
		while (res.next()) {
			String name = res.getString("name");
			connection.close();
			return name;
		}
		connection.close();
		return null;
	}
	
	/**
	 * @return never null
	 */
	public List<String> getUserList(boolean mustenabled) throws SQLException {
		Connection connection = createConnection();
		ResultSet res;
		if (mustenabled) {
			PreparedStatement pstatement = connection.prepareStatement("SELECT login FROM users WHERE enabled = ?");
			pstatement.setBoolean(1, mustenabled);
			res = pstatement.executeQuery();
		} else {
			PreparedStatement pstatement = connection.prepareStatement("SELECT login FROM users");
			res = pstatement.executeQuery();
		}
		
		List<String> userlist = new ArrayList<String>();
		while (res.next()) {
			userlist.add(res.getString("login"));
		}
		connection.close();
		return userlist;
	}
	
	/**
	 * @return null if user is unknow
	 */
	public String getUserInformations(String username) throws SQLException {
		Connection connection = createConnection();
		PreparedStatement pstatement = connection.prepareStatement("SELECT name, created, updated, enabled FROM users WHERE login = ?");
		pstatement.setString(1, username);
		ResultSet res = pstatement.executeQuery();
		while (res.next()) {
			StringBuilder sb = new StringBuilder();
			sb.append("user: ");
			sb.append(username);
			sb.append(", name: ");
			sb.append(res.getString("name"));
			sb.append(", created: ");
			sb.append(Loggers.dateLog(res.getDate("created").getTime()));
			sb.append(", updated: ");
			sb.append(Loggers.dateLog(res.getDate("updated").getTime()));
			sb.append(", enabled: ");
			sb.append(res.getBoolean("enabled"));
			connection.close();
			return sb.toString();
		}
		
		connection.close();
		return null;
	}
	
}
