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
package hd3gtv.mydmam.assetsxcross;

import java.util.Collection;

import hd3gtv.mydmam.assetsxcross.RestoreInterplayVantageAC.ManageableAsset;
import hd3gtv.mydmam.assetsxcross.RestoreInterplayVantageAC.ManageableAsset.ArchivedAsset;
import hd3gtv.tools.TableList;
import hd3gtv.tools.TableList.Row;

public class RestoreJob {
	
	/**
	 * Can be null
	 */
	private InterplayAsset base_asset;
	/**
	 * Can be null
	 */
	private Runnable onDone;
	private Collection<ManageableAsset> manageable_assets;
	
	RestoreJob(InterplayAsset base_asset, Collection<ManageableAsset> manageable_assets) {
		this.base_asset = base_asset;
		this.manageable_assets = manageable_assets;
	}
	
	RestoreJob(Collection<ManageableAsset> manageable_assets) {
		this.manageable_assets = manageable_assets;
	}
	
	RestoreJob setOnDone(Runnable onDone) {
		this.onDone = onDone;
		return this;
	}
	
	public void globalStatus(TableList table) {
		final Row row_base = table.createRow();
		if (base_asset != null) {
			row_base.addCell(base_asset.getType().getShortName());
			if (base_asset.getMyDMAMID() != null) {
				row_base.addCell(base_asset.getMyDMAMID());
			} else {
				row_base.addEmptyCell();
			}
			row_base.addCell(base_asset.getDisplayName().substring(0, Math.min(base_asset.getDisplayName().length(), 15)));
			if (base_asset.isOnline()) {
				row_base.addCell("online");
				return;
			}
		}
		
		manageable_assets.stream().forEach(m_asset -> {
			Row row_line = row_base;
			
			boolean show_col = false;
			if (base_asset != null) {
				show_col = base_asset.interplay_uri != m_asset.getAsset().interplay_uri;
			}
			
			if (show_col) {
				row_line = table.createRow();
				
				if (m_asset.getAsset().isOnline()) {
					row_line.addCell("  o");
				} else if (m_asset.getLocalizedArchivedVersion() != null) {
					if (m_asset.getLocalizedArchivedVersion().getVantageJob() != null) {
						row_line.addCell("  V");
					} else if (m_asset.getLocalizedArchivedVersion().getDestageJob() != null) {
						row_line.addCell("  D");
					} else {
						row_line.addCell("  !");
					}
				} else {
					row_line.addCell("  X");
				}
				
				if (m_asset.getAsset().getMyDMAMID() != null) {
					row_line.addCell(m_asset.getAsset().getMyDMAMID());
				} else {
					row_line.addEmptyCell();
				}
				row_line.addCell(m_asset.getAsset().getDisplayName().substring(0, Math.min(m_asset.getAsset().getDisplayName().length(), 15)));
			}
			
			if (m_asset.getAsset().isOnline()) {
				row_line.addCell("Online");
			} else if (m_asset.getLocalizedArchivedVersion() == null) {
				row_line.addCell("No localized version: can't restore this clip");
			} else if (m_asset.getLocalizedArchivedVersion().getVantageJob() != null) {
				row_line.addCell("Vantage: " + m_asset.getLocalizedArchivedVersion().getVantageJob().toString());
			} else if (m_asset.getLocalizedArchivedVersion().getDestageJob() != null) {
				row_line.addCell(m_asset.getLocalizedArchivedVersion().getDestageJob().toString());
			} else {
				row_line.addCell("No action pending for localized version !");
			}
		});
	}
	
	public boolean isDone() {
		boolean is_done = false;
		
		if (base_asset != null) {
			is_done = base_asset.isOnline();
		}
		
		if (is_done == false) {
			is_done = manageable_assets.stream().allMatch(m_asset -> {
				if (m_asset.getAsset().isOnline()) {
					return true;
				}
				if (m_asset.getLocalizedArchivedVersion() != null) {
					ArchivedAsset arch = m_asset.getLocalizedArchivedVersion();
					if (arch.getVantageJob() != null) {
						return true;
					}
				}
				return false;
			});
		}
		
		if (is_done & onDone != null) {
			onDone.run();
		}
		return is_done;
	}
	
}
