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
 * Copyright (C) hdsdi3g for hd3g.tv 17 sept. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.google.common.math.IntMath;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.StreamMaker;

/**
 * No internal sync, no internal lock. NOT THREAD SAFE. Don't support properly parallel writing.
 */
public class FileHashTable<T> {
	private static Logger log = Logger.getLogger(FileHashTable.class);
	
	private static final byte[] FILE_INDEX_HEADER = "MYDMAMGENHSH".getBytes(MyDMAM.UTF8);
	private static final int FILE_INDEX_VERSION = 1;
	private static final int HASH_ENTRY_SIZE = 12;
	private static final int FILE_INDEX_HEADER_LENGTH = FILE_INDEX_HEADER.length + 4 + 4 + 4;
	
	private final int linked_list_entry_size;
	private final int table_size;
	private final BiConsumer<T, ByteBuffer> factory_to_bytes;
	private final Function<ByteBuffer, T> factory_from_bytes;
	private final int data_buffer_size;
	private final File index_file;
	private final FileChannel channel;
	private final long file_index_start;
	private final long start_linked_lists_zone_in_index_file;
	private volatile long file_index_write_pointer;
	private final PriorityQueue<Long> free_linked_list_item_pointers;
	
	static final Set<OpenOption> OPEN_OPTIONS_FILE_EXISTS;
	static final Set<OpenOption> OPEN_OPTIONS_FILE_NOT_EXISTS;
	
	static {
		OPEN_OPTIONS_FILE_EXISTS = new HashSet<OpenOption>(3);
		Collections.addAll(OPEN_OPTIONS_FILE_EXISTS, StandardOpenOption.READ, StandardOpenOption.WRITE);
		OPEN_OPTIONS_FILE_NOT_EXISTS = new HashSet<OpenOption>(5);
		Collections.addAll(OPEN_OPTIONS_FILE_NOT_EXISTS, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW, StandardOpenOption.SPARSE);
	}
	
	public FileHashTable(File index_file, BiConsumer<T, ByteBuffer> factory_to_bytes, Function<ByteBuffer, T> factory_from_bytes, int data_buffer_size, int default_table_size) throws IOException {
		this.index_file = index_file;
		if (index_file == null) {
			throw new NullPointerException("\"index_file\" can't to be null");
		}
		this.factory_to_bytes = factory_to_bytes;
		if (factory_to_bytes == null) {
			throw new NullPointerException("\"factory_to_bytes\" can't to be null");
		}
		this.factory_from_bytes = factory_from_bytes;
		if (factory_from_bytes == null) {
			throw new NullPointerException("\"factory_from_bytes\" can't to be null");
		}
		this.data_buffer_size = data_buffer_size;
		if (data_buffer_size < 1) {
			throw new IndexOutOfBoundsException("\"data_buffer_size\" can't to be < 0");
		}
		linked_list_entry_size = ItemKey.SIZE + data_buffer_size + 8;
		
		free_linked_list_item_pointers = new PriorityQueue<>((l, r) -> Long.compare(l, r));
		
		ByteBuffer bytebuffer_header_index = ByteBuffer.allocate(FILE_INDEX_HEADER_LENGTH);
		file_index_start = bytebuffer_header_index.capacity();
		
		if (index_file.exists()) {
			channel = FileChannel.open(index_file.toPath(), OPEN_OPTIONS_FILE_EXISTS);
			int size = channel.read(bytebuffer_header_index, 0);
			if (size != FILE_INDEX_HEADER_LENGTH) {
				throw new IOException("Invalid header");
			}
			bytebuffer_header_index.flip();
			
			TransactionJournal.readAndEquals(bytebuffer_header_index, FILE_INDEX_HEADER, bad_datas -> {
				return new IOException("Invalid file header: " + new String(bad_datas));
			});
			int version = bytebuffer_header_index.getInt();
			if (version != FILE_INDEX_VERSION) {
				throw new IOException("Invalid version: " + version + " instead of " + FILE_INDEX_VERSION);
			}
			
			int actual_table_size = bytebuffer_header_index.getInt();
			int actual_key_size = bytebuffer_header_index.getInt();
			if (actual_key_size != ItemKey.SIZE) {
				throw new IOException("Invalid key_size: file is " + actual_key_size + " instead of " + ItemKey.SIZE);
			}
			
			table_size = actual_table_size;
			start_linked_lists_zone_in_index_file = file_index_start + ((long) actual_table_size) * (long) HASH_ENTRY_SIZE;
			
			file_index_write_pointer = Long.max(channel.size(), start_linked_lists_zone_in_index_file);
		} else {
			table_size = default_table_size;
			start_linked_lists_zone_in_index_file = file_index_start + ((long) default_table_size) * (long) HASH_ENTRY_SIZE;
			
			channel = FileChannel.open(index_file.toPath(), OPEN_OPTIONS_FILE_NOT_EXISTS);
			
			bytebuffer_header_index.put(FILE_INDEX_HEADER);
			bytebuffer_header_index.putInt(FILE_INDEX_VERSION);
			bytebuffer_header_index.putInt(default_table_size);
			bytebuffer_header_index.putInt(ItemKey.SIZE);
			bytebuffer_header_index.flip();
			channel.write(bytebuffer_header_index, 0);
			file_index_write_pointer = start_linked_lists_zone_in_index_file;
		}
	}
	
