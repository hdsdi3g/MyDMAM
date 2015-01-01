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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.mydmam.manager;

import hd3gtv.configuration.GitInfo;
import hd3gtv.log2.Log2;

import java.awt.Button;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Label;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;

import javax.swing.ImageIcon;

class UIFrame extends Frame {
	
	Button bt_quit;
	
	private UIActionListener actionlistener;
	AppManager manager;
	private ThreadStatusCheck t_statuscheck;
	
	UIFrame(String title, AppManager manager) {
		super();
		
		if (title == null) {
			throw new NullPointerException("\"title\" can't to be null");
		}
		this.manager = manager;
		if (manager == null) {
			throw new NullPointerException("\"manager\" can't to be null");
		}
		
		actionlistener = new UIActionListener(this);
		setTitle(title);
		setSize(300, 200);
		addWindowListener(actionlistener);
		addMouseListener(actionlistener);
		setLayout(null);
		
		bt_quit = new Button("Quitter");
		bt_quit.setBounds(new Rectangle(40, 120, 80, 35));
		bt_quit.addActionListener(actionlistener);
		add(bt_quit);
		
		Label lbl_about = new Label(title);
		lbl_about.setBounds(new Rectangle(40, 50, 250, 30));
		add(lbl_about);
		
		GitInfo git = GitInfo.getFromRoot();
		if (git != null) {
			Label lbl_version = new Label("Branch: " + git.getBranch() + ", commit: " + git.getCommit());
			lbl_version.setBounds(new Rectangle(40, 80, 250, 30));
			add(lbl_version);
		}
	}
	
	public void display() {
		setLocationRelativeTo(null);
		setResizable(false);
		try {
			setIconImage(new ImageIcon("public/img/app.gif").getImage());
			/*TrayIcon Window*/
		} catch (Exception e) {
			Log2.log.error("Can't load icon", e);
		}
		setVisible(true);
		
		t_statuscheck = new ThreadStatusCheck();
		t_statuscheck.start();
	}
	
	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2 = (Graphics2D) g;
		
		if (manager.isWorkingToShowUIStatus()) {
			g2.setColor(new Color(0, 255, 0));
		} else {
			g2.setColor(new Color(220, 220, 225));
		}
		Ellipse2D led_processing = new Ellipse2D.Double(150, 130, 15, 15);
		g2.fill(led_processing);
	}
	
	private class ThreadStatusCheck extends Thread {
		
		public ThreadStatusCheck() {
			setName("UIFrame StatusCheck");
			setDaemon(true);
		}
		
		public void run() {
			try {
				while (true) {
					repaint();
					sleep(500);
				}
			} catch (InterruptedException e) {
				Log2.log.error("UIFrame status check sleep problem", e);
			}
		}
	}
	
}
