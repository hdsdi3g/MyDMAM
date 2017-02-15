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

import java.util.ArrayList;

public class TableList {
	
	private ArrayList<String[]> table;
	private int[] tableSizes;
	private int rows;
	
	public TableList(int columns) {
		this.rows = columns;
		this.table = new ArrayList<>();
		this.tableSizes = new int[columns];
	}
	
	private void updateSizes(String[] elements) {
		for (int i = 0; i < tableSizes.length; i++) {
			if (elements[i] != null) {
				int j = tableSizes[i];
				j = Math.max(j, elements[i].length());
				tableSizes[i] = j;
			}
		}
	}
	
	/**
	 * Adds a row to the table with the specified elements.
	 */
	
	public TableList addRow(String... elements) {
		if (elements.length != rows) {
			throw new IllegalArgumentException();
		}
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] == null) {
				throw new NullPointerException("Row element[" + i + "] can't to be null");
			}
		}
		table.add(elements);
		updateSizes(elements);
		return this;
	}
	
	public void print() {
		StringBuilder line = null;
		
		line = null;
		ArrayList<String[]> localTable = table;
		
		if (localTable.isEmpty()) {
			String[] sa = new String[rows];
			localTable.add(sa);
		}
		
		localTable.forEach(arr -> {
			for (int i = 0; i < arr.length; i++) {
				if (arr[i] == null) {
					arr[i] = "";
				}
			}
		});
		
		for (String[] strings : localTable) {
			for (int i = 0; i < rows; i++) {
				if (line == null) {
					line = new StringBuilder();
				}
				String part = "";
				if (strings[i] != null) {
					part += strings[i];
				}
				while (part.length() < tableSizes[i] + 1) {
					part += " ";
				}
				for (int j = 0; j < 1; j++) {
					part += " ";
				}
				line.append(part);
			}
			System.out.println(line.toString());
			
			line = null;
		}
	}
	
}