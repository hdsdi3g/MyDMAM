/*
 * This file is part of YAML Configuration for MyDMAM
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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.configuration;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigurationClusterItem {
	
	public String address;
	public int port;
	
	ConfigurationClusterItem(String address, int port) {
		this.address = address;
		this.port = port;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(address);
		sb.append(":");
		sb.append(port);
		return sb.toString();
	}
	
	public InetSocketAddress getSocketAddress() {
		return new InetSocketAddress(address, port);
	}
	
	public static final Pattern COMA_SPLITER = Pattern.compile(",");
	public static final Pattern COLON_SPLITER = Pattern.compile(":");
	
	public final static IntPredicate COLON_SELECTOR = chr -> {
		return (char) chr == ':';
	};
	public final static IntPredicate SQUARE_BRACKET_ON_SELECTOR = chr -> {
		return (char) chr == '[';
	};
	public final static IntPredicate SQUARE_BRACKET_OFF_SELECTOR = chr -> {
		return (char) chr == ']';
	};
	
	/**
	 * IPv4/IPv6/Hostname parser with facultative ports
	 * @param raw_value like "1.2.3.4" or "1.2.3.4:1234" or "hostname" or "hostname:1234" or "12::34" or "1:2:3:4::5" or "[::1]:1234", coma separated.
	 */
	public static Stream<ConfigurationClusterItem> parse(String raw_value, int default_port) {
		return COMA_SPLITER.splitAsStream(raw_value).map(entry -> {
			return entry.trim();
		}).map(entry -> {
			int count_colon = (int) entry.chars().filter(COLON_SELECTOR).limit(2l).count();
			if (count_colon == 0) {
				/**
				 * IPv4/hostname only
				 */
				return new ConfigurationClusterItem(entry, default_port);
			} else if (count_colon == 1) {
				/**
				 * IPv4/hostname + port
				 */
				List<String> sub_entry = COLON_SPLITER.splitAsStream(entry).collect(Collectors.toList());
				return new ConfigurationClusterItem(sub_entry.get(0), Integer.parseInt(sub_entry.get(1)));
			} else {
				/**
				 * IPv6 or shit
				 */
				int sqbra_on = (int) entry.chars().filter(SQUARE_BRACKET_ON_SELECTOR).count();
				int sqbra_off = (int) entry.chars().filter(SQUARE_BRACKET_OFF_SELECTOR).count();
				if (sqbra_on > 1 | sqbra_off > 1 | (sqbra_on == 0 ^ sqbra_off == 0)) {
					throw new RuntimeException("Invalid IPv6 format: \"" + entry + "\"");
				} else if (sqbra_on == 1 & sqbra_off == 1) {
					/**
					 * IPv6 + port
					 */
					int sqbra_pos_on = entry.indexOf('[');
					int sqbra_pos_off = entry.indexOf(']');
					int last_colon_pos = entry.lastIndexOf(':');
					
					if (sqbra_pos_off + 1 != last_colon_pos) {
						throw new RuntimeException("Invalid IPv6 format: \"" + entry + "\" (problem with last colon, format is like \"[::1]:1234\" or \"::1\" )");
					}
					return new ConfigurationClusterItem(entry.substring(sqbra_pos_on + 1, sqbra_pos_off), Integer.parseInt(entry.substring(last_colon_pos + 1)));
				} else {
					/**
					 * IPv6 only
					 */
					return new ConfigurationClusterItem(entry, default_port);
				}
			}
		});
	}
	
}
