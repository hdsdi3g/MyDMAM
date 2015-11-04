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
jobs.Minicartridge =  React.createClass({
	render: function() {
		var job = this.props.job;
		/*
		status
		update_date
		processing_error
		neededstorages
		hookednames
		delete_after_completed
		urgent
		*/

		var progression_bar = null;
		var last_message = null;
		var step = null;

		if (job.progression) {
			var width = 0;
			if (job.progression.progress_size > 0) {
				width = (Math.round(job.progression.progress / job.progression.progress_size) * 100) + "%";
			}

			progression_bar = (
				<div className="progress" style={{height: "12px", marginBottom: 0}}>
					<div className="bar" style={{width: width}} />
				</div>
			);

			last_message = (<em><i className="icon-comment"/> {job.progression.last_message}</em>);

			if ((job.progression.step > 0) & (job.progression.step_count > 0)) {
				step = (<strong className="pull-right">
						{job.progression.step}
						<i className="icon-arrow-right" />
						{job.progression.step_count}
					</strong>
				);
			}
		}

		return (<div>
				<small>
					{job.name}{step}
					{progression_bar}
					{last_message}
				</small>
			</div>);
	},
});