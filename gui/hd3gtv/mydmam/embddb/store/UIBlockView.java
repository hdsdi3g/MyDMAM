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

import org.apache.log4j.Logger;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableView;
import javafx.stage.Stage;

public class UIBlockView extends Application {
	private static Logger log = Logger.getLogger(UIBlockView.class);
	
	public static void main(String[] args) {
		Application.launch(args);
	}
	
	/**
	 * See https://docs.oracle.com/javase/8/javafx/user-interface-tutorial/tree-table-view.htm
	 */
	public void start(Stage stage) throws Exception {
		stage.setTitle("Tree Table View Samples");
		final Scene scene = new Scene(new Group(), 400, 400);
		Group sceneRoot = (Group) scene.getRoot();
		
		final TreeItem<String> childNode1 = new TreeItem<>("Child Node 1");
		final TreeItem<String> childNode2 = new TreeItem<>("Child Node 2");
		final TreeItem<String> childNode3 = new TreeItem<>("Child Node 3");
		
		final TreeItem<String> sub_root = new TreeItem<>("Sub root");
		sub_root.getChildren().add(childNode1);
		sub_root.getChildren().add(childNode2);
		sub_root.getChildren().add(childNode3);
		sub_root.setExpanded(true);
		
		final TreeItem<String> root = new TreeItem<>("Root node");
		root.setExpanded(true);
		root.getChildren().add(new TreeItem<>("Child Node 0"));
		root.getChildren().add(sub_root);
		root.getChildren().add(new TreeItem<>("Child Node 4"));
		
		TreeTableColumn<String, String> column = new TreeTableColumn<>("Column");
		column.setPrefWidth(200);
		column.setCellValueFactory((CellDataFeatures<String, String> p) -> new ReadOnlyStringWrapper(p.getValue().getValue()));
		
		// Creating a tree table view
		final TreeTableView<String> treeTableView = new TreeTableView<>(root);
		treeTableView.getColumns().add(column);
		// treeTableView.setPrefWidth(200);
		treeTableView.setShowRoot(true);
		sceneRoot.getChildren().add(treeTableView);
		stage.setScene(scene);
		stage.show();
	}
	
}