	public void close() throws IOException {
		channel.close();
	}
	
	/*
	 Method: Separate chaining with linked lists
	 Index file struct:
	            <------------ n entries == table_size ------------->
	 [header...][hash entry][hash entry][hash entry][hash entry]...[linked list entry][----------linked list entry----------]...EOF
	            ^ file_index_start      < 12 bytes >                                  <key_size + data_buffer_size + 8 bytes>
	
	 With hash entry struct:
	 <---int, 4 bytes----><----------------long, 8 bytes------------------->
	 [Compressed hash key][absolute position for first index in linked list]
	 
	 With linked list entry struct:
	 <key_size><data_buffer_size><---------------long, 8 bytes------------------>
	 [hash key][--user's datas--][absolute position for linked list's next index]
	*/
	
	private long computeIndexFilePosition(int compressed_key) {
		if (compressed_key > table_size) {
			throw new IndexOutOfBoundsException("Can't get a compressed_key (" + compressed_key + ") > table size (" + table_size + ")");
		}
		return file_index_start + compressed_key * HASH_ENTRY_SIZE;
	}
	
	private int compressKey(byte[] key) {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(key);
		return Math.abs(result) % table_size;
	}
	
	private class HashEntry {
		int compressed_hash_key;
		long linked_list_first_index;
		
		HashEntry(int compressed_hash_key, long linked_list_first_index) {
			this.compressed_hash_key = compressed_hash_key;
			this.linked_list_first_index = linked_list_first_index;
		}
		
		/*
		Hash entry struct:
		<---int, 4 bytes----><----------------long, 8 bytes------------------->
		[Compressed hash key][absolute position for first index in linked list]
		*/
		HashEntry(ByteBuffer read_buffer) {
			compressed_hash_key = read_buffer.getInt();
			linked_list_first_index = read_buffer.getLong();
			if (log.isTraceEnabled() && linked_list_first_index > 0) {
				log.trace("Read HashEntry: " + this);
			}
		}
		
		void writeHashEntry() throws IOException {
			ByteBuffer key_table_buffer = ByteBuffer.allocate(HASH_ENTRY_SIZE);
			key_table_buffer.putInt(compressed_hash_key);
			key_table_buffer.putLong(linked_list_first_index);
			key_table_buffer.flip();
			
			if (log.isTraceEnabled()) {
				log.trace("Write hash_entry " + this);
			}
			
			int size = channel.write(key_table_buffer, computeIndexFilePosition(compressed_hash_key));
			if (size != HASH_ENTRY_SIZE) {
				throw new IOException("Can't write " + HASH_ENTRY_SIZE + " bytes for " + this);
			}
		}
		
		/*public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("compressed_hash_key=" + compressed_hash_key + ",");
			sb.append("linked_list_first_index=" + linked_list_first_index);
			return sb.toString();
		}*/
	}
	
