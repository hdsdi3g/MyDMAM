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
 * Copyright (C) hdsdi3g for hd3g.tv 25 nov. 2016
 * 
*/
package hd3gtv.mydmam.embddb.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.AsynchronousChannelGroup;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.tools.ActivityScheduledAction;
import hd3gtv.tools.ActivityScheduler;
import hd3gtv.tools.AddressMaster;
import hd3gtv.tools.InteractiveConsoleOrder;
import hd3gtv.tools.InteractiveConsoleOrderProducer;
import hd3gtv.tools.PressureMeasurement;
import hd3gtv.tools.TableList;

public class PoolManager implements InteractiveConsoleOrderProducer {
	
	private static Logger log = Logger.getLogger(PoolManager.class);
	
	private ArrayList<SocketServer> local_servers;
	
	@GsonIgnore
	private AllRequestHandlers all_request_handlers;
	
	@GsonIgnore
	private Protocol protocol;
	
	@GsonIgnore
	private AsynchronousChannelGroup channel_group;
	@GsonIgnore
	private BlockingQueue<Runnable> executor_pool_queue;
	@GsonIgnore
	private ThreadPoolExecutor executor_pool;
	
	@GsonIgnore
	private ShutdownHook shutdown_hook;
	
	@GsonIgnore
	private AddressMaster addr_master;
	
	private final UUID uuid_ref;
	
	@GsonIgnore
	private ActivityScheduler<Node> node_scheduler;
	@GsonIgnore
	private ActivityScheduler<PoolManager> pool_scheduler;
	
	private PressureMeasurement pressure_measurement_sended;
	private PressureMeasurement pressure_measurement_recevied;
	private PressureMeasurement pressure_measurement_netdiscover;
	private List<InetSocketAddress> bootstrap_servers;
	
	/**
	 * Can be null.
	 */
	private NetDiscover net_discover;
	
	/**
	 * synchronizedList
	 */
	private List<Node> nodes;
	
	@GsonIgnore
	private AtomicBoolean autodiscover_can_be_remake = null;
	/**
	 * synchronizedList
	 */
	@GsonIgnore
	private List<Consumer<Node>> onRemoveNodeCallbackList;
	
	public PoolManager(Protocol protocol, int thread_pool_queue_size) throws GeneralSecurityException, IOException {
		this.protocol = protocol;
		if (protocol == null) {
			throw new NullPointerException("\"protocol\" can't to be null");
		}
		if (thread_pool_queue_size < 1) {
			throw new IOException("Can't set thread_pool_queue_size to " + thread_pool_queue_size);
		}
		
		local_servers = new ArrayList<>();
		
		executor_pool_queue = new LinkedBlockingQueue<Runnable>(thread_pool_queue_size);
		executor_pool = new ThreadPoolExecutor(1, Runtime.getRuntime().availableProcessors(), thread_pool_queue_size, TimeUnit.MILLISECONDS, executor_pool_queue);
		executor_pool.setRejectedExecutionHandler((r, executor) -> {
			log.warn("Too many task to be executed at the same time ! This will not proceed: " + r);
		});
		channel_group = AsynchronousChannelGroup.withThreadPool(executor_pool);
		
		pressure_measurement_sended = new PressureMeasurement();
		pressure_measurement_recevied = new PressureMeasurement();
		pressure_measurement_netdiscover = new PressureMeasurement();
		
		nodes = Collections.synchronizedList(new ArrayList<>());
		onRemoveNodeCallbackList = Collections.synchronizedList(new ArrayList<>());
		autodiscover_can_be_remake = new AtomicBoolean(true);
		uuid_ref = UUID.randomUUID();
		addr_master = new AddressMaster();
		shutdown_hook = new ShutdownHook();
		all_request_handlers = new AllRequestHandlers(this);
		
		pool_scheduler = new ActivityScheduler<>();
		pool_scheduler.add(this, getScheduledAction());
		node_scheduler = new ActivityScheduler<>();
	}
	
