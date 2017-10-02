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
 * Copyright (C) hdsdi3g for hd3g.tv 1 oct. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.Hexview;
import hd3gtv.tools.StreamMaker;

/**
 * Not thread safe !
 */
public class FileIndexPaths {
	private static Logger log = Logger.getLogger(FileIndexPaths.class);
	
	private static final byte[] FILE_LLIST_HEADER = "MYDMAMGENLKL".getBytes(MyDMAM.UTF8);
	private static final int FILE_LLIST_VERSION = 1;
	private static final int FILE_LLIST_HEADER_LENGTH = FILE_LLIST_HEADER.length + 4;
	private static final byte[] ENTRY_PREFIX = "ITEM".getBytes(MyDMAM.UTF8);
	private static final int LLIST_ENTRY_SIZE = ENTRY_PREFIX.length + ItemKey.SIZE + 8;
	
	private final FileHashTableLong hash_table;
	
	private final File llist_file;
	private final FileChannel channel;
	private volatile long file_index_write_pointer;
	
	public FileIndexPaths(File index_file, File llist_file, int default_table_size) throws IOException {
		this.llist_file = llist_file;
		if (llist_file == null) {
			throw new NullPointerException("\"llist_file\" can't to be null");
		}
		hash_table = new FileHashTableLong(index_file, default_table_size);
		
		ByteBuffer bytebuffer_header_index = ByteBuffer.allocate(FILE_LLIST_HEADER_LENGTH);
		
		if (llist_file.exists()) {
			channel = FileChannel.open(llist_file.toPath(), FileHashTable.OPEN_OPTIONS_FILE_EXISTS);
			int size = channel.read(bytebuffer_header_index, 0);
			if (size != FILE_LLIST_HEADER_LENGTH) {
				throw new IOException("Invalid header");
			}
			bytebuffer_header_index.flip();
			
			TransactionJournal.readAndEquals(bytebuffer_header_index, FILE_LLIST_HEADER, bad_datas -> {
				return new IOException("Invalid file header: " + new String(bad_datas));
			});
			int version = bytebuffer_header_index.getInt();
			if (version != FILE_LLIST_VERSION) {
				throw new IOException("Invalid version: " + version + " instead of " + FILE_LLIST_VERSION);
			}
			
			file_index_write_pointer = channel.size();
		} else {
			channel = FileChannel.open(llist_file.toPath(), FileHashTable.OPEN_OPTIONS_FILE_NOT_EXISTS);
			bytebuffer_header_index.put(FILE_LLIST_HEADER);
			bytebuffer_header_index.putInt(FILE_LLIST_VERSION);
			bytebuffer_header_index.flip();
			int size = channel.write(bytebuffer_header_index, 0);
			if (size != FILE_LLIST_HEADER_LENGTH) {
				throw new IOException("Can't write the header");
			}
			file_index_write_pointer = FILE_LLIST_HEADER_LENGTH;
		}
	}
	
	public void close() throws IOException {
		hash_table.close();
		channel.close();
	}
	
	/*
	Linked list entry struct:
	[header...][linked list entry][linked list entry]...EOF
	                                                    ^ file_index_write_pointer
	With entry blocks:
	        <-----key_size-----><---------------long, 8 bytes------------------>
	[prefix][user data hash key][absolute position for linked list's next index]
	^ linked_list_pointer
	*/
	
	private class LinkedListEntry {
		byte[] user_data_hash_key;
		long next_linked_list_pointer;
		// final long linked_list_pointer;
		
		LinkedListEntry(byte[] user_data_hash_key, long next_linked_list_pointer) {
			this.user_data_hash_key = user_data_hash_key;
			if (user_data_hash_key == null) {
				throw new NullPointerException("\"user_data_hash_key\" can't to be null");
			}
			this.next_linked_list_pointer = next_linked_list_pointer;
			// linked_list_pointer = -1;
		}
		
		LinkedListEntry(byte[] user_data_hash_key) {
			this(user_data_hash_key, -1);
		}
		
