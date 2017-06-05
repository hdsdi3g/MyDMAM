/*
 * This file is part of modifiled TableList MRebhan/crogamp file
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * https://github.com/MRebhan/crogamp/blob/master/src/com/github/mrebhan/crogamp/cli/TableList.java
 * 
*/
package hd3gtv.tools;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;

public class TableList {
	
	private ArrayList<Row> table;
	private int max_cols;
	
	public TableList() {
		table = new ArrayList<>(1);
		max_cols = 1;
	}
	
	public class Row {
		private ArrayList<String> cells;
		
		private Row() {
			cells = new ArrayList<>(max_cols);
		}
		
		/**
		 * @param content can be null
		 */
		public Row addCell(String content) {
			cells.add(StringUtils.defaultString(content, "null"));
			max_cols = Math.max(max_cols, cells.size());
			return this;
		}
		
		/**
		 * @param content can be null
		 */
		public Row addBoolean(boolean value, String if_true, String if_false) {
			if (value) {
				cells.add(StringUtils.defaultString(if_true, "null"));
			} else {
				cells.add(StringUtils.defaultString(if_false, "null"));
			}
			max_cols = Math.max(max_cols, cells.size());
			return this;
		}
		
		/**
		 * @param content can be null
		 */
		public Row addCells(String... content) {
			if (content == null) {
				return this;
			}
			if (content.length == 0) {
				return this;
			}
			for (int pos = 0; pos < content.length; pos++) {
				addCell(content[pos]);
			}
			
			return this;
		}
		
		/**
		 * @param content can be null
		 */
		public Row addCells(Collection<String> content) {
			if (content == null) {
				return this;
			}
			if (content.isEmpty()) {
				return this;
			}
			content.forEach(cell -> {
				addCell(cell);
			});
			
			return this;
		}
		
		public Row addEmptyCell() {
			addCell("");
			return this;
		}
		
		public int getCellCount() {
			return cells.size();
		}
		
		private void print(PrintWriter out, ArrayList<Integer> cols_sizes) {
			for (int pos = 0; pos < cells.size(); pos++) {
				out.print(StringUtils.rightPad(cells.get(pos), cols_sizes.get(pos) + 2));
			}
			
			if (cells.size() < cols_sizes.size()) {
				for (int pos = cells.size(); pos < cols_sizes.size(); pos++) {
					out.print(StringUtils.rightPad("", cols_sizes.get(pos) + 2));
				}
			}
			
			out.println();
		}
	}
	
	public int size() {
		return table.size();
	}
	
	public Row createRow() {
		Row r = new Row();
		table.add(r);
		return r;
	}
	
	public TableList addRow(String... elements) {
		createRow().addCells(elements);
		return this;
	}
	
	public TableList addSimpleCellRow(String content) {
		createRow().addCell(content);
		return this;
	}
	
	public void print(PrintWriter out) {
		if (table.isEmpty()) {
			return;
		}
		
		final ArrayList<Integer> cols_sizes = new ArrayList<>(max_cols);
		for (int pos = 0; pos < max_cols; pos++) {
			cols_sizes.add(0);
		}
		
		table.forEach(row -> {
			ArrayList<String> row_cells = row.cells;
			for (int pos = 0; pos < row_cells.size(); pos++) {
				int string_len = row_cells.get(pos).length();
				if (cols_sizes.get(pos) < string_len) {
					cols_sizes.set(pos, string_len);
				}
			}
		});
		
		table.forEach(row -> {
			row.print(out, cols_sizes);
		});
		
		out.flush();
	}
	
	public void print(PrintStream out) {
		print(new PrintWriter(out));
	}
	
	/**
	 * To System.out
	 */
	public void print() {
		print(System.out);
		
		if (size() > 0) {
			System.out.println();
		}
	}
	
}