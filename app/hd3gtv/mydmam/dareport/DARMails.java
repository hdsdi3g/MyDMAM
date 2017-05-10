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
package hd3gtv.mydmam.dareport;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.mail.internet.InternetAddress;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.auth.AuthTurret;
import hd3gtv.mydmam.auth.UserNG;
import hd3gtv.mydmam.mail.EndUserBaseMail;

public class DARMails {
	
	private HashMap<DAREvent, List<DARReport>> reports_by_events;
	
	public DARMails() {
		reports_by_events = new HashMap<>();
	}
	
	/**
	 * Accumulate all reports for this day to one summary
	 */
	public void sendDailyReports() throws Exception {
		Loggers.DAReport.debug("Send daily mails");
		
		reports_by_events.clear();
		
		/**
		 * Get yesterday
		 */
		long start_bounded_date = DARDB.get().getYesterdayStartOfTime();
		long end_bounded_date = DARDB.get().getYesterdayEndOfTime();
		
		/**
		 * Get all events for day
		 */
		ArrayList<DAREvent> events_for_yesterday = DAREvent.datesBoundedList(start_bounded_date, end_bounded_date);
		if (events_for_yesterday.isEmpty()) {
			Loggers.DAReport.info("No events recorded for yesterday");
			return;
		}
		
		HashMap<String, DAREvent> yesterday_events_by_events_names = new HashMap<>();
		events_for_yesterday.forEach(event -> {
			yesterday_events_by_events_names.put(event.name, event);
		});
		
		/**
		 * Get all reports for each events
		 */
		LinkedHashMap<String, ArrayList<DARReport>> yesterday_reports_by_events_names = DARReport.listByEventsname(yesterday_events_by_events_names.keySet());
		if (yesterday_reports_by_events_names.isEmpty()) {
			Loggers.DAReport.info("Nobody has created reports for all events recorded yesterday. Events: " + yesterday_events_by_events_names.keySet().stream().collect(Collectors.joining(", ")));
			return;
		}
		
		final ArrayList<String> events_that_not_had_reports = new ArrayList<>(1);
		yesterday_reports_by_events_names.forEach((event_name, report_list_for_event) -> {
			if (report_list_for_event.isEmpty()) {
				events_that_not_had_reports.add(event_name);
			} else {
				reports_by_events.put(yesterday_events_by_events_names.get(event_name), report_list_for_event);
			}
		});
		if (events_that_not_had_reports.isEmpty()) {
			Loggers.DAReport.info("Nobody has created reports for some events recorded yesterday. Events: " + events_that_not_had_reports.stream().collect(Collectors.joining(", ")));
		}
		
		LinkedHashMap<DAREvent, List<DARReport>> yesterday_sorted_events = superSort();
		
		DARDB dardb = DARDB.get();
		ArrayList<InternetAddress> managers_addrs = dardb.getManagerAddrs();
		
		boolean done = sendMail(yesterday_sorted_events, managers_addrs, dardb.getMailLocale(), "daily");
		if (done == false) {
			return;
		}
		
		/**
		 * Get reports users mail from all reports from all events. Check if the mail addr exists and is valid.
		 */
		ArrayList<String> no_mails_user_list = new ArrayList<>();
		AuthTurret auth = MyDMAM.getPlayBootstrapper().getAuth();
		
		List<UserNG> all_valid_report_authors = yesterday_sorted_events.values().stream().map(report_list -> {
			return report_list.stream().map(report -> {
				return report.account_user_key;
			}).collect(Collectors.toList());
		}).flatMap((report_user_key_list) -> {
			return report_user_key_list.stream();
		}).distinct().map(user_key -> {
			return auth.getByUserKey(user_key);
		}).filter(user -> {
			if (user == null) {
				return false;
			}
			InternetAddress addr = user.getInternetAddress();
			if (addr == null) {
				/**
				 * It can't send a mail with no email addr.
				 */
				String full_name = user.getFullname();
				if (full_name == null) {
					no_mails_user_list.add(user.getName());
				} else {
					no_mails_user_list.add(full_name);
				}
				return false;
			}
			if (managers_addrs.contains(addr)) {
				/**
				 * Managers don't needs to recevie own reports, because they already receive all reports.
				 */
				return false;
			}
			return true;
		}).collect(Collectors.toList());
		
		if (no_mails_user_list.isEmpty() == false) {
			Loggers.DAReport.info("Some users has not set correctly their email address in database. No reports will be send for they. Users: " + no_mails_user_list.stream().collect(Collectors.joining(", ")));
		}
		
		/**
		 * Foreach user, get all events > its reports, with no empty events.
		 */
		Consumer<UserNG> action_prepare_send_user_mail = user -> {
			LinkedHashMap<DAREvent, List<DARReport>> user_events = new LinkedHashMap<>();
			
			yesterday_sorted_events.forEach((event, report_list) -> {
				/**
				 * Add only event if user add a report on it
				 */
				report_list.stream().filter(report -> {
					return user.getKey().equals(report.account_user_key);
				}).findFirst().ifPresent(report -> {
					user_events.put(event, Arrays.asList(report));
				});
			});
			
			try {
				sendMail(user_events, Arrays.asList(user.getInternetAddress()), user.getLocale(), "user-" + user.getName());
			} catch (FileNotFoundException e) {
				Loggers.DAReport.error("Can't found mail template", e);
			}
		};
		
		all_valid_report_authors.stream().forEach(action_prepare_send_user_mail);
	}
	