		/*
		Entry blocks:
		    	<-----key_size-----><---------------long, 8 bytes------------------>
		[prefix][user data hash key][absolute position for linked list's next index]
		^ linked_list_pointer
		*/
		LinkedListEntry(/*long linked_list_pointer, */ByteBuffer linkedlist_entry_buffer) throws IOException {
			/*this.linked_list_pointer = linked_list_pointer;
			if (linked_list_pointer < 1l) {
				throw new NullPointerException("\"linked_list_pointer\" can't to be < 1 (" + linkedlist_entry_buffer + ")");
			}*/
			
			byte[] prefix = new byte[ENTRY_PREFIX.length];
			linkedlist_entry_buffer.get(prefix);
			if (Arrays.equals(prefix, ENTRY_PREFIX) == false) {
				throw new IOException("Invalid preflix: " + Hexview.tracelog(prefix));
			}
			user_data_hash_key = new byte[ItemKey.SIZE];
			linkedlist_entry_buffer.get(user_data_hash_key);
			next_linked_list_pointer = linkedlist_entry_buffer.getLong();
			
			if (log.isTraceEnabled()) {
				log.trace("Read LinkedListEntry: current_key = " + MyDMAM.byteToString(user_data_hash_key) + ", next_linked_list_pointer = " + next_linked_list_pointer);
			}
		}
		
		void toByteBuffer(ByteBuffer write_buffer) {
			write_buffer.put(ENTRY_PREFIX);
			/*if (user_data_hash_key == null) {
				for (int pos = 0; pos < ItemKey.SIZE; pos++) {
					write_buffer.put((byte) 0);
				}
			} else {*/
			write_buffer.put(user_data_hash_key);
			// }
			write_buffer.putLong(next_linked_list_pointer);
		}
		
