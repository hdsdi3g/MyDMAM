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
package hd3gtv.mydmam.embddb;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationClusterItem;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.cli.CLIDefinition;
import hd3gtv.mydmam.embddb.network.PoolManager;
import hd3gtv.mydmam.embddb.network.Protocol;
import hd3gtv.mydmam.embddb.store.FileBackend;
import hd3gtv.mydmam.embddb.store.ItemFactory;
import hd3gtv.mydmam.embddb.store.ReadCache;
import hd3gtv.mydmam.embddb.store.ReadCacheEhcache;
import hd3gtv.tools.ApplicationArgs;
import hd3gtv.tools.CopyMove;
import hd3gtv.tools.InteractiveConsole;
import hd3gtv.tools.InteractiveConsoleOrder;

/**
 * Embedded and distributed and database
 */
public class EmbDDB implements InteractiveConsoleOrder {
	
	private static Logger log = Logger.getLogger(EmbDDB.class);
	
	private static String getMasterPasswordKey() throws GeneralSecurityException {
		String master_password_key = Configuration.global.getValue("embddb", "master_password_key", "");
		if (master_password_key.equalsIgnoreCase("SetMePlease")) {
			throw new GeneralSecurityException("You can't use \"SetMePlease\" as password for EmbDDB");
		}
		if (master_password_key.length() < 5) {
			log.warn("You should not use a so small password for EmbDDB (" + master_password_key.length() + " chars)");
		}
		return master_password_key;
	}
	
	/**
	 * @return can be null
	 */
	public static EmbDDB createFromConfiguration() throws GeneralSecurityException, IOException, InterruptedException {
		if (Configuration.global.isElementKeyExists("embddb", "master_password_key") == false) {
			return null;
		}
		
		EmbDDB result = new EmbDDB(getMasterPasswordKey());
		List<InetSocketAddress> bootstrap_addrs = Configuration.global.getClusterConfiguration("embddb", "bootstrap_nodes", null, result.protocol.getDefaultTCPPort()).stream().map(item -> {
			return item.getSocketAddress();
		}).collect(Collectors.toList());
		
		if (bootstrap_addrs.isEmpty() == false) {
			result.poolmanager.setBootstrapPotentialNodes(bootstrap_addrs);
			result.poolmanager.connectToBootstrapPotentialNodes("Loaded from configuration");
		}
		
		if (Configuration.global.getValueBoolean("embddb", "disable_multicast_discover") == false) {
			List<InetSocketAddress> multicast_groups = Configuration.global.getClusterConfiguration("embddb", "multicast_groups", null, result.protocol.getDefaultUDPMulticastPort()).stream().map(item -> {
				return item.getSocketAddress();
			}).collect(Collectors.toList());
			result.poolmanager.startNetDiscover(multicast_groups);
		}
		
		if (Configuration.global.isElementKeyExists("embddb", "durable_store_directory")) {
			result.setDurableStoreDirectory(new File(Configuration.global.getValue("embddb", "durable_store_directory", null)), false);
		}
		
		return result;
	}
	
	private final Protocol protocol;
	public final PoolManager poolmanager;
	private final LockEngine lock_engine;
	private final Telemetry telemetry;
	private final InteractiveConsole console;
	private File durable_store_directory;
	private final UUID uuid_ref;
	private boolean is_functionnal;
	
	private EmbDDB(String master_password_key) throws GeneralSecurityException, IOException {
		uuid_ref = UUID.randomUUID();
		console = new InteractiveConsole();
		protocol = new Protocol(master_password_key);
		poolmanager = new PoolManager(protocol, uuid_ref);
		lock_engine = new LockEngine(poolmanager);
		telemetry = new Telemetry(this);
	}
	
