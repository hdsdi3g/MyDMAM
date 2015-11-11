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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
watchfolders.AbstractFoundedFile =  React.createClass({
	btnDelete: function(e) {
		e.preventDefault();
		this.props.onDelete(this.props.abstract_founded_file);
	},
	render: function() {
		var abstract_founded_file = this.props.abstract_founded_file;
		var jobs = this.props.jobs;

		var tr_classes = classNames({
		    'error': abstract_founded_file.status === "ERROR",
		    'warning': abstract_founded_file.status === "IN_PROCESSING",
		    'info': abstract_founded_file.status === "DETECTED",
		    /*'success': abstract_founded_file.status === "PROCESSED",*/
		});

		var linked_jobs = [];
		for (job_key in abstract_founded_file.map_job_target) {
			if (jobs[job_key] == null) {
				continue;
			}
			var target = abstract_founded_file.map_job_target[job_key];
			var target_profile = target.substring(target.indexOf(":") + 1, target.length);
			var target_storage = target.substring(0, target.indexOf(":"));

			linked_jobs.push(<span key={job_key}>
					<i className="icon-folder-close"/>
					{target_storage}
					<span className="pull-right">
						<i className="icon-cog"/>
						{target_profile}
					</span>
					<mydmam.async.jobs.Minicartridge job={jobs[job_key]} />
				</span>
			);
		}

		return (<tr className={tr_classes}>
			<td>
				<mydmam.async.pathindex.reactStoragePathLink
					storagename={abstract_founded_file.storage_name}
					path={abstract_founded_file.path}
					add_link={abstract_founded_file.status !== "PROCESSED"} />
			</td>
			<td><mydmam.async.pathindex.reactDate date={abstract_founded_file.date} /></td>
			<td><mydmam.async.pathindex.reactFileSize size={abstract_founded_file.size} /></td>
			<td><mydmam.async.pathindex.reactDate date={abstract_founded_file.last_checked} /></td>
			<td>{i18n("manager.watchfolders.status." + abstract_founded_file.status)}</td>
			<td>{linked_jobs}</td>
			<td><button className="btn btn-mini btn-danger pull-right" onClick={this.btnDelete}><i className="icon-remove icon-white" /></button></td>
		</tr>);
	},
});