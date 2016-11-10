/*
 * This file is part of MyDMAM
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.datatype.XMLGregorianCalendar;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;

import hd3gtv.configuration.Configuration;

public class MyDMAM {
	
	public static final String LINESEPARATOR = System.getProperty("line.separator");
	
	/**
	 * Transform accents to non accented (ascii) version.
	 */
	public static final Pattern PATTERN_Combining_Diacritical_Marks = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	
	/**
	 * Transform accents to non accented (ascii) version, and remove all spaces chars.
	 */
	public final static Pattern PATTERN_Combining_Diacritical_Marks_Spaced = Pattern.compile("[\\p{InCombiningDiacriticalMarks}\\s]+");
	
	/**
	 * Remove all non char/number like #@-"ı\r\n\t\t,\\;.?&'(§°*$%+=... BUT keep "_"
	 */
	public final static Pattern PATTERN_Special_Chars = Pattern.compile("[^\\w]");
	
	public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static String APP_COPYRIGHT = "Copyright (C) hdsdi3g for hd3g.tv 2012-2016";
	
	/**
	 * @param filename without path
	 * @return S0000000 or 00000000 or null
	 */
	public static String getIdFromFilename(String filename) {
		if (filename == null) {
			return null;
		}
		if (filename.length() < 8) {
			return null;
		}
		char[] chars = filename.toCharArray();
		char curchar;
		for (int pos = 0; pos < 8; pos++) {
			curchar = chars[pos];
			if ((curchar > 47) && (curchar < 58)) {
				/**
				 * from 0 to 9
				 */
				continue;
			}
			if (((curchar == 83) || (curchar == 115)) && (pos == 0)) {
				/**
				 * Start by "S" or "s"
				 */
				continue;
			}
			return null;
		}
		
		return filename.substring(0, 8);
	}
	
	/**
	 * @return true if S0000000 or 00000000
	 */
	public static boolean isValidMediaId(String value) {
		if (value.length() != 8) {
			return false;
		}
		char[] chars = value.toCharArray();
		char curchar;
		for (int pos = 0; pos < 8; pos++) {
			curchar = chars[pos];
			if ((curchar > 47) && (curchar < 58)) {
				/**
				 * from 0 to 9
				 */
				continue;
			}
			if (((curchar == 83) || (curchar == 115)) && (pos == 0)) {
				/**
				 * Start by "S" or "s"
				 */
				continue;
			}
			return false;
		}
		return true;
	}
	
	public static final String byteToString(byte[] b) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < b.length; i++) {
			int v = b[i] & 0xFF;
			if (v < 16) {
				sb.append(0);
			}
			sb.append(Integer.toString(v, 16).toLowerCase());
		}
		return sb.toString();
	}
	
	private volatile static Properties configured_messages;
	
	/**
	 * @return Properties key/values by yaml configuration via "message" entry.
	 *         Restart to see changes.
	 */
	public static Properties getconfiguredMessages() {
		if (configured_messages == null) {
			configured_messages = new Properties();
			if (Configuration.global.isElementExists("messages")) {
				LinkedHashMap<String, String> conf = Configuration.global.getValues("messages");
				for (Map.Entry<String, String> entry : conf.entrySet()) {
					configured_messages.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return configured_messages;
	}
	
	static {
		Security.addProvider(new BouncyCastleProvider());
	}
	
	public static void testIllegalKeySize() {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256", "BC");
			byte[] key = md.digest("".getBytes());
			SecretKey skeySpec = new SecretKeySpec(key, "AES");
			IvParameterSpec salt = new IvParameterSpec(key, 0, 16);
			
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, salt);
			return;
		} catch (NoSuchAlgorithmException e) {
			Loggers.Manager.fatal("Can't found MessageDigest or Cipher Algorithm", e);
		} catch (NoSuchProviderException e) {
			Loggers.Manager.fatal("Can't found MessageDigest or Cipher Provider", e);
		} catch (InvalidKeyException e) {
			if (e.getMessage().equals("Illegal key size")) {
				System.err.println("");
				System.err.println("");
				System.err.println("--------~~~~~~===============~~~~~~--------");
				System.err.println("               Fatal error !");
				System.err.println("You must to setup Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files");
				System.err.println("");
				System.err.println("Go to http://www.oracle.com/technetwork/java/javase/downloads/index.html");
				System.err.println("And download it (it's free and legal)");
				System.err.println("");
				System.err.println("Unzip, and copy US_export_policy.jar and local_policy.jar to this directory:");
				System.err.println("");
				System.err.println(" " + System.getProperty("java.home") + File.separator + "lib" + File.separator + "security");
				System.err.println("");
				System.err.println("Overwrite the actual jar files");
				System.err.println("--------~~~~~~==============~~~~~~--------");
				System.err.println("");
				Loggers.Manager.fatal(
						"JCE no found ! Download JCE from http://www.oracle.com/technetwork/java/javase/downloads/index.html, and unzip in " + System.getProperty("java.home") + "/lib/security/");
			} else {
				Loggers.Manager.fatal("Invalid Cipher key", e);
			}
		} catch (InvalidAlgorithmParameterException e) {
			Loggers.Manager.fatal("Invalid Cipher Parameter", e);
		} catch (NoSuchPaddingException e) {
			Loggers.Manager.fatal("Invalid Cipher Padding", e);
		}
		Loggers.Manager.fatal("Check your Java environment, and the JCE configuration. MyDMAM can't work without it.");
		System.exit(1);
	}
	
	/**
	 * @throws ClassNotFoundException if null, anonymous, local, member (or static if can_to_be_static).
	 */
	public static void checkIsAccessibleClass(Class<?> context, boolean can_to_be_static) throws ClassNotFoundException {
		if (context == null) {
			throw new ClassNotFoundException("\"context\" can't to be null");
		}
		if (context.getClass().isAnonymousClass()) {
			throw new ClassNotFoundException("\"context\" can't to be an anonymous class");
		}
		if (context.getClass().isLocalClass()) {
			throw new ClassNotFoundException("\"context\" can't to be a local class");
		}
		if (context.getClass().isMemberClass()) {
			throw new ClassNotFoundException("\"context\" can't to be a member class");
		}
		if (can_to_be_static == false) {
			if (Modifier.isStatic(context.getClass().getModifiers())) {
				throw new ClassNotFoundException("\"context\" can't to be a static class");
			}
		}
		
	}
	
	public static class GsonClassSerializer implements JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {
		
		public JsonElement serialize(Class<?> src, Type typeOfSrc, JsonSerializationContext context) {
			if (src == null) {
				return null;
			}
			return new JsonPrimitive(src.getName());
		}
		
		public Class<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			try {
				return Class.forName(json.getAsString());
			} catch (Exception e) {
				return null;
			}
		}
	}
	
	/**
	 * Direct (de)serializer.
	 */
	public static class GsonJsonArraySerializer implements JsonSerializer<JsonArray>, JsonDeserializer<JsonArray> {
		
		public JsonElement serialize(JsonArray src, Type typeOfSrc, JsonSerializationContext context) {
			if (src == null) {
				return null;
			}
			return src;
		}
		
		public JsonArray deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			try {
				return json.getAsJsonArray();
			} catch (Exception e) {
				Loggers.Manager.error("Can't deserialize JsonArray", e);
				return null;
			}
		}
	}
	
	/**
	 * Direct (de)serializer.
	 */
	public static class GsonJsonObjectSerializer implements JsonSerializer<JsonObject>, JsonDeserializer<JsonObject> {
		
		public JsonElement serialize(JsonObject src, Type typeOfSrc, JsonSerializationContext context) {
			if (src == null) {
				return null;
			}
			return src;
		}
		
		public JsonObject deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			try {
				return json.getAsJsonObject();
			} catch (Exception e) {
				Loggers.Manager.error("Can't deserialize JsonObject", e);
				return null;
			}
		}
	}
	
	public static class XMLGregorianCalendarSerializer implements JsonSerializer<XMLGregorianCalendar>, JsonDeserializer<XMLGregorianCalendar> {
		
		public XMLGregorianCalendar deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTimeInMillis(json.getAsBigInteger().longValue());
			return new XMLGregorianCalendarImpl(gc);
		}
		
		public JsonElement serialize(XMLGregorianCalendar src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(src.toGregorianCalendar().getTimeInMillis());
		}
	}
	
	public static class InetAddrSerializer implements JsonSerializer<InetAddress>, JsonDeserializer<InetAddress> {
		
		public InetAddress deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			try {
				return InetAddress.getByName(json.getAsString());
			} catch (UnknownHostException e) {
				throw new JsonParseException(json.getAsString(), e);
			}
		}
		
		public JsonElement serialize(InetAddress src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(src.getHostAddress());
		}
	}
	
	public static class URLSerializer implements JsonSerializer<URL>, JsonDeserializer<URL> {
		
		public URL deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			try {
				return new URL(json.getAsString());
			} catch (MalformedURLException e) {
				throw new JsonParseException(json.getAsString(), e);
			}
		}
		
		public JsonElement serialize(URL src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(src.toString());
		}
	}
	
	/**
	 * Register JsonArray, JsonObject, XMLGregorianCalendar, Class, InetAddress, URL.
	 */
	public static void registerBaseSerializers(GsonBuilder gson_builder) {
		gson_builder.registerTypeAdapter(JsonArray.class, new MyDMAM.GsonJsonArraySerializer());
		gson_builder.registerTypeAdapter(JsonObject.class, new MyDMAM.GsonJsonObjectSerializer());
		gson_builder.registerTypeAdapter(XMLGregorianCalendar.class, new MyDMAM.XMLGregorianCalendarSerializer());
		gson_builder.registerTypeAdapter(Class.class, new MyDMAM.GsonClassSerializer());
		gson_builder.registerTypeAdapter(InetAddress.class, new MyDMAM.InetAddrSerializer());
		gson_builder.registerTypeAdapter(URL.class, new MyDMAM.URLSerializer());
	}
	
	/**
	 * Search application.conf in classpath, and return the /mydmam main directory.
	 */
	public static final File APP_ROOT_PLAY_DIRECTORY;
	
	static {
		APP_ROOT_PLAY_DIRECTORY = getMyDMAMRootPlayDirectory();
	}
	
	private static File getMyDMAMRootPlayDirectory() {
		String[] classpathelements = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
		/**
		 * Search application.conf
		 */
		for (int i = 0; i < classpathelements.length; i++) {
			if (classpathelements[i].endsWith(".jar")) {
				continue;
			}
			File applicationconf_file = new File(classpathelements[i] + File.separator + "application.conf");
			if (applicationconf_file.exists()) {
				return (new File(classpathelements[i]).getParentFile());
			}
		}
		Loggers.Manager.error("Can't found MyDMAM Play application", new FileNotFoundException(new File("").getAbsolutePath()));
		return new File("");
	}
	
	/**
	 * Compares two version strings.
	 * Use this instead of String.compareTo() for a non-lexicographical
	 * comparison that works for version strings. e.g. "1.10".compareTo("1.6").
	 * It remove the "v" char in front.
	 * @note It does not work if "1.10" is supposed to be equal to "1.10.0".
	 * @param str1 a string of ordinal numbers separated by decimal points.
	 * @param str2 a string of ordinal numbers separated by decimal points.
	 * @return The result is a negative integer if str1 is _numerically_ less than str2.
	 *         The result is a positive integer if str1 is _numerically_ greater than str2.
	 *         The result is zero if the strings are _numerically_ equal.
	 * @see from http://stackoverflow.com/questions/6701948/efficient-way-to-compare-version-strings-in-java
	 */
	public static int versionCompare(String version1, String version2) {
		String str1 = version1;
		if (str1.startsWith("v")) {
			str1 = str1.substring(1);
		}
		str1 = str1.trim();
		
		String str2 = version2;
		if (str2.startsWith("v")) {
			str2 = str2.substring(1);
		}
		str2 = str2.trim();
		
		String[] vals1 = str1.split("\\.");
		String[] vals2 = str2.split("\\.");
		int i = 0;
		/**
		 * Set index to first non-equal ordinal or length of shortest version string
		 */
		while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
			i++;
		}
		/**
		 * Compare first non-equal ordinal number
		 */
		if (i < vals1.length && i < vals2.length) {
			int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
			return Integer.signum(diff);
		}
		/**
		 * The strings are equal or one string is a substring of the other
		 * e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
		 */
		return Integer.signum(vals1.length - vals2.length);
	}
}