	private void setDurableStoreDirectory(File durable_store_directory, final boolean volatile_dir) throws IOException {
		FileUtils.forceMkdir(durable_store_directory);
		CopyMove.checkExistsCanRead(durable_store_directory);
		CopyMove.checkIsWritable(durable_store_directory);
		
		final File expected_pid_file = new File(durable_store_directory.getPath() + File.separator + "instance.lock");
		ByteBuffer value_pid = ByteBuffer.allocate(4);
		
		FileChannel _channel = null;
		if (expected_pid_file.exists()) {
			_channel = FileChannel.open(expected_pid_file.toPath(), MyDMAM.OPEN_OPTIONS_FILE_EXISTS);
		} else {
			_channel = FileChannel.open(expected_pid_file.toPath(), MyDMAM.OPEN_OPTIONS_FILE_NOT_EXISTS);
		}
		
		final FileChannel channel = _channel;
		final FileLock lock = channel.tryLock();
		if (lock == null) {
			channel.read(value_pid);
			value_pid.flip();
			log.error("Actually the PID #" + value_pid.getInt() + " as currently locked " + expected_pid_file);
			throw new IOException("Can't run more than EmbDDB instances on the same store: " + durable_store_directory);
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				lock.release();
				channel.close();
			} catch (IOException e) {
				log.error("Can't release EmbDDB file lock: " + expected_pid_file.getPath(), e);
			}
			try {
				FileUtils.forceDelete(expected_pid_file);
			} catch (IOException e) {
				log.error("Can't remove EmbDDB file lock: " + expected_pid_file.getPath(), e);
			}
			
			if (volatile_dir) {
				try {
					FileUtils.forceDelete(durable_store_directory);
				} catch (IOException e) {
					log.error("Can't remove store directory: " + durable_store_directory.getPath(), e);
				}
			}
		}, "Release EmbDDB lock file"));
		
		value_pid.putInt(MyDMAM.getJVMProcessPID());
		value_pid.flip();
		channel.write(value_pid);
		channel.force(true);
		
		if (volatile_dir) {
			log.info("Set base volatile store directory to " + durable_store_directory.getAbsolutePath());
		} else {
			log.info("Set base store directory to " + durable_store_directory.getAbsolutePath());
		}
		this.durable_store_directory = durable_store_directory;
	}
	
	private void setVolatileStoreDirectory() throws IOException {
		if (Configuration.global.isElementKeyExists("embddb", "durable_store_directory") == false) {
			return;
		}
		File global_dir = new File(Configuration.global.getValue("embddb", "durable_store_directory", null));
		File volatile_dir = new File(global_dir.getAbsolutePath() + File.separator + "_volatile_" + uuid_ref);
		setDurableStoreDirectory(volatile_dir, true);
	}
	
	public void startServers() throws IOException {
		List<InetSocketAddress> listen_addrs = Configuration.global.getClusterConfiguration("embddb", "listen_only_from", null, protocol.getDefaultTCPPort()).stream().map(item -> {
			return item.getSocketAddress();
		}).collect(Collectors.toList());
		
		poolmanager.startLocalServers(listen_addrs);
	}
	
	public Telemetry getTelemetry() {
		return telemetry;
	}
	
	public void addConsoleOrder(String order, String name, String description, Class<?> creator, BiConsumer<String, PrintStream> procedure) {
		console.addConsoleOrder(order, name, description, creator, procedure);
	}
	
	/**
	 * Blocking !
	 */
	public void startConsole() {
		poolmanager.addConsoleAction(console);
		telemetry.addConsoleAction(console);
		console.waitActions();
	}
	
	public static class CLI implements CLIDefinition {
		
		public String getCliModuleName() {
			return "pool";
		}
		
		public String getCliModuleShortDescr() {
			return "Start pool interactive console (EmbDDB)";
		}
		
		public void execCliModule(ApplicationArgs args) throws Exception {
			EmbDDB embddb = new EmbDDB(getMasterPasswordKey());
			embddb.setVolatileStoreDirectory();
			
			if (args.getParamExist("-listen")) {
				String specific_listen = args.getSimpleParamValue("-listen");
				if (specific_listen == null) {
					embddb.poolmanager.startLocalServers();
				} else {
					List<InetSocketAddress> listen_list = ConfigurationClusterItem.parse(specific_listen, embddb.protocol.getDefaultTCPPort()).map(cci -> {
						return cci.getSocketAddress();
					}).collect(Collectors.toList());
					embddb.poolmanager.startLocalServers(listen_list);
				}
			}
			if (args.getParamExist("-discover")) {
				List<InetSocketAddress> multicast_groups = Configuration.global.getClusterConfiguration("embddb", "multicast_groups", null, embddb.protocol.getDefaultUDPMulticastPort()).stream().map(item -> {
					return item.getSocketAddress();
				}).collect(Collectors.toList());
				embddb.poolmanager.startNetDiscover(multicast_groups);
			}
			if (args.getParamExist("-bootstrap")) {
				List<InetSocketAddress> bootstrap_addrs = Configuration.global.getClusterConfiguration("embddb", "bootstrap_nodes", null, embddb.protocol.getDefaultTCPPort()).stream().map(item -> {
					return item.getSocketAddress();
				}).collect(Collectors.toList());
				
				if (bootstrap_addrs.isEmpty() == false) {
					embddb.poolmanager.setBootstrapPotentialNodes(bootstrap_addrs);
					embddb.poolmanager.connectToBootstrapPotentialNodes("Loaded from configuration, with CLI");
				}
			}
			
			embddb.startConsole();
		}
		
		public void showFullCliModuleHelp() {
			System.out.println("Usage " + getCliModuleName() + " [-listen [addr[:port],]] [-discover] [-bootstrap]");
			System.out.println("With:");
			System.out.println("  -listen for start server, with a local addresses/host and port to listen, IPv4 or 6");
			System.out.println("  -discover for start netdiscover tool, and detect other running nodes");
			System.out.println("  -bootstrap for direct connect at start to some preconfigured nodes addresses. They can be off.");
			System.out.println("Beware if you start CLI on the same host with an other MyDMAM instance (maybe some port listen conflicts).");
		}
		
		public boolean isFunctionnal() {
			return Configuration.global.isElementKeyExists("embddb", "master_password_key");
		}
	}
	
	public LockEngine getLockEngine() {
		return lock_engine;
	}
	
	private IOPipeline pipeline;
	private FileBackend store_file_backend;
	private ReadCache read_cache;
	
	/**
	 * Thread safe, and manage and avoid multiples instances.
	 */
	public <T> DistributedStore<T> getDistributedStore(ItemFactory<T> item_factory, long max_size_for_cached_commit_log, long grace_period_for_expired_items, int expected_item_count, Consistency consistency) throws IOException {
		if (durable_store_directory == null) {
			return null;
		}
		synchronized (durable_store_directory) {
			if (pipeline == null) {
				pipeline = new IOPipeline(poolmanager);
			}
			if (read_cache == null) {
				read_cache = ReadCacheEhcache.getCache();
			}
			if (store_file_backend == null) {
				store_file_backend = new FileBackend(durable_store_directory, uuid_ref, key -> {
					read_cache.remove(key);
				});
			}
			String database_name = Configuration.global.getValue("embddb", "database_name", "default");
			
			DistributedStore<T> previous = pipeline.getStoreByClass(item_factory.getType());
			if (previous != null) {
				return previous;
			}
			
			return new DistributedStore<>(database_name, item_factory, store_file_backend, read_cache, max_size_for_cached_commit_log, grace_period_for_expired_items, expected_item_count, consistency, pipeline);
		}
	}
	
}
