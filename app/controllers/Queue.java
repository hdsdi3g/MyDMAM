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
package controllers;

import hd3gtv.mydmam.taskqueue.Broker;
import hd3gtv.mydmam.taskqueue.TaskJobStatus;
import hd3gtv.mydmam.taskqueue.WorkerStatusChange;

import org.json.simple.JSONObject;

import play.data.validation.Required;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;

@With(Secure.class)
@SuppressWarnings("unchecked")
public class Queue extends Controller {
	
	@Check("showQueue")
	public static void index() {
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("queue.taskslist"));
		render();
	}
	
	@Check("showQueue")
	public static void workers() {
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("queue.workerlist"));
		render();
	}
	
	@Check("showQueue")
	public static void getall() throws Exception {
		JSONObject jo = new JSONObject();
		jo.put("tasksandjobs", Broker.getTasksAndJobs(0));
		jo.put("allendedjobs", Broker.getAllEndedJobs());
		jo.put("activetriggers", Broker.getActiveTriggerWorkers());
		jo.put("query", "all");
		jo.put("since", 0l);
		jo.put("result", true);
		renderJSON(jo.toJSONString());
	}
	
	@Check("showQueue")
	public static void getupdate(@Required long since) throws Exception {
		JSONObject jo = new JSONObject();
		jo.put("tasksandjobs", Broker.getTasksAndJobs(since));
		jo.put("counttasksandjobsstatus", Broker.getAllTasksAndJobsStatusCount());
		jo.put("endlife", Broker.getEndlifeJobs());
		jo.put("allendedjobs", Broker.getAllEndedJobs());
		jo.put("activetriggers", Broker.getActiveTriggerWorkers());
		jo.put("query", "tasksandjobs");
		jo.put("since", since);
		jo.put("result", true);
		renderJSON(jo.toJSONString());
	}
	
	@Check("showQueue")
	public static void getworkers() throws Exception {
		JSONObject jo = new JSONObject();
		jo.put("workers", Broker.getWorkers());
		jo.put("processingtasks", Broker.getProcessingTasks());
		jo.put("query", "workers");
		jo.put("result", true);
		renderJSON(jo.toJSONString());
	}
	
	/**
	 * Open for all users
	 */
	public static void gettasksjobs() throws Exception {
		String[] tasksjobs_keys = params.getAll("tasksjobs_keys[]");
		if (tasksjobs_keys == null) {
			renderJSON("{}");
			return;
		}
		if (tasksjobs_keys.length == 0) {
			renderJSON("{}");
			return;
		}
		renderJSON(Broker.getTasksAndJobsByKeys(tasksjobs_keys));
	}
	
	@Check("updateQueue")
	public static void changeworkerstate(@Required String worker_ref, @Required String newstate) throws Exception {
		JSONObject jo = new JSONObject();
		jo.put("query", "changeworkerstate");
		jo.put("result", Broker.changeWorkerState(worker_ref, WorkerStatusChange.fromString(newstate)));
		renderJSON(jo.toJSONString());
	}
	
	@Check("updateQueue")
	public static void changeworkercyclicperiod(@Required String worker_ref, @Required int period) throws Exception {
		JSONObject jo = new JSONObject();
		jo.put("query", "changeworkercyclicperiod");
		jo.put("result", Broker.changeWorkerCyclicPeriod(worker_ref, period));
		renderJSON(jo.toJSONString());
	}
	
	@Check("updateQueue")
	public static void changetaskstatus(@Required String task_key, @Required String status) throws Exception {
		JSONObject jo = new JSONObject();
		jo.put("query", "changetaskstatus");
		TaskJobStatus selected_status = TaskJobStatus.fromString(status);
		if (selected_status == TaskJobStatus.WAITING) {
			jo.put("result", Broker.changeTaskStatus(task_key, selected_status));
		} else if (selected_status == TaskJobStatus.POSTPONED) {
			jo.put("result", Broker.changeTaskStatus(task_key, selected_status));
		} else if (selected_status == TaskJobStatus.CANCELED) {
			jo.put("result", Broker.changeTaskStatus(task_key, selected_status));
		} else {
			jo.put("result", false);
		}
		renderJSON(jo.toJSONString());
	}
	
	@Check("updateQueue")
	public static void changetaskpriority(@Required String task_key, @Required int priority) throws Exception {
		JSONObject jo = new JSONObject();
		jo.put("query", "changetaskpriority");
		jo.put("result", Broker.changeTaskPriority(task_key, priority));
		renderJSON(jo.toJSONString());
	}
	
	@Check("updateQueue")
	public static void changetaskmaxage(@Required String task_key, @Required long date_max_age) throws Exception {
		JSONObject jo = new JSONObject();
		jo.put("query", "changetaskmaxage");
		if (date_max_age == 0) {
			date_max_age = Long.MAX_VALUE;
		}
		jo.put("result", Broker.changeTaskMaxAge(task_key, date_max_age));
		renderJSON(jo.toJSONString());
	}
	
}
