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
 * Copyright (C) hdsdi3g for hd3g.tv 27 janv. 2018
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import org.apache.log4j.Logger;

import hd3gtv.mydmam.MyDMAM;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class UIBlockView extends Application {
	private static Logger log = Logger.getLogger(UIBlockView.class);
	
	public static void main(String[] args) {
		Application.launch(args);
	}
	
	private final TreeItem<AtomBlockEntry> root_node;
	
	public UIBlockView() {
		root_node = new TreeItem<>();
		root_node.setExpanded(true);
	}
	
	/**
	 * See https://docs.oracle.com/javase/8/javafx/user-interface-tutorial/tree-table-view.htm
	 */
	public void start(Stage stage) throws Exception {
		BorderPane border = new BorderPane();
		final Scene scene = new Scene(border);
		stage.setScene(scene);
		stage.setTitle("MyDMAM EmbDDB AtomBlock viewer");
		
		final TreeTableView<AtomBlockEntry> treeTableView = new TreeTableView<>(root_node);
		border.setCenter(treeTableView);
		treeTableView.setShowRoot(false);
		
		TreeTableColumn<AtomBlockEntry, String> column_file = new TreeTableColumn<>("File / FourCC");
		column_file.setPrefWidth(180);
		column_file.setCellValueFactory(p -> {
			return new ReadOnlyStringWrapper(p.getValue().getValue().title);
		});
		treeTableView.getColumns().add(column_file);
		
		TreeTableColumn<AtomBlockEntry, String> column_version = new TreeTableColumn<>("Version");
		column_version.setPrefWidth(70);
		column_version.setCellValueFactory(p -> {
			return new ReadOnlyStringWrapper(p.getValue().getValue().version);
		});
		treeTableView.getColumns().add(column_version);
		
		TreeTableColumn<AtomBlockEntry, String> column_start = new TreeTableColumn<>("Start block");
		column_start.setPrefWidth(100);
		column_start.setCellValueFactory(p -> {
			return new ReadOnlyStringWrapper(p.getValue().getValue().start_b);
		});
		treeTableView.getColumns().add(column_start);
		
		TreeTableColumn<AtomBlockEntry, String> column_start_p = new TreeTableColumn<>("Start payload");
		column_start_p.setPrefWidth(100);
		column_start_p.setCellValueFactory(p -> {
			return new ReadOnlyStringWrapper(p.getValue().getValue().start_p);
		});
		treeTableView.getColumns().add(column_start_p);
		
		TreeTableColumn<AtomBlockEntry, String> column_psize = new TreeTableColumn<>("Payload size");
		column_psize.setPrefWidth(100);
		column_psize.setCellValueFactory(p -> {
			return new ReadOnlyStringWrapper(p.getValue().getValue().p_size);
		});
		treeTableView.getColumns().add(column_psize);
		
		TreeTableColumn<AtomBlockEntry, String> column_end_p = new TreeTableColumn<>("End payload");
		column_end_p.setPrefWidth(100);
		column_end_p.setCellValueFactory(p -> {
			return new ReadOnlyStringWrapper(p.getValue().getValue().end_p);
		});
		treeTableView.getColumns().add(column_end_p);
		
		TreeTableColumn<AtomBlockEntry, String> column_end = new TreeTableColumn<>("End block");
		column_end.setPrefWidth(100);
		column_end.setCellValueFactory(p -> {
			return new ReadOnlyStringWrapper(p.getValue().getValue().end_b);
		});
		treeTableView.getColumns().add(column_end);
		
		MenuItem item_open_file = new MenuItem("Open file...");
		item_open_file.setOnAction(ev -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Open Resource File");
			File selected_file = fileChooser.showOpenDialog(stage);
			if (selected_file == null) {
				return;
			}
			
			try {
				loadFile(selected_file);
			} catch (IOException e) {
				log.error("Can't open file \"" + selected_file + "\"", e);
				displayException("Error during file opening", "Can't open file \"" + selected_file + "\"", e);
			}
		});
		
		MenuItem item_exit = new MenuItem("Exit");
		item_exit.setOnAction(ev -> {
			ev.consume();
			System.exit(0);
		});
		
		ContextMenu c_menu = new ContextMenu(item_open_file, item_exit);
		treeTableView.setContextMenu(c_menu);
		
		stage.show();
		
		/**
		 * Create test file
		 */
		if (getParameters().getUnnamed().isEmpty()) {
			File f = new File("mydmam-test-uiblockview.bin");
			if (f.exists() == false) {
				FileChannel channel = FileChannel.open(f.toPath(), MyDMAM.OPEN_OPTIONS_FILE_NOT_EXISTS);
				log.info("Create test file " + f.getAbsolutePath());
				
				Function<Integer, byte[]> makePayload = size -> {
					byte[] payload = new byte[size];
					ThreadLocalRandom.current().nextBytes(payload);
					return payload;
				};
				
				byte[] p0 = makePayload.apply(ThreadLocalRandom.current().nextInt(10, 1000));
				byte[] p1 = makePayload.apply(ThreadLocalRandom.current().nextInt(10, 1000));
				byte[] p2 = makePayload.apply(ThreadLocalRandom.current().nextInt(10, 1000));
				byte[] p3 = makePayload.apply(ThreadLocalRandom.current().nextInt(10, 1000));
				byte[] p4 = makePayload.apply(ThreadLocalRandom.current().nextInt(10, 1000));
				byte[] p5 = makePayload.apply(ThreadLocalRandom.current().nextInt(10, 1000));
				
				AtomBlock b0 = new AtomBlock(channel, 0l, p0.length, "Tst1", (short) 1).write(ByteBuffer.wrap(p0));
				
				AtomBlock b1 = b0.createNextInChannel(AtomBlock.computeSubBlocksSize(p1.length, p2.length, p3.length), "Tst2", (short) 2);
				AtomBlock b1_1 = b1.createSubBlock(p1.length, "T2s1", (short) 3).write(ByteBuffer.wrap(p1));
				AtomBlock b1_2 = b1_1.createNextInChannel(p2.length, "T2s2", (short) 4).write(ByteBuffer.wrap(p2));
				b1_2.createNextInChannel(p3.length, "T2s3", (short) 5).write(ByteBuffer.wrap(p3));
				
				AtomBlock b2 = b1.createNextInChannel(p4.length, "Tst3", (short) 6).write(ByteBuffer.wrap(p4));
				b2.createNextInChannel(p5.length, "Tst4", (short) 7).write(ByteBuffer.wrap(p5));
				
				channel.force(true);
				channel.close();
			}
			loadFile(f);
		} else {
			getParameters().getUnnamed().stream().forEach(f -> {
				try {
					loadFile(new File(f));
				} catch (IOException e) {
					log.error("Can't open file \"" + f + "\"", e);
					displayException("Error during file opening", "Can't open file \"" + f + "\"", e);
				}
			});
		}
	}
	
	private void displayException(String title, String header, Exception e) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle(title);
		alert.setHeaderText(header);
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String exceptionText = sw.toString();
		
		TextArea textArea = new TextArea(exceptionText);
		textArea.setEditable(false);
		textArea.setWrapText(true);
		
		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);
		
		alert.getDialogPane().setExpandableContent(textArea);
		alert.setContentText("Error: " + e.getMessage());
		alert.showAndWait();
	}
	
	private static class AtomBlockEntry {
		final String title;
		final String version;
		final String start_b;
		final String end_b;
		final String start_p;
		final String end_p;
		final String p_size;
		
		public AtomBlockEntry(File file) {
			title = file.getName();
			version = "";
			start_b = "";
			end_b = String.valueOf(file.length());
			start_p = "";
			end_p = "";
			p_size = "";
		}
		
		public AtomBlockEntry(AtomBlock block) {
			title = block.getFourCC();
			version = String.valueOf(block.getVersion());
			start_b = String.valueOf(block.getBlock_start_pos_in_file());
			end_b = String.valueOf(block.getBlock_end_pos_in_file());
			start_p = String.valueOf(block.getPayloadStart());
			end_p = String.valueOf(block.getPayloadEnd());
			p_size = String.valueOf(block.getPayloadSize());
		}
		
	}
	
	private static void atomBlockWalker(AtomBlock block, TreeItem<AtomBlockEntry> parent) throws IOException {
		TreeItem<AtomBlockEntry> current = new TreeItem<>(new AtomBlockEntry(block));
		
		Optional<AtomBlock> first_sub_block = block.parseSubBlocks().findFirst();
		if (first_sub_block.isPresent()) {
			atomBlockWalker(first_sub_block.get(), current);
		}
		current.setExpanded(true);
		parent.getChildren().add(current);
		
		AtomBlock next = block.getNextInChannel();
		if (next != null) {
			atomBlockWalker(next, parent);
		}
	}
	
	private void loadFile(File file) throws IOException {
		if (file.exists() == false) {
			throw new FileNotFoundException(file.getAbsolutePath());
		}
		FileChannel channel = FileChannel.open(file.toPath(), MyDMAM.OPEN_OPTIONS_FILE_EXISTS_READ_ONLY);
		log.info("Open file " + file.getAbsolutePath());
		
		TreeItem<AtomBlockEntry> file_parent = new TreeItem<>(new AtomBlockEntry(file));
		file_parent.setExpanded(true);
		
		AtomBlock first_block = new AtomBlock(channel, 0l);
		atomBlockWalker(first_block, file_parent);
		
		root_node.getChildren().add(file_parent);
		channel.close();
	}
	
}