	private class LinkedListEntry {
		byte[] current_key;
		T data;
		long next_linked_list_pointer;
		
		final long linked_list_pointer;
		
		LinkedListEntry(byte[] current_key, T data) {
			if (data == null) {
				throw new NullPointerException("\"data\" can't to be null");
			}
			this.current_key = current_key;
			this.data = data;
			this.next_linked_list_pointer = -1;
			linked_list_pointer = -1;
		}
		
		/*
		Linked list entry struct:
		<key_size><data_buffer_size><---------------long, 8 bytes------------------>
		[hash key][--user's datas--][absolute position for linked list's next index]
		*/
		LinkedListEntry(long linked_list_pointer, ByteBuffer linkedlist_entry_buffer) {
			this.linked_list_pointer = linked_list_pointer;
			if (linked_list_pointer < 1l) {
				throw new NullPointerException("\"linked_list_pointer\" can't to be < 1 (" + linkedlist_entry_buffer + ")");
			}
			
			current_key = new byte[ItemKey.SIZE];
			linkedlist_entry_buffer.get(current_key);
			data = factory_from_bytes.apply(linkedlist_entry_buffer);
			next_linked_list_pointer = linkedlist_entry_buffer.getLong();
			
			if (log.isTraceEnabled()) {
				log.trace("Read LinkedListEntry: current_key = " + MyDMAM.byteToString(current_key) + ", next_linked_list_pointer = " + next_linked_list_pointer);
			}
		}
		
		void toByteBuffer(ByteBuffer write_buffer) {
			write_buffer.put(current_key);
			if (data == null) {
				for (int pos = 0; pos < data_buffer_size; pos++) {
					write_buffer.put((byte) 0);
				}
			} else {
				factory_to_bytes.accept(data, write_buffer);
			}
			write_buffer.putLong(next_linked_list_pointer);
		}
		
		void writeLinkedlistEntry(long linked_list_pointer) throws IOException {
			ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(linked_list_entry_size);
			toByteBuffer(linkedlist_entry_buffer);
			linkedlist_entry_buffer.flip();
			
			if (log.isTraceEnabled()) {
				log.trace("Write linked_list_entry " + this + " in " + linked_list_pointer);
			}
			int size = channel.write(linkedlist_entry_buffer, linked_list_pointer);
			if (size != linked_list_entry_size) {
				throw new IOException("Can't write " + linked_list_entry_size + " bytes for " + this);
			}
		}
		
		/*public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("current_key=#" + MyDMAM.byteToString(current_key).substring(0, 8) + ",");
			sb.append("next_linked_list_pointer=" + next_linked_list_pointer);
			return sb.toString();
		}*/
		
		void clear() throws IOException {
			synchronized (free_linked_list_item_pointers) {
				data = null;
				next_linked_list_pointer = 0;
				for (int i = 0; i < current_key.length; i++) {
					current_key[i] = 0;
				}
				writeLinkedlistEntry(linked_list_pointer);
				free_linked_list_item_pointers.offer(linked_list_pointer);
			}
		}
		
	}
	