	public void startNetDiscover(List<InetSocketAddress> multicast_groups) throws IOException {
		if (net_discover == null) {
			net_discover = new NetDiscover(this, multicast_groups, pressure_measurement_netdiscover);
			net_discover.start();
		}
	}
	
	PressureMeasurement getPressureMeasurementSended() {
		return pressure_measurement_sended;
	}
	
	PressureMeasurement getPressureMeasurementRecevied() {
		return pressure_measurement_recevied;
	}
	
	public UUID getUUIDRef() {
		return uuid_ref;
	}
	
	public Protocol getProtocol() {
		return protocol;
	}
	
	public AddressMaster getAddressMaster() {
		return addr_master;
	}
	
	ActivityScheduler<Node> getNode_scheduler() {
		return node_scheduler;
	}
	
	AsynchronousChannelGroup getChannelGroup() {
		return channel_group;
	}
	
	public void setBootstrapPotentialNodes(List<InetSocketAddress> servers) {
		this.bootstrap_servers = servers;
	}
	
	public void connectToBootstrapPotentialNodes(String reason) {
		if (bootstrap_servers == null) {
			return;
		}
		bootstrap_servers.forEach(addr -> {
			try {
				declareNewPotentialDistantServer(addr, new ConnectionCallback() {
					
					public void onNewConnectedNode(Node node) {
						log.info("Connected to node (bootstrap): " + node + " by " + reason);
					}
					
					public void onLocalServerConnection(InetSocketAddress server) {
						log.warn("Can't add server (" + server.getHostString() + "/" + server.getPort() + ") not node list, by " + reason);
					}
					
					public void alreadyConnectedNode(Node node) {
						log.debug("Node is already connected: " + node + ", by " + reason);
					}
				});
			} catch (Exception e) {
				log.error("Can't create node: " + addr + ", by " + reason, e);
			}
		});
	}
	
	public void startLocalServers() throws IOException {
		startLocalServers(addr_master.getAddresses().map(addr -> {
			return new InetSocketAddress(addr, protocol.getDefaultTCPPort());
		}).collect(Collectors.toList()));
	}
	
	/**
	 * @param listen_list can be null or empty
	 */
	public void startLocalServers(List<InetSocketAddress> listen_list) throws IOException {
		if (listen_list == null) {
			startLocalServers();
			return;
		} else if (listen_list.isEmpty()) {
			if (addr_master.getAddresses().count() > 0l) {
				startLocalServers();
				return;
			} else {
				throw new IOException("Can't load servers, no IP addrs for this host (how this can be possible ?)");
			}
		} else if (listen_list.stream().anyMatch(listen -> {
			try {
				return listen.getAddress().isAnyLocalAddress() | listen.getAddress().equals(InetAddress.getByName("0.0.0.0"));
			} catch (UnknownHostException e) {
				log.error("Can't parse Addr", e);
				return false;
			}
		})) {
			startLocalServers();
			return;
		}
		
		ArrayList<String> logresult = new ArrayList<>();
		listen_list.forEach(listen -> {
			try {
				SocketServer local_server = new SocketServer(this, listen);
				local_server.start();
				local_servers.add(local_server);
				logresult.add(listen.getHostString() + "/" + listen.getPort());
			} catch (IOException e) {
				log.error("Can't start server on " + listen.getHostString() + "/" + listen.getPort());
			}
		});
		
		log.info("Start local server on " + logresult);
		
		if (net_discover != null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			net_discover.updatePayload();
		}
		
		// T O D O manage white/black range addr list for node discover
		
		Runtime.getRuntime().addShutdownHook(shutdown_hook);
	}
	
