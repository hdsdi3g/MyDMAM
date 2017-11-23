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
 * Copyright (C) hdsdi3g for hd3g.tv 8 janv. 2017
 * 
*/
package hd3gtv.mydmam.embddb.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import hd3gtv.tools.AddressMaster;

public class RequestNodelist extends RequestHandler<Void> {
	
	private static Logger log = Logger.getLogger(RequestNodelist.class);
	
	private final AddressMaster address_master;
	
	public RequestNodelist(PoolManager pool_manager) {
		super(pool_manager);
		address_master = pool_manager.getAddressMaster();
	}
	
	public HandleName getHandleName() {
		return new HandleName("nodelist");
	}
	
	public void onRequest(DataBlock block, Node source_node) {
		try {
			Predicate<InetAddress> non_local_address_filter = addr -> {
				return AddressMaster.isLocalAddress(addr) == false;
			};
			List<InetAddress> current_host_routed_addr_list = address_master.getAddresses().filter(non_local_address_filter).collect(Collectors.toList());
			
			JsonArray list = block.getJsonDatas().getAsJsonArray();
			if (list.size() == 0) {
				return;
			}
			ArrayList<JsonObject> jo_list = new ArrayList<>(list.size());
			
			list.forEach(je -> {
				jo_list.add(je.getAsJsonObject());
			});
			
			UUID this_uuid = pool_manager.getUUIDRef();
			
			jo_list.stream().filter(jo -> {
				/**
				 * Remove all actual UUID from list
				 */
				try {
					UUID uuid = Node.getUUIDFromAutodiscoverIDCard(jo);
					return uuid.equals(this_uuid) == false && pool_manager.get(uuid) == null;
				} catch (Exception e) {
					log.warn("Invalid UUID format in json " + list.toString() + " from " + source_node, e);
					return false;
				}
			}).map(jo -> {
				/**
				 * Get all addr from list
				 */
				try {
					return Node.getAddressFromAutodiscoverIDCard(pool_manager, jo);
				} catch (Exception e) {
					log.warn("Invalid Addr format in json " + list.toString() + " from " + source_node, e);
					return null;
				}
			}).filter(server_node_addr_list -> {
				/**
				 * Remove null addr (by security)
				 */
				if (server_node_addr_list == null) {
					return false;
				}
				server_node_addr_list.removeIf(addr -> {
					return addr == null;
				});
				
				return true;
			}).forEach(server_node_addr_list -> {
				/**
				 * Prepare item list for discriminate/sort potential nodes by:
				 * - this localhost connected
				 * - distant address connected to a common host connected network.
				 * - the rest of addresses
				 */
				
				List<DistantSocketEntry> distant_socket_entries = server_node_addr_list.stream().map(socket_addr -> {
					return new DistantSocketEntry(socket_addr, current_host_routed_addr_list, source_node);
				}).collect(Collectors.toList());
				
				if (log.isTraceEnabled()) {
					log.trace("Get distant_socket_entries list from " + source_node + ": " + distant_socket_entries);
				}
				
				if (distant_socket_entries.stream().anyMatch(dist_addr -> {
					return dist_addr.is_this_current_host;
				})) {
					/**
					 * There are some routed IPs who equals this host.
					 * Communicate via localhost is possible.
					 */
					distant_socket_entries.stream().sorted((l, r) -> {
						/**
						 * Sort the localhost addresses in first
						 */
						if (l.is_local_addr == false && r.is_local_addr) {
							return -1;
						} else if (l.is_local_addr && r.is_local_addr == false) {
							return 1;
						}
						return 0;
					}).forEach(dist_addr -> {
						dist_addr.connectTo();
					});
				} else {
					distant_socket_entries.stream().filter(dist_addr -> {
						/**
						 * Remove localhost IPs
						 */
						return dist_addr.is_local_addr == false;
					}).sorted((l, r) -> {
						/**
						 * Sort the connected to routed network addresses in first
						 */
						if (l.is_connected_to_routed_network == false && r.is_connected_to_routed_network) {
							return -1;
						} else if (l.is_connected_to_routed_network && r.is_connected_to_routed_network == false) {
							return 1;
						}
						return 0;
					}).forEach(dist_addr -> {
						dist_addr.connectTo();
					});
				}
			});
		} catch (Exception e) {
			log.warn("Error during autodiscover nodelist (from " + source_node + ")", e);
		}
	}
	
	private class DistantSocketEntry {
		InetSocketAddress socket_addr;
		boolean is_local_addr;
		boolean is_this_current_host;
		Node source_node;
		boolean is_connected_to_routed_network;
		
		DistantSocketEntry(InetSocketAddress socket_addr, List<InetAddress> current_host_routed_addr_list, Node source_node) {
			this.socket_addr = socket_addr;
			if (socket_addr == null) {
				throw new NullPointerException("\"socket_addr\" can't to be null");
			}
			this.source_node = source_node;
			if (source_node == null) {
				throw new NullPointerException("\"source_node\" can't to be null");
			}
			
			is_local_addr = AddressMaster.isLocalAddress(socket_addr.getAddress());
			
			is_this_current_host = current_host_routed_addr_list.stream().anyMatch(this_addr -> {
				return this_addr.equals(socket_addr.getAddress());
			});
			
			if (is_local_addr == false && is_this_current_host == false) {
				is_connected_to_routed_network = address_master.isInNetworkRange(socket_addr.getAddress());
			} else {
				is_connected_to_routed_network = false;
			}
		}
		
		public String toString() {
			LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
			log.put("socket_addr", socket_addr);
			log.put("is_local_addr", is_local_addr);
			log.put("is_this_current_host", is_this_current_host);
			log.put("is_connected_to_routed_network", is_connected_to_routed_network);
			return log.toString();
		}
		
		private void connectTo() {
			if (pool_manager.isListenToThis(socket_addr)) {
				log.debug("Selected this node via " + socket_addr + ", don't declare new node");
				return;
			}
			
			try {
				pool_manager.declareNewPotentialDistantServer(socket_addr, new ConnectionCallback() {
					
					public void onNewConnectedNode(Node node) {
						log.info("Autodiscover allowed to connect to " + node + " (provided by " + source_node + ")");
					}
					
					public void onLocalServerConnection(InetSocketAddress server) {
						log.warn("Autodiscover cant add this server (" + server + ")  as node (provided by " + source_node + ")");
					}
					
					public void alreadyConnectedNode(Node node) {
						log.info("Autodiscover cant add an already connected node (" + node + " provided by " + source_node + ")");
					}
				});
			} catch (Exception e) {
				log.error("Autodiscover operation can't to connect to node via " + socket_addr);
			}
		}
	}
	
	public DataBlock createRequest(Void opt) {
		return new DataBlock(this, pool_manager.makeAutodiscoverList());
	}
	
}
