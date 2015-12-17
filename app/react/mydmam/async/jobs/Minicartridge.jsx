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


		return (<div>
				<small>
					{job.name}
					<mydmam.async.broker.JobProgression job={job} />
				</small>
			</div>);
	},
});