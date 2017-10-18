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
 * Copyright (C) hdsdi3g for hd3g.tv 11 sept. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.embddb.store.FileBackend.StoreBackend;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.tools.ThreadPoolExecutorFactory;

/**
 * Polyvalent and agnostic object storage
 * Bind Backend and StoreItemFactory
 */
@GsonIgnore
public final class Store<T> implements Closeable {
	private static Logger log = Logger.getLogger(Store.class);
	
	protected final String database_name;
	private final ReadCache read_cache;
	private final StoreBackend backend;
	private final ItemFactory<T> item_factory;
	private final ThreadPoolExecutorFactory executor;
	private final ScheduledExecutorService scheduled_ex_service;
	
	private final ConcurrentHashMap<ItemKey, Item> journal_write_cache;
	private final AtomicLong journal_write_cache_size;
	private final long grace_period_for_expired_items;
	private final String generic_class_name;
	private volatile boolean closed;
	
	/**
	 * @param read_cache dedicated cache for this store
	 */
	public Store(String database_name, ItemFactory<T> item_factory, FileBackend file_backend, ReadCache read_cache, long max_size_for_cached_commit_log, long grace_period_for_expired_items, int expected_item_count) throws IOException {
		closed = false;
		this.database_name = database_name;
		if (database_name == null) {
			throw new NullPointerException("\"database_name\" can't to be null");
		}
		this.item_factory = item_factory;
		if (item_factory == null) {
			throw new NullPointerException("\"item_factory\" can't to be null");
		}
		if (file_backend == null) {
			throw new NullPointerException("\"file_backend\" can't to be null");
		}
		
		generic_class_name = item_factory.getType().getSimpleName();
		backend = file_backend.get(database_name, generic_class_name, expected_item_count);
		
		this.read_cache = read_cache;
		if (read_cache == null) {
			throw new NullPointerException("\"read_cache\" can't to be null");
		}
		journal_write_cache = new ConcurrentHashMap<>();
		journal_write_cache_size = new AtomicLong(0);
		if (max_size_for_cached_commit_log < 1l) {
			throw new NullPointerException("\"max_size_for_cached_commit_log\" can't to be < 1");
		}
		this.grace_period_for_expired_items = grace_period_for_expired_items;
		if (grace_period_for_expired_items < 1l) {
			throw new NullPointerException("\"grace_period_for_expired_items\" can't to be < 1");
		}
		
		executor = new ThreadPoolExecutorFactory("EMBDDB-Store-" + database_name + "_" + generic_class_name, Thread.MIN_PRIORITY + 1, e -> {
			log.error("Genric error for " + database_name + "/" + generic_class_name, e);
		});
		
		scheduled_ex_service = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(false);
				t.setName("Store durable writes for " + database_name + "_" + generic_class_name);
				t.setPriority(Thread.MIN_PRIORITY + 2);
				return t;
			}
		});
		
		scheduled_ex_service.scheduleAtFixedRate(() -> {
			if (journal_write_cache_size.get() > max_size_for_cached_commit_log) {
				try {
					doDurableWrites();
				} catch (Exception e1) {
					log.error("Error during durable writes for store " + database_name + "/" + generic_class_name, e1);
					scheduled_ex_service.shutdown();
				}
			}
		}, 1, 1, TimeUnit.SECONDS);
	}
	
	/**
	 * Blocking
	 */
	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		scheduled_ex_service.shutdown();
		while (scheduled_ex_service.isTerminated() == false) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
			}
		}
		executor.shutdownAndTerminate();
		backend.close();
	}
	
	public CompletableFuture<String> put(String _id, T element, long ttl, TimeUnit unit) {
		if (closed) {
			throw new RuntimeException("Store is closed");
		}
		return put(_id, null, element, ttl, unit);
	}
	
	public CompletableFuture<String> put(String _id, String path, T element, long ttl, TimeUnit unit) {
		if (closed) {
			throw new RuntimeException("Store is closed");
		}
		return CompletableFuture.supplyAsync(() -> {
			try {
				put(item_factory.toItem(element).setPath(path).setId(_id).setTTL(unit.toMillis(ttl)));
				return _id;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);
	}
	
	private void put(Item item) throws IOException {
		ItemKey key = item.getKey();
		Item old_item = journal_write_cache.put(key, item);
		if (old_item != null) {
			journal_write_cache_size.getAndAdd(-old_item.getPayload().length);
		}
		journal_write_cache_size.getAndAdd(item.getPayload().length);
		read_cache.remove(key);
		backend.writeInJournal(item, item.getDeleteDate() + grace_period_for_expired_items);
	}
	
	/**
	 * @return null if not found
	 */
	public CompletableFuture<T> get(String _id) {
		if (closed) {
			throw new RuntimeException("Store is closed");
		}
		return CompletableFuture.supplyAsync(() -> {
			try {
				ItemKey key = new ItemKey(_id);
				
				Item item = journal_write_cache.get(key);
				if (item == null) {
					item = read_cache.get(key);
					if (item == null) {
						ByteBuffer read_buffer = backend.read(key);
						if (read_buffer == null) {
							return null;
						}
						item = new Item(read_buffer);
						read_cache.put(item);
					}
				}
				
				if (item.isDeleted()) {
					return null;
				}
				return item_factory.getFromItem(item);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}, executor);
	}
	
	/**
	 * Don't check item TTL/deleted, just reference presence.
	 */
	public CompletableFuture<Boolean> hasPresence(String _id) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				ItemKey key = new ItemKey(_id);
				if (journal_write_cache.containsKey(key)) {
					return true;
				} else if (read_cache.has(key)) {
					return true;
				} else if (backend.contain(key)) {
					return true;
				} else {
					return false;
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);
	}
	
	public CompletableFuture<String> removeById(String _id) {
		if (closed) {
			throw new RuntimeException("Store is closed");
		}
		return CompletableFuture.supplyAsync(() -> {
			try {
				ItemKey key = new ItemKey(_id);
				Item actual_item = null;
				if (journal_write_cache.containsKey(key)) {
					actual_item = journal_write_cache.get(key);
					if (actual_item.isDeleted()) {
						return _id;
					}
				} else {
					actual_item = read_cache.get(key);
					if (actual_item == null) {
						ByteBuffer read_buffer = backend.read(key);
						if (read_buffer == null) {
							return _id;
						}
						actual_item = new Item(read_buffer);
					}
				}
				put(actual_item.setPayload(new byte[0]).setTTL(-1l));
				return _id;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);
	}
	
	/**
	 * @return all items presence, even deleted
	 */
	private Stream<Item> mapToItemAndPushToReadCacheAndIsActuallyNotExistsInCommitLog(Stream<ByteBuffer> source) {
		return source.map(read_buffer -> {
			Item item = new Item(read_buffer);
			read_cache.put(item);
			return item;
		}).filter(item -> {
			return journal_write_cache.containsKey(item.getKey()) == false;
		});
	}
	
	/**
	 * @return ignore deleted
	 */
	private Stream<Item> accumulateWithCommitLog(Stream<Item> stored_items, HashSet<Item> commit_log_filtered) {
		stored_items.filter(item -> {
			return item.isDeleted() == false;
		}).forEach(item -> {
			if (commit_log_filtered.contains(item) == false) {
				commit_log_filtered.add(item);
			}
		});
		return commit_log_filtered.stream();
	}
	
	private Stream<Item> getAllNonDeletedItems() throws IOException {
		Stream<Item> stored_items = mapToItemAndPushToReadCacheAndIsActuallyNotExistsInCommitLog(backend.getAllDatas());
		
		return accumulateWithCommitLog(stored_items, new HashSet<>(journal_write_cache.values())).filter(item -> {
			return item.isDeleted() == false;
		});
	}
	
	public CompletableFuture<List<T>> getAllValues() {
		if (closed) {
			throw new RuntimeException("Store is closed");
		}
		return CompletableFuture.supplyAsync(() -> {
			try {
				return getAllNonDeletedItems().map(item -> {
					return item_factory.getFromItem(item);
				}).collect(Collectors.toList());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);
	}
	
	public CompletableFuture<Map<ItemKey, T>> getAll() {
		if (closed) {
			throw new RuntimeException("Store is closed");
		}
		return CompletableFuture.supplyAsync(() -> {
			try {
				return getAllNonDeletedItems().collect(Collectors.toMap(item -> {
					return item.getKey();
				}, item -> {
					return item_factory.getFromItem(item);
				}));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);
	}
	
	private Stream<Item> internalGetByPath(String path) throws IOException {
		Stream<Item> stored_items = mapToItemAndPushToReadCacheAndIsActuallyNotExistsInCommitLog(backend.getDatasByPath(path));
		HashSet<Item> commit_log_filtered = new HashSet<Item>(journal_write_cache.values().stream().filter(item -> {
			return item.isDeleted() == false && item.getPath().startsWith(path);
		}).collect(Collectors.toSet()));
		
		return accumulateWithCommitLog(stored_items, commit_log_filtered);
	}
	
	public CompletableFuture<List<T>> getByPath(String path) {
		if (closed) {
			throw new RuntimeException("Store is closed");
		}
		return CompletableFuture.supplyAsync(() -> {
			try {
				return internalGetByPath(path).map(item -> {
					return item_factory.getFromItem(item);
				}).collect(Collectors.toList());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);
	}
	
	private void remove(Stream<Item> items) {
		items.forEach(item -> {
			item.setPayload(new byte[0]);
			item.setTTL(-1);
			item.setPath(null);
			ItemKey key = item.getKey();
			Item old_item = journal_write_cache.put(key, item);
			if (old_item != null) {
				journal_write_cache_size.getAndAdd(-old_item.getPayload().length);
			}
			read_cache.remove(key);
			try {
				backend.writeInJournal(item, System.currentTimeMillis() + grace_period_for_expired_items);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}
	
	public CompletableFuture<String> removeAllByPath(String path) {
		if (closed) {
			throw new RuntimeException("Store is closed");
		}
		return CompletableFuture.supplyAsync(() -> {
			try {
				remove(internalGetByPath(path));
				return path;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);
	}
	
	/**
	 * Blocking.
	 */
	public void truncate() throws Exception {
		if (closed) {
			throw new RuntimeException("Store is closed");
		}
		executor.insertPauseTask(() -> {
			journal_write_cache_size.set(0);
			journal_write_cache.clear();
			read_cache.clear();
			try {
				backend.clear();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}
	
	public String getDatabaseName() {
		return database_name;
	}
	
	/**
	 * Blocking.
	 */
	public void doDurableWrites() throws Exception {
		if (closed) {
			throw new RuntimeException("Store is closed");
		}
		executor.insertPauseTask(() -> {
			try {
				List<ItemKey> updated = backend.doDurableWritesAndRotateJournal();
				updated.forEach(k -> {
					read_cache.remove(k);
				});
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			journal_write_cache_size.set(0);
			journal_write_cache.clear();
		});
	}
	
	/**
	 * Blocking.
	 */
	public void cleanUpFiles() throws Exception {
		if (closed) {
			throw new RuntimeException("Store is closed");
		}
		executor.insertPauseTask(() -> {
			try {
				backend.cleanUpFiles();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}
	
	/**
	 * Blocking.
	 */
	public void doDurableWritesAndCleanUpFiles() throws Exception {
		if (closed) {
			throw new RuntimeException("Store is closed");
		}
		executor.insertPauseTask(() -> {
			try {
				List<ItemKey> updated = backend.doDurableWritesAndRotateJournal();
				updated.forEach(k -> {
					read_cache.remove(k);
				});
				journal_write_cache_size.set(0);
				journal_write_cache.clear();
				backend.cleanUpFiles();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
	}
	
	private static final String XML_ROOT_ELEMENT = "embddb_store";
	private static final String XML_ROOT_ELEMENT_ATTR_VERSION = "version";
	private static final String XML_ROOT_ELEMENT_ATTR_CREATED = "created";
	private static final String XML_ROOT_ELEMENT_ATTR_DATABASE = "database";
	private static final String XML_ROOT_ELEMENT_ATTR_CLASSNAME = "classname";
	
	public CompletableFuture<File> xmlExport() {
		if (closed) {
			throw new RuntimeException("Store is closed");
		}
		return CompletableFuture.supplyAsync(() -> {
			FileOutputStream fileoutputstream = null;
			try {
				long now = System.currentTimeMillis();
				File destination = backend.makeFile(Loggers.dateFilename(now) + ".xml");
				
				fileoutputstream = new FileOutputStream(destination);
				OutputFormat of = new OutputFormat();
				of.setMethod("xml");
				of.setEncoding("UTF-8");
				of.setVersion("1.0");
				of.setIndenting(true);
				of.setIndent(2);
				XMLSerializer serializer = new XMLSerializer(fileoutputstream, of);
				serializer.startDocument();
				
				/**
				 * Headers
				 */
				AttributesImpl atts = new AttributesImpl();
				atts.addAttribute("", "", XML_ROOT_ELEMENT_ATTR_VERSION, "CDATA", String.valueOf(XML_DOCUMENT_VERSION));
				atts.addAttribute("", "", XML_ROOT_ELEMENT_ATTR_CREATED, "CDATA", String.valueOf(now));
				atts.addAttribute("", "", XML_ROOT_ELEMENT_ATTR_DATABASE, "CDATA", database_name);
				atts.addAttribute("", "", XML_ROOT_ELEMENT_ATTR_CLASSNAME, "CDATA", generic_class_name);
				
				serializer.startElement("", "", XML_ROOT_ELEMENT, atts);
				serializer.comment("created: " + Loggers.dateLog(now));
				
				Stream<Item> stored_items = mapToItemAndPushToReadCacheAndIsActuallyNotExistsInCommitLog(backend.getAllDatas());
				Stream<Item> stored_items_with_comit_log = accumulateWithCommitLog(stored_items, new HashSet<>(journal_write_cache.values()));
				
				stored_items_with_comit_log.forEach(item -> {
					try {
						item.toXML(serializer);
					} catch (SAXException | IOException e) {
						throw new RuntimeException(e);
					}
				});
				
				serializer.endElement("", "", XML_ROOT_ELEMENT);
				serializer.endDocument();
				
				return destination;
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				if (fileoutputstream != null) {
					try {
						fileoutputstream.close();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}, executor);
	}
	
	public static final int XML_DOCUMENT_VERSION = 1;
	
	/**
	 * Blocking.
	 */
	public CompletableFuture<File> xmlImport(File document) throws Exception {
		if (closed) {
			throw new RuntimeException("Store is closed");
		}
		return CompletableFuture.supplyAsync(() -> {
			try {
				XMLImport xml_import = new XMLImport();
				
				SAXParserFactory fabrique = SAXParserFactory.newInstance();
				SAXParser parser = fabrique.newSAXParser();
				
				try {
					fabrique.setFeature("http://xml.org/sax/features/external-general-entities", false);
				} catch (ParserConfigurationException pce) {
				}
				try {
					fabrique.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
				} catch (ParserConfigurationException pce) {
				}
				parser.getXMLReader().setProperty("http://xml.org/sax/properties/lexical-handler", xml_import);
				
				InputStream fis = new BufferedInputStream(new FileInputStream(document), 0xFFFF);
				InputSource is = new InputSource(fis);
				is.setEncoding("UTF-8");
				parser.parse(is, xml_import);
				fis.close();
				return document;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}, executor);
	}
	
	private class XMLImport extends DefaultHandler implements ErrorHandler, LexicalHandler {
		
		StringBuilder rawtext = null;
		String current_raw_payload = null;
		
		public void startDocument() throws SAXException {
			rawtext = new StringBuilder();
		}
		
		HashMap<String, String> current_item_attributes;
		
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (qName.equals(XML_ROOT_ELEMENT)) {
				int current_version = Integer.parseInt(attributes.getValue(XML_ROOT_ELEMENT_ATTR_VERSION));
				if (current_version != XML_DOCUMENT_VERSION) {
					throw new SAXException("Invalid XML version " + current_version);
				}
				String current_database_name = attributes.getValue(XML_ROOT_ELEMENT_ATTR_DATABASE);
				String current_generic_class_name = attributes.getValue(XML_ROOT_ELEMENT_ATTR_CLASSNAME);
				
				if (database_name.equalsIgnoreCase(current_database_name) == false) {
					throw new SAXException("Invalid database name: " + current_database_name + " (this is " + database_name + ")");
				} else if (generic_class_name.equalsIgnoreCase(current_generic_class_name) == false) {
					throw new SAXException("Invalid database name: " + current_generic_class_name + " (this is " + generic_class_name + ")");
				}
				
				log.debug("XML creation date: " + Loggers.dateLog(Long.valueOf(attributes.getValue(XML_ROOT_ELEMENT_ATTR_CREATED))));
			} else if (qName.equals(Item.XML_ITEM_ELEMENT)) {
				
				int attr_size = attributes.getLength();
				current_item_attributes = new HashMap<>(attr_size);
				for (int pos = 0; pos < attr_size; pos++) {
					current_item_attributes.put(attributes.getQName(pos), attributes.getValue(pos));
				}
				current_raw_payload = "";
			} else if (qName.equals(Item.XML_ITEM_CHUNK)) {
				/**
				 * Do nothing...
				 */
			} else {
				throw new SAXException("Unknown XML qName: " + qName);
			}
		}
		
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (qName.equals(Item.XML_ITEM_ELEMENT)) {
				try {
					put(new Item(current_item_attributes, current_raw_payload));
					rawtext = null;
					current_raw_payload = null;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		public void startCDATA() throws SAXException {
			rawtext = new StringBuilder();
		}
		
		public void endCDATA() throws SAXException {
			current_raw_payload += rawtext.toString();
		}
		
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (rawtext != null) {
				rawtext.append(new String(ch, start, length));
			}
		}
		
		public void comment(char[] ch, int start, int length) throws SAXException {
			if (log.isTraceEnabled()) {
				String comment = new String(ch, start, length);
				if (comment.trim().isEmpty() == false) {
					log.trace("XML Comment: " + comment);
				}
			}
		}
		
		public void endDocument() throws SAXException {
		}
		
		public void error(SAXParseException e) throws SAXException {
			log.error("XML Parsing error", e);
		}
		
		public void fatalError(SAXParseException e) throws SAXException {
			log.error("XML Parsing error", e);
		}
		
		public void warning(SAXParseException e) throws SAXException {
			log.error("XML Parsing warning", e);
		}
		
		public void startDTD(String name, String publicId, String systemId) throws SAXException {
		}
		
		public void endDTD() throws SAXException {
		}
		
		public void startEntity(String name) throws SAXException {
		}
		
		public void endEntity(String name) throws SAXException {
		}
		
	}
	
	// TODO network I/O
	
}
