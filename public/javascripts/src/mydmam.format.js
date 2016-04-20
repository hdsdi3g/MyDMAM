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
/*jshint eqnull:true, loopfunc:true, shadow:true, jquery:true */

/**
 * Define stand-alone mydmam functions.
 */
(function(mydmam) {
	if(!mydmam.format){mydmam.format = {};}

	var addZeros = function(text) {
		var returntext = '00' + text;
		return returntext.substr(returntext.length - 2, returntext.length);
	};
	mydmam.format.addzeros = addZeros;

	var formatDate = function(epoch) {
		if (epoch < 1) {
			return "";
		}
		var date = new Date(epoch);
		var returntext = addZeros(date.getHours()) + ':' + addZeros(date.getMinutes()) + ':' + addZeros(date.getSeconds());
		return returntext;
	};
	mydmam.format.date = formatDate;

	var formatFullDate = function(epoch) {
		if (epoch < 1) {
			return "";
		}
		var date = new Date(epoch);
		var returntext = addZeros(date.getDate()) + '/' + addZeros(date.getMonth() + 1) + '/' + date.getFullYear() + ' ' + addZeros(date.getHours()) + ':'+ addZeros(date.getMinutes()) + ':' + addZeros(date.getSeconds());
		return returntext;
	};
	mydmam.format.fulldate = formatFullDate;

	var formatDate_JustDate = function(epoch) {
		var date = new Date(epoch);
		var returntext = addZeros(date.getDate()) + '/' + addZeros(date.getMonth() + 1) + '/' + date.getFullYear();
		return returntext;
	};
	mydmam.format.justdate = formatDate_JustDate;

	mydmam.format.secondsToYWDHMS = function(seconds) {
		if (seconds === 0) {
			return "0";
		}
		var sb = '';

		var oneyear = 31536000;
		// (365 x 24 x 60 x 60 ) en secondes
		var _years = seconds / oneyear;
		var years = Math.floor(_years);
		if (years > 0) {
			sb = sb.append(years);
			if (years > 1) {
				sb = sb.append(' ' + i18n('timeunit.years') + ' ');
			} else {
				sb = sb.append(' ' + i18n('timeunit.year') + ' ');
			}
		}

		var oneweek = 604800;
		// 7 x 24 x 60 x 60 en secondes
		var _weeks = (_years - years) * oneyear / oneweek;
		var weeks = Math.floor(_weeks);
		if (weeks > 0) {
			sb = sb.append(weeks);
			if (weeks > 1) {
				sb = sb.append(' ' + i18n('timeunit.weeks') + ' ');
			} else {
				sb = sb.append(' ' + i18n('timeunit.week') + ' ');
			}
		}

		var oneday = 86400;
		// 24 x 60 x 60 en secondes
		var _days = (_weeks - weeks) * oneweek / oneday;
		var days = Math.floor(_days);
		if (days > 0) {
			sb = sb.append(days);
			if (days > 1) {
				sb = sb.append(' ' + i18n('timeunit.days') + ' ');
			} else {
				sb = sb.append(' ' + i18n('timeunit.day') + ' ');
			}
		}

		// secondes restantes
		var sec = (_days - days) * oneday;

		var hrs = Math.floor(sec / 3600);
		if (hrs < 10) {
			sb = sb.append(0);
		}
		sb = sb.append(hrs);
		sb = sb.append(":");

		var _diff_hours = sec / 3600;
		var diff_hours = Math.floor(_diff_hours); // en heures
		var min = (_diff_hours - diff_hours) * 60;

		if (min < 10) {
			sb = sb.append(0);
		}
		sb = sb.append(Math.floor(min));

		sb = sb.append(":");
		var secresult = Math.round((min - Math.floor(min)) * 60);

		if (secresult < 10) {
			sb = sb.append(0);
		}

		sb = sb.append(secresult);

		return sb;
	};

	mydmam.format.msecToHMSms = function(msec) {
		if (msec === 0) {
			return "0.000";
		}
		var sb = '';

		var sec = msec / 1000;

		var hrs = Math.floor(sec / 3600);
		if (hrs >= 1) {
			if (hrs < 10) {
				sb = sb.append(0);
			}
			sb = sb.append(hrs);
			sb = sb.append(":");
		}
		
		var _diff_hours = sec / 3600;
		var diff_hours = Math.floor(_diff_hours); // en heures
		var min = (_diff_hours - diff_hours) * 60;

		if (min >= 1 | hrs >= 1) {
			if (min < 10) {
				sb = sb.append(0);
			}
			sb = sb.append(Math.floor(min));
			sb = sb.append(":");
		}

		var secresult = (min - Math.floor(min)) * 60;
		//var secresult = Math.round((min - Math.floor(min)) * 60);

		if (secresult < 10) {
			sb = sb.append(0);
		}

		sb = sb.append(secresult.toFixed(3));

		return sb;
	};

	mydmam.format.timeAgo = function(epoch_since_date, i18n_pos, i18n_neg) {
		var delta = (new Date().getTime() - epoch_since_date) / 1000;
		var i18n_label = i18n_pos;
		if (delta < 0) {
			i18n_label = i18n_neg;
		}
		return i18n(i18n_label, mydmam.format.secondsToYWDHMS(Math.abs(delta)));
	};

	var numberformat = null;
	if (window.Intl) {
		numberformat = new window.Intl.NumberFormat();
	}

	mydmam.format.number = function(value) {
		if (numberformat) {
			return numberformat.format(value);
		} else {
			return value;
		}
	};

})(window.mydmam);