	private LinkedHashMap<DAREvent, List<DARReport>> superSort() {
		LinkedHashMap<DAREvent, List<DARReport>> result = new LinkedHashMap<>();
		
		ArrayList<DAREvent> all_events = new ArrayList<>(reports_by_events.keySet());
		DAREvent.sortEvents(all_events);
		
		all_events.forEach(event -> {
			result.put(event, DARReport.sortDARReport(reports_by_events.get(event)));
		});
		
		return result;
	}
	
	public void sendReportForAdmin(UserNG user, DAREvent event) throws Exception {
		Loggers.DAReport.info("Send by mail all reports for event \"" + event.name + "\" to " + user.getFullname());
		
		reports_by_events.put(event, DARReport.listByEventname(event.name));
		
		LinkedHashMap<DAREvent, List<DARReport>> sorted_reports = superSort();
		
		sendMail(sorted_reports, Arrays.asList(user.getInternetAddress()), user.getLocale(), "admin-" + user.getName());
	}
	
	public final static String formatDate(long date, Locale locale) {
		DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, locale);
		return df.format(new Date(date));
	}
	
	public final static String formatTime(long date, Locale locale) {
		return new SimpleDateFormat("HH:mm").format(new Date(date));
	}
	
	public final static String nl2br(String value) {
		return value.replaceAll("\r\n", "\n").replaceAll("\n", "<br>");
	}
	
	private boolean sendMail(LinkedHashMap<DAREvent, List<DARReport>> events, List<InternetAddress> to, Locale locale, String mail_type) throws FileNotFoundException {
		if (events.isEmpty()) {
			throw new IndexOutOfBoundsException("No events to display !");
		}
		InternetAddress[] _to = new InternetAddress[to.size()];
		_to = to.toArray(_to);
		
		EndUserBaseMail mail = new EndUserBaseMail(locale, "dailyactivityreport", _to);
		HashMap<String, Object> mail_vars = new HashMap<String, Object>();
		mail_vars.put("events", events);
		mail_vars.put("date_now", events.keySet().stream().findFirst().get().planned_date);
		mail_vars.put("locale", locale);
		
		// TODO test with multiple reports
		
		Loggers.DAReport.info("Dump mail \"events\" var content " + MyDMAM.gson_kit.getGsonPretty().toJson(events));// TODO (after debug) set to trace
		mail.setDumpMailContentToFile(new File("darmail_" + mail_type + ".htm"));// TODO (after debug) remove
		
		return mail.send(mail_vars);
	}
	
}
