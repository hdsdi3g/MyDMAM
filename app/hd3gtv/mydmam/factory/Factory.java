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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.factory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.elasticsearch.common.collect.Lists;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

import hd3gtv.mydmam.Loggers;
import hd3gtv.tools.CopyMove;

/**
 * Create Objects and search Class
 */
public class Factory {
	
	private ArrayList<File> classpath;
	private HashMap<String, Class<?>> class_names;
	private HashSet<String> absent_class_names;
	private ConcurrentHashMap<Class<?>, Constructor<?>> class_constructor;
	private final Object lock;
	
	public Factory() {
		ArrayList<String> classpath_string = Lists.newArrayList(System.getProperty("java.class.path").split(System.getProperty("path.separator")));
		
		classpath = new ArrayList<>(classpath_string.size());
		classpath_string.forEach(cp -> {
			File f = new File(cp);
			try {
				CopyMove.checkExistsCanRead(f);
				classpath.add(f.getCanonicalFile());
			} catch (Exception e) {
				Loggers.Manager.error("Can't access to classpath item: " + cp);
			}
		});
		
		class_names = new HashMap<>();
		absent_class_names = new HashSet<>();
		class_constructor = new ConcurrentHashMap<>();
		lock = new Object();
	}
	
	/**
	 * @return CanonicalFile and ExistsCanRead
	 */
	public ArrayList<File> getClasspath() {
		return classpath;
	}
	
	/**
	 * @return CanonicalFile and ExistsCanRead
	 */
	public Stream<File> getClasspathOnlyDirectories() {
		return getClasspath().stream().filter(cp -> {
			return cp.isDirectory();
		});
	}
	
	/**
	 * @return maybe null
	 */
	public Class<?> getClassByName(String class_name) {
		if (class_name == null) {
			return null;
		}
		if (absent_class_names.contains(class_name)) {
			return null;
		}
		if (class_names.containsKey(class_name)) {
			return class_names.get(class_name);
		}
		synchronized (lock) {
			try {
				Class<?> checked = Class.forName(class_name);
				class_names.put(class_name, checked);
				return checked;
			} catch (ClassNotFoundException e) {
				absent_class_names.add(class_name);
				return null;
			}
		}
	}
	
	public boolean isClassExists(String class_name) {
		if (class_name == null) {
			return false;
		}
		if (absent_class_names.contains(class_name)) {
			return false;
		}
		if (class_names.containsKey(class_name)) {
			return true;
		}
		return getClassByName(class_name) != null;
	}
	
	public <T> T create(Class<T> from_class) throws ReflectiveOperationException {
		checkIsAccessibleClass(from_class, true);
		
		Constructor<?> constructor = class_constructor.computeIfAbsent(from_class, cl -> {
			Optional<Constructor<?>> o_result = Arrays.asList(cl.getConstructors()).stream().filter(c -> {
				return c.isAccessible() && c.getParameterCount() == 0 && c.isVarArgs() == false;
			}).findFirst();
			
			try {
				if (o_result.isPresent() == false) {
					throw new ClassNotFoundException(cl.getName());
				}
			} catch (ClassNotFoundException e) {
				Loggers.Manager.error("Class " + cl.getName() + " can't be instancied directly", e);
				return null;
			}
			
			return o_result.get();
		});
		
		if (constructor == null) {
			throw new ReflectiveOperationException("Can't found an available constructor for " + from_class.getName());
		}
		
		@SuppressWarnings("unchecked")
		T result = (T) constructor.newInstance();
		
		return result;
	}
	
	/**
	 * @param onError can be null
	 * @return never null
	 */
	public <T> T create(String class_name, Class<T> type) throws ReflectiveOperationException {
		if (class_name == null) {
			throw new NullPointerException("\"class_name\" can't to be null");
		}
		if (type == null) {
			throw new NullPointerException("\"type\" can't to be null");
		}
		
		Class<?> candidate = getClassByName(class_name);
		if (candidate == null) {
			return null;
		}
		try {
			@SuppressWarnings("unchecked")
			T result = (T) create(candidate);
			return result;
		} catch (LinkageError le) {
			throw new ReflectiveOperationException("Invalid class " + type.getName(), le);
		} catch (ClassCastException cce) {
			throw new ReflectiveOperationException("Try to cast to " + type.getName(), cce);
		}
	}
	
	/**
	 * @throws ClassNotFoundException if null, anonymous, local, member (or static if can_to_be_static).
	 */
	public void checkIsAccessibleClass(Class<?> context, boolean can_to_be_static) throws ClassNotFoundException {
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
	
	public List<Class<?>> getAllClassesFromPackage(String package_name) throws ClassNotFoundException {
		try {
			ClassPath cp = ClassPath.from(ClassLoader.getSystemClassLoader());
			ImmutableSet<ClassInfo> packg = cp.getTopLevelClasses(package_name);
			return packg.stream().map(cl -> {
				try {
					return Class.forName(cl.getName());
				} catch (Exception e) {
					Loggers.Manager.error("Can't load class " + cl.getName(), e);
					return null;
				}
			}).filter(cl -> {
				return cl != null;
			}).collect(Collectors.toList());
		} catch (IOException e) {
			throw new ClassNotFoundException("Can't search in classpath", e);
		}
		// return Lists.newArrayList(Auth.class);
	}
	
}
