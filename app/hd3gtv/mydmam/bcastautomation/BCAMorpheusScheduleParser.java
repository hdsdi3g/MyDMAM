/*
 * This file is part of MyDMAM
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.bcastautomation;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Properties;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import hd3gtv.mydmam.Loggers;

/**
 * Import Schedule/AsRun xml file.
 */
class BCAMorpheusScheduleParser extends DefaultHandler implements ErrorHandler {
	
	static final int IMAGE_DURATION = 40;
	private BCAMorpheus bca;
	
	BCAMorpheusScheduleParser(BCAMorpheus bca, File schfile) throws IOException {
		this.bca = bca;
		
		try {
			SAXParserFactory fabrique = SAXParserFactory.newInstance();
			SAXParser parseur = fabrique.newSAXParser();
			InputStream fis = new BufferedInputStream(new FileInputStream(schfile), 8192);
			InputSource is = new InputSource(fis);
			parseur.parse(is, this);
			parseur = null;
			fis.close();
		} catch (ParserConfigurationException pce) {
			throw new IOException(pce);
		} catch (SAXException se) {
			throw new IOException(se);
		}
	}
	
	public void error(SAXParseException e) throws SAXException {
		Loggers.BroadcastAutomation.error("Schedule XML Parsing error", e);
	}
	
	public void fatalError(SAXParseException e) throws SAXException {
		Loggers.BroadcastAutomation.error("Schedule XML Parsing error", e);
	}
	
	public void warning(SAXParseException e) throws SAXException {
		Loggers.BroadcastAutomation.warn("Schedule XML Parsing warning", e);
	}
	
	/**
	 * Convert "09-OCT-2012 13:30:15:20" (GMT) to an Unix time (at local time at this date)
	 */
	public static long convertDate(String schdate) {
		String sch_day = schdate.substring(0, 2);
		String sch_month = schdate.substring(3, 6);
		String sch_year = schdate.substring(7, 11);
		String sch_hour = schdate.substring(12, 14);
		String sch_minutes = schdate.substring(15, 17);
		String sch_seconds = schdate.substring(18, 20);
		String sch_images = schdate.substring(21, 23);
		
		int year = Integer.parseInt(sch_year);
		int month = -1;
		if (sch_month.equals("JAN")) {
			month = 0;
		} else if (sch_month.equals("FEB")) {
			month = 1;
		} else if (sch_month.equals("MAR")) {
			month = 2;
		} else if (sch_month.equals("APR")) {
			month = 3;
		} else if (sch_month.equals("MAY")) {
			month = 4;
		} else if (sch_month.equals("JUN")) {
			month = 5;
		} else if (sch_month.equals("JUL")) {
			month = 6;
		} else if (sch_month.equals("AUG")) {
			month = 7;
		} else if (sch_month.equals("SEP")) {
			month = 8;
		} else if (sch_month.equals("OCT")) {
			month = 9;
		} else if (sch_month.equals("NOV")) {
			month = 10;
		} else if (sch_month.equals("DEC")) {
			month = 11;
		}
		int day = Integer.parseInt(sch_day);
		int hour = Integer.parseInt(sch_hour);
		int minute = Integer.parseInt(sch_minutes);
		int second = Integer.parseInt(sch_seconds);
		int images = Integer.parseInt(sch_images) * IMAGE_DURATION;
		
		Calendar distantdate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		distantdate.set(year, month, day, hour, minute, second);
		distantdate.set(Calendar.MILLISECOND, images);
		
		return distantdate.getTimeInMillis();
	}
	
	private HashMap<Integer, BCAMorpheusScheduleParserEvent> events;
	private String schedulename;
	private String channel;
	private long schedulenotionalstarttime;
	
	private BCAMorpheusScheduleParserEvent currentevent;
	private StringBuffer rawtext;
	
	private int firsteventuid = 0;
	