	/**
	 * Blocking
	 */
	public void closeAll() {
		log.info("Close all functions: clients, server, autodiscover... It's a blocking operation");
		
		if (net_discover != null) {
			net_discover.close();
		}
		
		pool_scheduler.remove(this);
		sayToAllNodesToDisconnectMe(true);
		
		local_servers.forEach(s -> {
			s.wantToStop();
		});
		local_servers.forEach(s -> {
			s.waitToStop();
		});
		
		executor_pool.shutdown();
		
		try {
			executor_pool.awaitTermination(500, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			log.error("Can't wait to stop executor waiting list", e);
			executor_pool.shutdownNow();
		}
		
		try {
			Runtime.getRuntime().removeShutdownHook(shutdown_hook);
		} catch (IllegalStateException e) {
		}
	}
	
	void executeInThePool(Runnable r) {
		if (executor_pool == null) {
			return;
		}
		if (executor_pool.isShutdown() | executor_pool.isTerminated() | executor_pool.isTerminated()) {
			return;
		}
		executor_pool.execute(r);
	}
	
	public boolean isListenToThis(InetSocketAddress server) {
		return getListenedServerAddress().anyMatch(addr -> {
			return addr.equals(server);
		});
	}
	
	public Stream<InetSocketAddress> getListenedServerAddress() {
		return local_servers.stream().map(s -> {
			return s.getListen();
		}).filter(addr -> {
			return addr != null;
		});
	}
	
	/**
	 * @param callback_on_connection Always callback it, even if already exists.
	 */
	public void declareNewPotentialDistantServer(InetSocketAddress server, ConnectionCallback callback_on_connection) throws IOException {
		if (isListenToThis(server)) {
			callback_on_connection.onLocalServerConnection(server);
			return;
		}
		
		Node node = get(server);
		
		if (node != null) {
			callback_on_connection.alreadyConnectedNode(node);
		} else {
			new SocketClient(this, server, n -> {
				if (add(n)) {
					callback_on_connection.onNewConnectedNode(n);
				} else {
					callback_on_connection.alreadyConnectedNode(get(server));
				}
			});
		}
	}
	
	public AllRequestHandlers getAllRequestHandlers() {
		return all_request_handlers;
	}
	
	private class ShutdownHook extends Thread {
		public void run() {
			closeAll();
		}
	}
	
	/**
	 * Check if node is already open, else close it.
	 * @return null if empty
	 */
	public Node get(InetSocketAddress addr) {
		if (addr == null) {
			throw new NullPointerException("\"addr\" can't to be null");
		}
		
		Optional<Node> o_node = nodes.stream().filter(n -> {
			return addr.equals(n.getSocketAddr());
		}).findFirst();
		
		if (o_node.isPresent() == false) {
			return null;
		}
		
		Node n = o_node.get();
		
		if (n.isOpenSocket() == false) {
			remove(n);
			return null;
		}
		
		return n;
	}
	
	/**
	 * Check if nodes are already open, else close it.
	 * @return empty if emtpy
	 */
	public List<Node> get(InetAddress addr) {
		if (addr == null) {
			throw new NullPointerException("\"addr\" can't to be null");
		}
		
		List<Node> result = nodes.stream().filter(n -> {
			InetSocketAddress n_addr = n.getSocketAddr();
			if (n_addr == null) {
				return false;
			}
			return n_addr.getAddress().equals(addr);
		}).collect(Collectors.toList());
		
		result.removeIf(n -> {
			if (n.isOpenSocket() == false) {
				remove(n);
				return true;
			}
			return false;
		});
		
		return result;
	}
	
	/**
	 * Check if node is already open, else close it.
	 * @return null if empty
	 */
	public Node get(UUID uuid) {
		if (uuid == null) {
			throw new NullPointerException("\"uuid\" can't to be null");
		}
		
		Optional<Node> o_node = nodes.stream().filter(n -> {
			return n.equalsThisUUID(uuid);
		}).findFirst();
		
		if (o_node.isPresent() == false) {
			return null;
		}
		
		Node n = o_node.get();
		
		if (n.isOpenSocket() == false) {
			remove(n);
			return null;
		}
		
		return n;
	}
	
	public boolean isConnectedTo(UUID uuid) {
		if (uuid == null) {
			throw new NullPointerException("\"uuid\" can't to be null");
		}
		
		return nodes.stream().filter(n -> {
			return n.equalsThisUUID(uuid);
		}).findFirst().isPresent();
	}
	
	/**
	 * Async
	 */
	private void callbackAllListOnRemoveNode(Node node) {
		this.executeInThePool(() -> {
			onRemoveNodeCallbackList.forEach(h -> {
				h.accept(node);
			});
		});
	}
	
	public void purgeClosedNodes() {
		nodes.removeIf(n -> {
			if (n.isOpenSocket()) {
				return false;
			}
			
			callbackAllListOnRemoveNode(n);
			autodiscover_can_be_remake.set(true);
			return true;
		});
		
		if (nodes.isEmpty()) {
			connectToBootstrapPotentialNodes("Opened nodes list empty after purge");
		}
	}
	
	public void remove(Node node) {
		log.info("Remove node " + node);
		
		autodiscover_can_be_remake.set(true);
		callbackAllListOnRemoveNode(node);
		nodes.remove(node);
		
		if (log.isDebugEnabled()) {
			log.debug("Full node list: " + nodes);
		} else {
			if (nodes.isEmpty()) {
				log.info("Now, it's not connected to any nodes");
			}
		}
		
		node_scheduler.remove(node);
		
		if (nodes.isEmpty()) {
			connectToBootstrapPotentialNodes("Opened nodes list empty after purge");
		}
	}
	
	/**
	 * @return false if node is already added
	 */
	public boolean add(Node node) {
		if (nodes.contains(node)) {
			if (node.isOpenSocket()) {
				return false;
			} else {
				remove(node);
			}
		}
		log.info("Add node " + node);
		autodiscover_can_be_remake.set(true);
		nodes.add(node);
		all_request_handlers.getRequestByClass(RequestHello.class).sendRequest(null, node);
		
		if (log.isDebugEnabled()) {
			log.debug("Full node list: " + nodes);
		}
		
		return true;
	}
	
	/**
	 * @return array of objects (Node.getAutodiscoverIDCard())
	 */
	JsonArray makeAutodiscoverList() {
		JsonArray autodiscover_list = new JsonArray();
		nodes.forEach(n -> {
			JsonObject jo = n.getAutodiscoverIDCard();
			if (jo != null) {
				autodiscover_list.add(jo);
			}
		});
		return autodiscover_list;
	}
	
	private ActivityScheduledAction<PoolManager> getScheduledAction() {
		return new ActivityScheduledAction<PoolManager>() {
			
			public String getScheduledActionName() {
				return "Purge closed nodes and send autodiscover requests";
			}
			
			public boolean onScheduledActionError(Exception e) {
				log.error("Can't do reguar scheduled nodelist operations", e);
				return true;
			}
			
			public TimeUnit getScheduledActionPeriodUnit() {
				return TimeUnit.SECONDS;
			}
			
			public long getScheduledActionPeriod() {
				return 60;
			}
			
			public long getScheduledActionInitialDelay() {
				return 10;
			}
			
			public Runnable getRegularScheduledAction() {
				return () -> {
					purgeClosedNodes();
					if (autodiscover_can_be_remake.compareAndSet(true, false)) {
						DataBlock to_send = all_request_handlers.getRequestByClass(RequestNodelist.class).createRequest(null);
						if (to_send != null) {
							nodes.forEach(n -> {
								n.sendBlock(to_send, false);
							});
						}
					}
				};
			}
		};
	}
	
	public void addRemoveNodeCallback(Consumer<Node> h) {
		if (h == null) {
			throw new NullPointerException("\"h\" can't to be null");
		}
		onRemoveNodeCallbackList.add(h);
	}
	
	public void removeRemoveNodeCallback(Consumer<Node> h) {
		if (h == null) {
			throw new NullPointerException("\"h\" can't to be null");
		}
		onRemoveNodeCallbackList.remove(h);
	}
	
	private void sayToAllNodesToDisconnectMe(boolean blocking) {
		DataBlock to_send = all_request_handlers.getRequestByClass(RequestDisconnect.class).createRequest("All nodes instance shutdown");
		nodes.forEach(n -> {
			n.sendBlock(to_send, true);
		});
		
		if (blocking) {
			try {
				while (nodes.isEmpty() == false) {
					Thread.sleep(1);
				}
			} catch (InterruptedException e1) {
			}
		}
	}
	
	/**
	 * @param filter for select some nodes, can be null (all nodes)
	 * @return result / all nodes
	 */
	public <O, T extends RequestHandler<O>> List<Node> sayToAllNodes(Class<T> request_class, O option, Predicate<Node> filter) {
		if (request_class == null) {
			throw new NullPointerException("\"request_class\" can't to be null");
		}
		
		DataBlock to_send = all_request_handlers.getRequestByClass(request_class).createRequest(option);
		
		Predicate<Node> filterActiveNodes = node -> {
			return node.isUUIDSet() && node.isOpenSocket();
		};
		
		List<Node> nodes_to_send = nodes.stream().filter(node -> {
			if (filter != null) {
				return filterActiveNodes.and(filter).test(node);
			}
			return filterActiveNodes.test(node);
		}).collect(Collectors.toList());
		
		if (log.isTraceEnabled()) {
			log.trace("Send a " + request_class.getSimpleName() + " request (" + to_send.getDatas().length + " bytes) to " + nodes_to_send.size() + " node(s): " + nodes_to_send);
		} else if (log.isDebugEnabled()) {
			log.debug("Send a " + request_class.getSimpleName() + " request to " + nodes_to_send.size() + " node(s)");
		}
		
		nodes_to_send.forEach(n -> {
			n.sendBlock(to_send);
		});
		return nodes_to_send;
	}
	
	/**
	 * Register new tricks to EmbDDB
	 */
	public void addRequestHandler(RequestHandler<?> rh) {
		try {
			all_request_handlers.addRequest(rh);
		} catch (IndexOutOfBoundsException e) {
		}
	}
	
	public class AllRequestHandlers {
		private PoolManager referer;
		private HashMap<String, RequestHandler<?>> requestHandler;
		
		private AllRequestHandlers(PoolManager referer) {
			this.referer = referer;
			requestHandler = new HashMap<>();
			
			addRequest(new RequestError(referer));
			addRequest(new RequestHello(referer));
			addRequest(new RequestDisconnect(referer));
			addRequest(new RequestNodelist(referer));
			addRequest(new RequestPoke(referer));
		}
		
		private synchronized void addRequest(RequestHandler<?> r) {
			String name = r.getHandleName();
			if (name == null) {
				throw new NullPointerException("Request getHandleName can't to be null");
			}
			if (name.isEmpty()) {
				throw new NullPointerException("Request getHandleName can't to be empty");
			}
			if (requestHandler.containsKey(name)) {
				throw new IndexOutOfBoundsException("Another Request was loaded with name " + name + " (" + r.getClass() + " and " + requestHandler.get(name).getClass() + ")");
			}
			requestHandler.put(name, r);
		}
		
		@SuppressWarnings("unchecked")
		public <T extends RequestHandler<?>> T getRequestByClass(Class<T> request_class) {
			Optional<RequestHandler<?>> o_r = requestHandler.values().stream().filter(r -> {
				return r.getClass().equals(request_class);
			}).findFirst();
			
			if (o_r.isPresent()) {
				return (T) o_r.get();
			}
			
			try {
				log.info("Can't found class " + request_class + " in current class list");
				RequestHandler<?> r = request_class.getConstructor(PoolManager.class).newInstance(referer);
				addRequest(r);
				return (T) r;
			} catch (Exception e) {
				log.error("Can't instance class " + request_class.getName(), e);
			}
			
			return null;
		}
		
		public void onReceviedNewBlock(DataBlock block, Node node) throws WantToCloseLinkException {
			if (log.isTraceEnabled()) {
				log.trace("Get " + block.toString() + " from " + node);
			}
			
			if (requestHandler.containsKey(block.getRequestName()) == false) {
				if (log.isTraceEnabled()) {
					log.trace("Can't handle block name \"" + block.getRequestName() + "\" from " + node);
				}
				return;
			}
			
			requestHandler.get(block.getRequestName()).onRequest(block, node);
		}
		
	}
	
	public void setConsoleActions(InteractiveConsoleOrder console) {
		console.addOrder("ql", "Queue list", "Display actual queue list", getClass(), (param, out) -> {
			out.println("Executor status:");
			TableList table = new TableList();
			table.addRow("Active", String.valueOf(executor_pool.getActiveCount()));
			table.addRow("Max capacity", String.valueOf(executor_pool_queue.remainingCapacity()));
			table.addRow("Completed", String.valueOf(executor_pool.getCompletedTaskCount()));
			table.addRow("Core pool", String.valueOf(executor_pool.getCorePoolSize()));
			table.addRow("Pool", String.valueOf(executor_pool.getPoolSize()));
			table.addRow("Largest pool", String.valueOf(executor_pool.getLargestPoolSize()));
			table.addRow("Maximum pool", String.valueOf(executor_pool.getMaximumPoolSize()));
			table.print(out);
			out.println();
			
			if (executor_pool_queue.isEmpty()) {
				out.println("No waiting task to display in queue.");
			} else {
				out.println("Display " + executor_pool_queue.size() + " waiting tasks.");
				executor_pool_queue.stream().forEach(r -> {
					out.println(" * " + r.toString() + " in " + r.getClass().getName());
				});
			}
		});
		
		console.addOrder("nl", "Node list", "Display actual connected node", getClass(), (param, out) -> {
			TableList table = new TableList();
			nodes.stream().forEach(node -> {
				node.addToActualStatus(table);
			});
			table.print(out);
		});
		
		console.addOrder("node", "Node action", "Do action to a node", getClass(), (param, out) -> {
			if (param == null) {
				out.println("Usage:");
				out.println("node add address [port]");
				out.println("   for add a new node (after a valid connection)");
				out.println("node rm address [port]");
				out.println("   remove a node with protocol (to a disconnect request)");
				out.println("node close address [port]");
				out.println("   for disconnect directly a node");
				out.println("node isopen address [port]");
				out.println("   for force to check the socket state (open or close)");
				return;
			}
			
			InetSocketAddress addr = parseAddressFromCmdConsole(param);
			
			if (addr == null) {
				out.println("Can't get address from ”" + param + "”");
				return;
			}
			
			if (param.startsWith("add")) {
				try {
					declareNewPotentialDistantServer(addr, new ConnectionCallback() {
						
						public void onNewConnectedNode(Node node) {
							out.println("Node " + node + " is added sucessfully");
						}
						
						public void onLocalServerConnection(InetSocketAddress server) {
							out.println("You can't add local server to new node: " + server);
						}
						
						public void alreadyConnectedNode(Node node) {
							out.println("You can't add this node " + node + " because it's already added");
						}
					});
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			} else {
				Node node = get(addr);
				if (node == null) {
					List<Node> search_nodes = get(addr.getAddress());
					if (search_nodes.isEmpty()) {
						out.println("Can't found node " + addr + " in current list. Please check with nl command");
					} else if (search_nodes.size() > 1) {
						out.println("Too many nodes on the " + addr + " in current list. Please check with nl command and enter TCP port");
					} else {
						node = search_nodes.get(0);
					}
				}
				
				if (node != null) {
					if (param.startsWith("rm")) {
						node.sendRequest(RequestDisconnect.class, "Manual via console");
					} else if (param.startsWith("close")) {
						node.close(getClass());
						remove(node);
					} else if (param.startsWith("isopen")) {
						out.println("Is now open: " + node.isOpenSocket());
					} else {
						out.println("Order ”" + param + "” is unknow");
					}
				} else {
					out.println("Can't found node " + addr + " in current list. Please check with nl command");
				}
			}
		});
		
		console.addOrder("gcnodes", "Garbage collector node list", "Purge closed nodes", getClass(), (param, out) -> {
			purgeClosedNodes();
		});
		console.addOrder("closenodes", "Close all nodes", "Force to disconnect all connected nodes", getClass(), (param, out) -> {
			sayToAllNodesToDisconnectMe(false);
		});
		
		console.addOrder("sch", "Activity scheduler", "Display the activated regular task list", getClass(), (param, out) -> {
			TableList table = new TableList();
			
			if (node_scheduler.isEmpty()) {
				out.println("No regular tasks to display for nodes.");
			} else {
				node_scheduler.getAllScheduledTasks(table);
			}
			
			if (pool_scheduler.isEmpty()) {
				if (node_scheduler.isEmpty()) {
					out.println();
				}
				out.println("No regular tasks to display for nodelist.");
			} else {
				pool_scheduler.getAllScheduledTasks(table);
			}
			
			table.print(out);
		});
		
		console.addOrder("srv", "Servers status", "Display all servers status", getClass(), (param, out) -> {
			TableList table = new TableList();
			local_servers.forEach(local_server -> {
				try {
					if (local_server.isOpen()) {
						table.addRow("open", local_server.getListen().getHostString(), String.valueOf(local_server.getListen().getPort()));
					} else {
						table.addRow("CLOSED", local_server.getListen().getHostString(), String.valueOf(local_server.getListen().getPort()));
					}
				} catch (NullPointerException e) {
				}
			});
			table.print(out);
		});
		
		console.addOrder("closesrv", "Close servers", "Close all opened servers", getClass(), (param, out) -> {
			local_servers.forEach(local_server -> {
				if (local_server.isOpen()) {
					out.println("Close server " + local_server.getListen().getHostString() + "...");
					local_server.waitToStop();
				}
			});
		});
		
		console.addOrder("poke", "Poke servers", "Poke all server, or one if specified", getClass(), (param, out) -> {
			if (param == null) {
				nodes.stream().forEach(node -> {
					out.println("Poke " + node);
					node.sendRequest(RequestPoke.class, null);
				});
			} else {
				InetSocketAddress addr = parseAddressFromCmdConsole(param);
				if (addr != null) {
					Node node = get(addr);
					if (node == null) {
						get(addr.getAddress()).forEach(n -> {
							out.println("Poke " + n);
							n.sendRequest(RequestPoke.class, null);
						});
					} else {
						out.println("Poke " + node);
						node.sendRequest(RequestPoke.class, null);
					}
				}
			}
		});
		
		console.addOrder("ip", "IP properties", "Show actual network properties", getClass(), (param, out) -> {
			TableList table = new TableList();
			addr_master.dump(table);
			table.print(out);
		});
		
		console.addOrder("stats", "Get last pressure measurement", "Get nodelist data stats", getClass(), (param, out) -> {
			TableList list = new TableList();
			PressureMeasurement.toTableHeader(list);
			pressure_measurement_recevied.getActualStats(false).toTable(list, "Recevied");
			pressure_measurement_sended.getActualStats(false).toTable(list, "Sended");
			pressure_measurement_netdiscover.getActualStats(false).toTable(list, "Netdiscover");
			list.print(out);
		});
		
		console.addOrder("resetstats", "Reset pressure measurement", "Get nodelist data stats and reset it", getClass(), (param, out) -> {
			TableList list = new TableList();
			PressureMeasurement.toTableHeader(list);
			pressure_measurement_recevied.getActualStats(true).toTable(list, "Recevied");
			pressure_measurement_sended.getActualStats(true).toTable(list, "Sended");
			pressure_measurement_netdiscover.getActualStats(true).toTable(list, "Netdiscover");
			list.print(out);
		});
	}
	
	/**
	 * @param param like "action addr" or "action addr port" or "action addr/port" or "action addr:port"
	 */
	private InetSocketAddress parseAddressFromCmdConsole(String param) {
		int first_space = param.indexOf(" ");
		if (first_space < 1) {
			return null;
		}
		String full_addr = param.substring(first_space).trim();
		int port = protocol.getDefaultTCPPort();
		
		int port_pos = full_addr.indexOf(" ");
		if (port_pos == -1) {
			port_pos = full_addr.indexOf("/");
		}
		if (port_pos == -1) {
			port_pos = full_addr.lastIndexOf(":");
		}
		if (port_pos > 1) {
			try {
				port = Integer.parseInt(full_addr.substring(port_pos + 1));
				full_addr = full_addr.substring(0, port_pos);
			} catch (NumberFormatException e) {
				log.debug("Can't get port value: " + param);
			}
		}
		return new InetSocketAddress(full_addr, port);
	}
}
