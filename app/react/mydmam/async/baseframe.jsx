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

async.Home = createReactClass({
	render: function() {
		var styleh1 = {
			fontFamily: "'Exo', sans-serif",
			fontWeight: 700,
		};
		var stylep = {
			fontFamily: "'Exo', sans-serif",
			fontWeight: 300,
		};

		return (<div className="container">
			<div className="hero-unit">
				<h1 style={styleh1}>{i18n("site.name")}</h1>
				<p style={stylep}>{i18n("site.baseline")}</p>
			</div>
		</div>);
	}
});

async.Footer = createReactClass({
	render: function() {
		return (<div className="container-fluid" style={{textAlign: "center", marginTop: "1.5em"}}>
			<small className="muted">
				<a href="http://mydmam.org" style={{color: "#999999", fontWeight: 500, }} className="exofont">MyDMAM</a> {i18n("site.aboutfooter")}
			</small>
		</div>);
	}
});