	public void startDocument() throws SAXException {
		events = new HashMap<Integer, BCAMorpheusScheduleParserEvent>();
		rawtext = new StringBuffer();
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		String read = new String(ch, start, length);
		if (read.trim().length() > 0) {
			rawtext.append(read.trim());
		}
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (qName.equalsIgnoreCase("PreviousUid") | qName.equalsIgnoreCase("OwnerUid") | qName.equalsIgnoreCase("IsFixed") | qName.equalsIgnoreCase("EventKind") | qName.equalsIgnoreCase("IsMediaBall")
				| qName.equalsIgnoreCase("IsUpStreamEvent") | qName.equalsIgnoreCase("IsBackupMixerEvent") | qName.equalsIgnoreCase("IsGuardEvent") | qName.equalsIgnoreCase("IsUpStreamGuardEvent")) {
			rawtext = new StringBuffer();
			return;
		}
		if (qName.equalsIgnoreCase("Parameter")) {
			this.currentevent.fields.put(attributes.getValue("Name"), attributes.getValue("Value"));
			return;
		}
		if (qName.equalsIgnoreCase("MasterSOM")) {
			return;
		}
		if (qName.equalsIgnoreCase("PlayoutDeviceSOM")) {
			return;
		}
		if (qName.equalsIgnoreCase("ActualInpoint")) {
			return;
		}
		
		if (qName.equalsIgnoreCase("Fields")) {
			this.currentevent.fields = new Properties();
			return;
		}
		
		if (qName.equalsIgnoreCase("Event")) {
			this.currentevent = new BCAMorpheusScheduleParserEvent(Integer.parseInt(attributes.getValue("Uid")), attributes.getValue("FullyQualifiedType"), bca);
			if (firsteventuid == 0) {
				firsteventuid = Integer.parseInt(attributes.getValue("Uid"));
			}
			if (attributes.getValue("NotionalStartTime") != null) {
				currentevent.notionalstarttime = convertDate(attributes.getValue("NotionalStartTime"));
			}
			currentevent.notionalduration = attributes.getValue("NotionalDuration");
			return;
		}
		
		if (qName.equalsIgnoreCase("Events")) {
			this.channel = attributes.getValue("Channel");
			if (attributes.getValue("NotionalStartTime") != null) {
				this.schedulenotionalstarttime = convertDate(attributes.getValue("NotionalStartTime"));
			}
			return;
		}
		
		if (qName.equalsIgnoreCase("Schedule")) {
			this.schedulename = attributes.getValue("Name");
			return;
		}
		
		if (qName.equalsIgnoreCase("ScheduleName")) {
			return;
		}
		if (qName.equalsIgnoreCase("ScheduleInformationList")) {
			return;
		}
		if (qName.equalsIgnoreCase("ScheduleInformation")) {
			return;
		}
		if (qName.equalsIgnoreCase("ScheduleExternalId")) {
			return;
		}
		if (qName.equalsIgnoreCase("IsInvalid")) {
			return;
		}
		
		Loggers.BroadcastAutomation.debug("Unknow start qName: " + qName);
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase("PreviousUid")) {
			this.currentevent.previousuid = Integer.parseInt(this.rawtext.toString());
			return;
		}
		if (qName.equalsIgnoreCase("OwnerUid")) {
			this.currentevent.owneruid = Integer.parseInt(this.rawtext.toString());
			return;
		}
		if (qName.equalsIgnoreCase("IsFixed")) {
			this.currentevent.isfixed = this.rawtext.toString().equalsIgnoreCase("True");
			return;
		}
		if (qName.equalsIgnoreCase("IsMediaBall")) {
			this.currentevent.ismediaball = this.rawtext.toString().equalsIgnoreCase("True");
			return;
		}
		if (qName.equalsIgnoreCase("IsUpStreamEvent")) {
			this.currentevent.isupstreamevent = this.rawtext.toString().equalsIgnoreCase("True");
			return;
		}
		if (qName.equalsIgnoreCase("IsBackupMixerEvent")) {
			this.currentevent.isbackupmixerevent = this.rawtext.toString().equalsIgnoreCase("True");
			return;
		}
		if (qName.equalsIgnoreCase("IsGuardEvent")) {
			this.currentevent.isguardevent = this.rawtext.toString().equalsIgnoreCase("True");
			return;
		}
		if (qName.equalsIgnoreCase("IsUpStreamGuardEvent")) {
			this.currentevent.isupstreamguardevent = this.rawtext.toString().equalsIgnoreCase("True");
			return;
		}
		if (qName.equalsIgnoreCase("EventKind")) {
			this.currentevent.eventkind = this.rawtext.toString();
			return;
		}
		if (qName.equalsIgnoreCase("Parameter") | qName.equalsIgnoreCase("Fields") | qName.equalsIgnoreCase("MasterSOM") | qName.equalsIgnoreCase("PlayoutDeviceSOM")
				| qName.equalsIgnoreCase("ActualInpoint")) {
			return;
		}
		
		if (qName.equalsIgnoreCase("Event")) {
			this.events.put(this.currentevent.uid, this.currentevent);
			this.currentevent = null;
			return;
		}
		
		if (qName.equalsIgnoreCase("Events")) {
			return;
		}
		if (qName.equalsIgnoreCase("Schedule")) {
			return;
		}
		
		if (qName.equalsIgnoreCase("ScheduleName")) {
			return;
		}
		if (qName.equalsIgnoreCase("ScheduleInformationList")) {
			return;
		}
		if (qName.equalsIgnoreCase("ScheduleInformation")) {
			return;
		}
		if (qName.equalsIgnoreCase("ScheduleExternalId")) {
			return;
		}
		if (qName.equalsIgnoreCase("IsInvalid")) {
			return;
		}
		
		Loggers.BroadcastAutomation.debug("Unknow end qName: " + qName);
	}
	
	public String getChannel() {
		return channel;
	}
	
	public String getSchedulename() {
		return schedulename;
	}
	
	public long getSchedulenotionalstarttime() {
		return schedulenotionalstarttime;
	}
	
	private BCAMorpheusScheduleParserEvent getParentEvent(BCAMorpheusScheduleParserEvent event) {
		if (event.owneruid == -1) {
			return event;
		} else {
			return getParentEvent(events.get(event.owneruid));
		}
	}
	
	public ArrayList<BCAMorpheusScheduleParserEvent> getEvents() {
		/**
		 * The order is very important: it converts to a list that does not mix.
		 */
		ArrayList<BCAMorpheusScheduleParserEvent> al_events = new ArrayList<BCAMorpheusScheduleParserEvent>(events.size() / 4); // environ
		
		BCAMorpheusScheduleParserEvent event;
		BCAMorpheusScheduleParserEvent parentevent;
		for (int pos = firsteventuid; pos < (firsteventuid + events.size()); pos++) {
			/**
			 * For all events.
			 */
			event = events.get(pos);
			/**
			 * It look for sub events, it add them to the main events
			 */
			if (event.owneruid == -1) {
				al_events.add(event);
			} else {
				parentevent = getParentEvent(event);
				if (parentevent.events == null) {
					parentevent.events = new HashMap<>(1);
				}
				parentevent.events.put(event.uid, event);
			}
		}
		return al_events;
	}
}