	/**
	 * @return empty list if nothing to read (no next_pointer)
	 * @throws RuntimeException
	 */
	private Stream<LinkedListEntry> getAllLinkedListItemsForHashEntry(HashEntry entry) {
		AtomicLong next_pointer = new AtomicLong(entry.linked_list_first_index);
		ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(linked_list_entry_size);
		
		StreamMaker<LinkedListEntry> stream = StreamMaker.create(() -> {
			if (next_pointer.get() <= 0) {
				return null;
			}
			try {
				int s = channel.read(linkedlist_entry_buffer, next_pointer.get());
				if (s != linked_list_entry_size) {
					return null;
				}
				linkedlist_entry_buffer.flip();
				LinkedListEntry r = new LinkedListEntry(next_pointer.get(), linkedlist_entry_buffer);
				linkedlist_entry_buffer.clear();
				
				if (next_pointer.get() == r.next_linked_list_pointer) {
					throw new IOException("Next pointer is this pointer: " + r + " for " + entry);
				}
				next_pointer.set(r.next_linked_list_pointer);
				return r;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		return stream.stream();
	}
	
	private Stream<LinkedListEntry> getAllLinkedListItems() throws IOException, RuntimeException {
		ByteBuffer read_buffer = ByteBuffer.allocate(HASH_ENTRY_SIZE * table_size);
		int size = channel.read(read_buffer, file_index_start);
		if (size < 1) {
			return Stream.empty();
		}
		
		read_buffer.flip();
		if (read_buffer.capacity() != size) {
			return Stream.empty();
		}
		
		StreamMaker<HashEntry> stream = StreamMaker.create(() -> {
			if (read_buffer.remaining() - HASH_ENTRY_SIZE < 0) {
				return null;
			}
			return new HashEntry(read_buffer);
		});
		
		return stream.stream().filter(hash_entry -> {
			return hash_entry.linked_list_first_index > 0;
		}).flatMap(hash_entry -> {
			return getAllLinkedListItemsForHashEntry(hash_entry);
		});
	}
	
	public class EntryValue {
		public final ItemKey key;
		public final T value;
		
		private EntryValue(ItemKey key, T value) {
			this.key = key;
			if (key == null) {
				throw new NullPointerException("\"key\" can't to be null");
			}
			this.value = value;
			if (value == null) {
				throw new NullPointerException("\"value\" can't to be null");
			}
		}
	}
	
	public Stream<EntryValue> stream() throws IOException {
		return getAllLinkedListItems().map(lle -> {
			return new EntryValue(new ItemKey(lle.current_key), lle.data);
		});
	}
	
	/**
	 * @param onDone the Absolute position for the first index in linked list, or null
	 */
	private HashEntry readHashEntry(int compressed_key) throws IOException {
		long index_file_pos = computeIndexFilePosition(compressed_key);
		if (index_file_pos > index_file.length()) {
			if (log.isTraceEnabled()) {
				log.trace("Can't found hash entry (EOF), compress key=" + compressed_key);
			}
			return null;
		}
		
		ByteBuffer key_table_buffer = ByteBuffer.allocate(HASH_ENTRY_SIZE);
		int size = channel.read(key_table_buffer, index_file_pos);
		
		if (size != HASH_ENTRY_SIZE) {
			if (log.isTraceEnabled()) {
				log.trace("Invalid hash entry size (" + size + " bytes(s)), compress key=" + compressed_key);
			}
			return null;
		}
		key_table_buffer.flip();
		int real_compress_key = key_table_buffer.getInt();
		if (compressed_key != real_compress_key) {
			if (log.isTraceEnabled()) {
				log.trace("Invalid hash entry header (real compress key=" + real_compress_key + "), compress key=" + compressed_key);
			}
			return null;
		}
		long result = key_table_buffer.getLong();
		if (result == 0) {
			if (log.isTraceEnabled()) {
				log.trace("Empty value for hash entry, compress key=" + compressed_key);
			}
			return null;
		}
		
		return new HashEntry(compressed_key, result);
	}
	
	/**
	 * Thread safe
	 * @return created item position
	 */
	private long addNewLinkedlistEntry(LinkedListEntry ll_entry) throws IOException {
		ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(linked_list_entry_size);
		ll_entry.toByteBuffer(linkedlist_entry_buffer);
		linkedlist_entry_buffer.flip();
		
		synchronized (free_linked_list_item_pointers) {
			Long linked_list_pointer = free_linked_list_item_pointers.poll();
			
			if (linked_list_pointer == null) {
				linked_list_pointer = file_index_write_pointer;
				file_index_write_pointer += linked_list_entry_size;
				
				if (log.isTraceEnabled()) {
					log.trace("Add new linked_list_entry " + ll_entry + " in " + linked_list_pointer);
				}
			} else if (log.isTraceEnabled()) {
				log.trace("Reuse old linked_list_entry " + ll_entry + " in " + linked_list_pointer);
			}
			
			int size = channel.write(linkedlist_entry_buffer, linked_list_pointer);
			if (size != linked_list_entry_size) {
				throw new IOException("Can't write " + linked_list_entry_size + " bytes for new " + ll_entry);
			}
			return linked_list_pointer;
			
		}
	}
	
	/**
	 * @return data or null
	 */
	private T getDataPointerFromHashKey(byte[] key) throws IOException {
		int compressed_key = compressKey(key);
		
		HashEntry hash_entry = readHashEntry(compressed_key);
		if (hash_entry == null) {
			if (log.isTraceEnabled()) {
				log.trace("Can't found hash key " + MyDMAM.byteToString(key));
			}
			return null;
		}
		
		Stream<LinkedListEntry> linked_list_items = getAllLinkedListItemsForHashEntry(hash_entry);
		
		Optional<LinkedListEntry> o_linked_list_item = linked_list_items.filter(linked_list_item -> {
			return Arrays.equals(key, linked_list_item.current_key);
		}).findFirst();
		
		if (o_linked_list_item.isPresent() == false) {
			return null;
		} else {
			return o_linked_list_item.get().data;
		}
	}
	
	/**
	 * @param data can't to be null
	 */
	public void put(ItemKey item_key, T data) throws IOException {
		if (data == null) {
			throw new NullPointerException("\"data\" can't to be null");
		}
		byte[] key = item_key.key;
		
		int compressed_key = compressKey(key);
		HashEntry hash_entry = readHashEntry(compressed_key);
		
		if (hash_entry == null) {
			if (log.isTraceEnabled()) {
				log.trace("Hash entry (compressed_key=" + compressed_key + ") don't exists, create it and put in the hash table");
			}
			long new_linked_list_pointer = addNewLinkedlistEntry(new LinkedListEntry(key, data));
			new HashEntry(compressed_key, new_linked_list_pointer).writeHashEntry();
		} else {
			if (log.isTraceEnabled()) {
				log.trace("Search if linked list entry exists for key #" + MyDMAM.byteToString(key).substring(0, 12) + " in compressed_key=" + compressed_key);
			}
			List<LinkedListEntry> linked_list_items = getAllLinkedListItemsForHashEntry(hash_entry).collect(Collectors.toList());
			
			Optional<LinkedListEntry> o_linked_list_item = linked_list_items.stream().filter(linked_list_item -> {
				return Arrays.equals(key, linked_list_item.current_key);
			}).findFirst();
			
			if (o_linked_list_item.isPresent()) {
				if (log.isTraceEnabled()) {
					log.trace("Entry exists, replace current entry for #" + MyDMAM.byteToString(key).substring(0, 12) + " in compressed_key=" + compressed_key);
				}
				LinkedListEntry linked_list_entry = o_linked_list_item.get();
				if (data.equals(linked_list_entry.data)) {
					return;
				}
				linked_list_entry.data = data;
				linked_list_entry.writeLinkedlistEntry(linked_list_entry.linked_list_pointer);
			} else {
				if (linked_list_items.isEmpty()) {
					if (log.isTraceEnabled()) {
						log.trace("Append new entry to an empty list for #" + MyDMAM.byteToString(key).substring(0, 12) + " in compressed_key=" + compressed_key);
					}
					long new_linked_list_pointer = addNewLinkedlistEntry(new LinkedListEntry(key, data));
					new HashEntry(compressed_key, new_linked_list_pointer).writeHashEntry();
				} else {
					if (log.isTraceEnabled()) {
						log.trace("Append new entry to actual list (chain) for #" + MyDMAM.byteToString(key).substring(0, 12) + " in compressed_key=" + compressed_key);
					}
					long new_linked_list_pointer = addNewLinkedlistEntry(new LinkedListEntry(key, data));
					
					LinkedListEntry last_item_to_update = linked_list_items.get(linked_list_items.size() - 1);
					last_item_to_update.next_linked_list_pointer = new_linked_list_pointer;
					last_item_to_update.writeLinkedlistEntry(last_item_to_update.linked_list_pointer);
				}
			}
		}
	}
	
	/**
	 * @return entry can be null if not found or if was really null.
	 */
	public T getEntry(ItemKey key) throws IOException {
		return getDataPointerFromHashKey(key.key);
	}
	
	public int size() throws IOException {
		return (int) getAllLinkedListItems()/*.filter(lli -> lli.data_pointer > 0)*/.count();
	}
	
	public boolean isEmpty() throws IOException {
		return getAllLinkedListItems()/*.filter(lli -> lli.data_pointer > 0)*/.findAny().isPresent() == false;
	}
	
	public boolean has(ItemKey key) throws IOException {
		return getDataPointerFromHashKey(key.key) != null;
	}
	
	/**
	 * @return removed old item, or null.
	 */
	public T remove(ItemKey item_key) throws IOException {
		byte[] key = item_key.key;
		int compressed_key = compressKey(key);
		final Predicate<LinkedListEntry> isThisSearchedItem = linked_list_item -> {
			return Arrays.equals(key, linked_list_item.current_key);
		};
		
		HashEntry hash_entry = readHashEntry(compressed_key);
		if (hash_entry == null) {
			if (log.isTraceEnabled()) {
				log.trace("Can't found hash key (compress key=" + compressed_key + ") for " + item_key);
			}
			return null;
		} else {
			List<LinkedListEntry> hash_entry_linked_list = StreamMaker.takeUntilTrigger(isThisSearchedItem, getAllLinkedListItemsForHashEntry(hash_entry)).collect(Collectors.toList());
			if (hash_entry_linked_list.isEmpty()) {
				if (log.isTraceEnabled()) {
					log.trace("Nothing to remove: empty list (hash_entry=" + hash_entry + ") for " + item_key);
				}
				return null;
			}
			LinkedListEntry last_linked_list_item_to_remove = hash_entry_linked_list.get(hash_entry_linked_list.size() - 1);
			
			if (isThisSearchedItem.test(last_linked_list_item_to_remove) == false) {
				if (log.isTraceEnabled()) {
					log.trace("Item is not present to hash_list... so, nothing to remove (hash_entry=" + hash_entry + ") for " + item_key);
				}
				return null;
			}
			
			if (hash_entry.linked_list_first_index != hash_entry_linked_list.get(0).linked_list_pointer) {
				throw new IOException("Invalid hashtable structure for " + compressed_key + " (" + hash_entry.linked_list_first_index + ", " + hash_entry_linked_list.get(0).linked_list_pointer);
			}
			
			T old_item = last_linked_list_item_to_remove.data;
			long next_valid_linked_list_pointer = last_linked_list_item_to_remove.next_linked_list_pointer;
			
			/**
			 * Clear the actual
			 */
			last_linked_list_item_to_remove.clear();
			
			if (hash_entry_linked_list.size() == 1) {
				/**
				 * {55:A}...[A>B][B>-1], remove [A]: {55:B}...-----[B>-1]
				 * change hash_entry first target == me.next.target
				 */
				hash_entry.linked_list_first_index = next_valid_linked_list_pointer;
				hash_entry.writeHashEntry();
			} else {
				/**
				 * [A>B][B>C][C>-1], remove [B]: [A>C]-----[C>-1]
				 * change the me.previous.next_target == me.next.target
				 */
				LinkedListEntry last_valid_linked_list_item = hash_entry_linked_list.get(hash_entry_linked_list.size() - 2);
				last_valid_linked_list_item.next_linked_list_pointer = next_valid_linked_list_pointer;
				last_valid_linked_list_item.writeLinkedlistEntry(last_valid_linked_list_item.linked_list_pointer);
			}
			return old_item;
		}
	}
	
	public void clear() throws IOException {
		synchronized (free_linked_list_item_pointers) {
			log.info("Clear " + index_file);
			free_linked_list_item_pointers.clear();
			file_index_write_pointer = start_linked_lists_zone_in_index_file;
			channel.truncate(FILE_INDEX_HEADER_LENGTH);
			channel.force(true);
		}
	}
	
	void purge() throws IOException {
		if (channel.isOpen()) {
			channel.close();
		}
		FileUtils.forceDelete(index_file);
	}
	
	public static final int computeHashTableBestSize(int estimate_number_of_elements) {
		if (estimate_number_of_elements == 0) {
			return computeHashTableBestSize(1000);
		}
		return IntMath.pow(2, 1 + IntMath.log2(estimate_number_of_elements, RoundingMode.CEILING));
	}
	
}
