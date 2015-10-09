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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.manager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JOptionPane;

import hd3gtv.mydmam.Loggers;

class UIActionListener implements ActionListener, WindowListener, MouseListener {
	
	private UIFrame frame;
	
	UIActionListener(UIFrame frame) {
		this.frame = frame;
		if (frame == null) {
			throw new NullPointerException("\"frame\" can't to be null");
		}
	}
	
	@Override
	public void windowOpened(WindowEvent e) {
	}
	
	@Override
	public void windowIconified(WindowEvent e) {
	}
	
	@Override
	public void windowDeiconified(WindowEvent e) {
	}
	
	@Override
	public void windowActivated(WindowEvent e) {
	}
	
	@Override
	public void windowDeactivated(WindowEvent e) {
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == frame.bt_quit) {
			windowClosing(null);
		}
	}
	
	@Override
	public void windowClosing(WindowEvent e) {
		int confirm = JOptionPane.showConfirmDialog(frame.getOwner(), "Do you really want close the application ?", frame.getTitle(), JOptionPane.YES_NO_OPTION);
		if (confirm != JOptionPane.YES_OPTION) {
			return;
		}
		
		if (e == null) {
			frame.dispose();
		} else {
			e.getWindow().dispose();
		}
	}
	
	public void windowClosed(WindowEvent e) {
		Loggers.Manager.info("User close application");
		frame.manager.stopAll();
		System.exit(0);
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		// frame.setTitle(e.getPoint().x + " " + e.getPoint().y);
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
	}
	
	@Override
	public void mouseEntered(MouseEvent e) {
	}
	
	@Override
	public void mouseExited(MouseEvent e) {
	}
	
}