		/*void writeLinkedlistEntry(long linked_list_pointer) throws IOException {
			ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(LLIST_ENTRY_SIZE);
			toByteBuffer(linkedlist_entry_buffer);
			linkedlist_entry_buffer.flip();
			
			if (log.isTraceEnabled()) {
				log.trace("Write linked_list_entry " + this + " in " + linked_list_pointer);
			}
			int size = channel.write(linkedlist_entry_buffer, linked_list_pointer);
			if (size != LLIST_ENTRY_SIZE) {
				throw new IOException("Can't write " + LLIST_ENTRY_SIZE + " bytes for " + this);
			}
		}*/
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("user_data_hash_key=#" + MyDMAM.byteToString(user_data_hash_key).substring(0, 8) + ",");
			sb.append("next_linked_list_pointer=" + next_linked_list_pointer);
			return sb.toString();
		}
		
	}
	
	/**
	 * @return empty list if nothing to read (no next_pointer)
	 * @throws RuntimeException
	 */
	private Stream<LinkedListEntry> getAllLinkedListItemsForHashEntry(long linked_list_first_index) {
		AtomicLong next_pointer = new AtomicLong(linked_list_first_index);
		ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(LLIST_ENTRY_SIZE);
		
		StreamMaker<LinkedListEntry> stream = StreamMaker.create(() -> {
			if (next_pointer.get() <= 0) {
				return null;
			}
			try {
				int s = channel.read(linkedlist_entry_buffer, next_pointer.get());
				if (s != LLIST_ENTRY_SIZE) {
					return null;
				}
				linkedlist_entry_buffer.flip();
				LinkedListEntry r = new LinkedListEntry(/*next_pointer.get(), */linkedlist_entry_buffer);
				linkedlist_entry_buffer.clear();
				
				if (next_pointer.get() == r.next_linked_list_pointer) {
					throw new IOException("Next pointer is this pointer: " + r + " for start " + linked_list_first_index);
				}
				next_pointer.set(r.next_linked_list_pointer);
				return r;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		return stream.stream();
	}
	
	/**
	 * @return created item position
	 */
	private long addNewLinkedlistEntry(LinkedListEntry ll_entry) throws IOException {
		ByteBuffer linkedlist_entry_buffer = ByteBuffer.allocate(LLIST_ENTRY_SIZE);
		ll_entry.toByteBuffer(linkedlist_entry_buffer);
		linkedlist_entry_buffer.flip();
		
		long linked_list_pointer = file_index_write_pointer;
		file_index_write_pointer += LLIST_ENTRY_SIZE;
		
		if (log.isTraceEnabled()) {
			log.trace("Add new linked_list_entry " + ll_entry + " in " + linked_list_pointer);
		}
		
		int size = channel.write(linkedlist_entry_buffer, linked_list_pointer);
		if (size != LLIST_ENTRY_SIZE) {
			throw new IOException("Can't write " + LLIST_ENTRY_SIZE + " bytes for new " + ll_entry);
		}
		return linked_list_pointer;
	}
	
	private Stream<ItemKey> getAllKeysInPath(ItemKey path) throws IOException {
		long linked_list_first_index = hash_table.getEntry(path);
		if (linked_list_first_index < 1) {
			return Stream.empty();
		}
		return getAllLinkedListItemsForHashEntry(linked_list_first_index).map(item -> {
			return new ItemKey(item.user_data_hash_key);
		});
	}
	
	/**
	 * @param path null and empty will be ignored
	 */
	public void add(ItemKey item_key, String path) throws IOException {
		if (item_key == null) {
			throw new NullPointerException("\"item_key\" can't to be null");
		}
		if (path == null) {
			return;
		}
		if (path.isEmpty()) {
			return;
		}
		
		ItemKey hash_path = new ItemKey(path);
		long linked_list_first_index = hash_table.getEntry(hash_path);
		
		if (linked_list_first_index < 1) {
			if (log.isTraceEnabled()) {
				log.trace("Path \"" + path + "\" don't exists, create it and put in the hash table");
			}
			long new_linked_list_pointer = addNewLinkedlistEntry(new LinkedListEntry(item_key.key));
			hash_table.put(hash_path, new_linked_list_pointer);
		} else {
			if (log.isTraceEnabled()) {
				log.trace("Search if linked list entry exists for path \"" + path + "\"");
			}
			
			Optional<LinkedListEntry> o_linked_list_item = getAllLinkedListItemsForHashEntry(linked_list_first_index).filter(linked_list_item -> {
				return Arrays.equals(item_key.key, linked_list_item.user_data_hash_key);
			}).findFirst();
			
			if (o_linked_list_item.isPresent() == false) {
				if (getAllLinkedListItemsForHashEntry(linked_list_first_index).findFirst().isPresent() == false) {
					if (log.isTraceEnabled()) {
						log.trace("Append new entry to an empty list for path \"" + path + "\"");
					}
					long new_linked_list_pointer = addNewLinkedlistEntry(new LinkedListEntry(item_key.key));
					hash_table.put(hash_path, new_linked_list_pointer);
				} else {
					if (log.isTraceEnabled()) {
						log.trace("Append new entry to actual list (chain) for path \"" + path + "\"");
					}
					long new_linked_list_pointer = addNewLinkedlistEntry(new LinkedListEntry(item_key.key, linked_list_first_index));
					hash_table.put(hash_path, new_linked_list_pointer);
				}
			} else {
				if (log.isTraceEnabled()) {
					log.trace("Entry exists in the correct path");
				}
				return;
			}
		}
	}
	
	public int pathCount() throws IOException {
		return hash_table.size();
	}
	
	public boolean isEmpty() throws IOException {
		return hash_table.isEmpty();
	}
	
	public boolean has(String path) throws IOException {
		return hash_table.has(new ItemKey(path));
	}
	
	/**
	 * Only remove reference, not linked list items
	 */
	public void remove(String path) throws IOException {
		if (path.equals("/") | path.equals("/*")) {
			return;
		}
		hash_table.remove(new ItemKey(path));
		if (path.endsWith("*")) {
			hash_table.remove(new ItemKey(path.substring(0, path.length() - 1)));
		} else {
			hash_table.remove(new ItemKey(path + "*"));
		}
	}
	
	/**
	 * Can't all items, because it'll be slow.
	 */
	public Stream<ItemKey> getAllKeysInPath(String path) throws IOException {
		if (path == null) {
			throw new NullPointerException("\"path\" can't to be null");
		} else if (path.isEmpty()) {
			throw new NullPointerException("\"path\" can't to be empty");
		} else if (path.endsWith("*")) {
			throw new NullPointerException("Invalid path: ends with \"*\"");
		}
		
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return getAllKeysInPath(new ItemKey(path));
	}
	
	public void clear() throws IOException {
		log.info("Clear " + llist_file);
		hash_table.clear();
		file_index_write_pointer = FILE_LLIST_HEADER_LENGTH;
		channel.truncate(FILE_LLIST_HEADER_LENGTH);
		channel.force(true);
	}
	
}
