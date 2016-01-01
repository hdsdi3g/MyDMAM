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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/

manager.Perfstats = React.createClass({
	getInitialState: function() {
		return {
			list: {},
			interval: null,
		};
	},
	componentWillMount: function() {
		this.refresh();
	},
	refresh: function() {
		mydmam.async.request("instances", "allperfstats", null, function(list) {
			this.setState({list: list});
		}.bind(this));
	},
	componentDidMount: function(){
		this.setState({interval: setInterval(this.refresh, 5000)});
	},
	componentWillUnmount: function() {
		if (this.state.interval) {
			clearInterval(this.state.interval);
		}
	},
	render: function() {
		var items = [];
		for (var instance_ref in this.state.list) {
			items.push(<manager.PerfstatsInstance key={instance_ref} instance={this.state.list[instance_ref]} />);
		}
		return (
			<div>{items}</div>
		);
	},
});

manager.PerfstatsInstance = React.createClass({
	render: function() {
		var instance = this.props.instance;

		var showMBsize= function(val) {
			if (val > 1000 * 1000 * 10) {
				return mydmam.format.number(Math.round(val / (1000 * 1000))) + " MB";
			} else {
				return mydmam.format.number(val) + " bytes";
			}
		}

		var update_since = null;
		if (instance.now + 2000 < Date.now()) {
			update_since = (<mydmam.async.pathindex.reactSinceDate i18nlabel="manager.perfstats.since" date={instance.now} />);
		}

		var percent_free = (instance.freeMemory / instance.maxMemory) * 100;
		var percent_total = ((instance.totalMemory / instance.maxMemory) * 100) - percent_free;

		var heap_used = ((instance.heapUsed / instance.maxMemory) * 100);
		var non_heap_used = ((instance.nonHeapUsed / instance.maxMemory) * 100);

		var gc_table = [];
		for (var pos in instance.gc) {
			var gc = instance.gc[pos];
			gc_table.push(<tr key={pos}>
				<th>{gc.name}</th>
				<td>{gc.time / 1000} sec</td>
				<td>{gc.count} items</td>
			</tr>);
		}

		var os_table = null;
		if (instance.os) {
			var os = instance.os;
			os_table = (<table className="table table-bordered table-striped table-condensed table-hover" style={{width: "inherit"}}>
				<tr>
					<th>CPU load</th>
					<td>JVM process: {Math.round(os.getProcessCpuLoad * 100) / 100}</td>
					<td colSpan="2">System: {Math.round(os.getSystemCpuLoad * 100) / 100}</td>
				</tr>
				<tr>
					<th>JVM CPU time</th>
					<td colSpan="3">{Math.round(os.getProcessCpuTime / (1000 * 1000 * 100)) / 100} sec</td>
				</tr>
				<tr>
					<th>Physical memory</th>
					<td>Free: {showMBsize(os.getFreePhysicalMemorySize)}</td>
					<td>Total: {showMBsize(os.getTotalPhysicalMemorySize)}</td>
					<td>Used: {Math.floor(((os.getTotalPhysicalMemorySize - os.getFreePhysicalMemorySize) / os.getTotalPhysicalMemorySize) * 100)}%</td>
				</tr>
				<tr>
					<th>Swap</th>
					<td>Free: {showMBsize(os.getFreeSwapSpaceSize)}</td>
					<td>Total: {showMBsize(os.getTotalSwapSpaceSize)}</td>
					<td>Used: {Math.floor(((os.getTotalSwapSpaceSize - os.getFreeSwapSpaceSize) / os.getTotalSwapSpaceSize) * 100)}%</td>
				</tr>
				<tr>
					<td colSpan="4">Committed virtual memory size: {showMBsize(os.getCommittedVirtualMemorySize)}</td>
				</tr>
			</table>);
		}

		return (<div>
			<h4>
				{instance.instance_name}&nbsp;
				<small className="muted">{instance.pid}@{instance.host_name}</small>&nbsp;
				<span className="badge badge-important">
					Load {Math.round(instance.getSystemLoadAverage * 100)/100}
				</span>&nbsp;
				{update_since}
			</h4>
			<div style={{marginLeft: "15px"}}>
				Memory free: {showMBsize(instance.freeMemory)}, total: {showMBsize(instance.totalMemory)}, max: {showMBsize(instance.maxMemory)}.<br />
			    <div className="progress" style={{width: "40%"}}>
					<div className="bar bar-warning" style={{width: percent_total + "%"}}></div>
					<div className="bar bar-success" style={{width: percent_free + "%"}}></div>
				</div>

				<p>
					Classes count unloaded: {instance.getUnloadedClassCount}, loaded: {instance.getLoadedClassCount}, total loaded: {instance.getTotalLoadedClassCount}, object pending finalization: {instance.getObjectPendingFinalizationCount}
				</p>
				
				Heap memory: <span className="text-info">{showMBsize(instance.heapUsed)}</span>, non heap: <span className="text-error">{showMBsize(instance.nonHeapUsed)}</span><br />

				<div className="progress" style={{width: "40%"}}>
					<div className="bar bar-info" style={{width: heap_used + "%"}}></div>
					<div className="bar bar-danger" style={{width: non_heap_used + "%"}}></div>
				</div>

				{os_table}
				
				<table className="table table-bordered table-striped table-condensed table-hover" style={{width: "inherit"}}>
					<tbody>
						{gc_table}
					</tbody>
				</table>
			</div>
			<hr />			
		</div>);
	},
});
