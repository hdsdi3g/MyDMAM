/*
 * This file is part of Java Tools by hdsdi3g'.
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
 * Copyright (C) hdsdi3g for hd3g.tv 2011-2013
 * 
*/
package hd3gtv.tools;

/**
 * @author hdsdi3g
 * @version 1.0
 */
public class Timecode {
	
	/**
	 * En secondes.
	 */
	protected float value;
	
	private float fps;
	
	public Timecode(float value, float fps) {
		this.value = value;
		this.fps = fps;
	}
	
	public void setFps(float fps) {
		this.fps = fps;
	}
	
	/**
	 * @param value tel que 00:00:00:00 les ":" peuvent etre remplaces par n'importe quel autre char.
	 * @param fps le nombre d'image par secondes.
	 * @throws NumberFormatException en cas d'erreur sur l'analyse des valeurs.
	 */
	public Timecode(String value, float fps) throws NumberFormatException {
		if (value.length() != 11) {
			throw new NumberFormatException("Time value is too long : \"" + value + "\"");
		}
		// 1234567890123456789
		// 00.00.00.00
		
		float hrs_val = Integer.valueOf(value.substring(0, 2));
		float min_val = Integer.valueOf(value.substring(3, 5));
		float sec_val = Integer.valueOf(value.substring(6, 8));
		float frm_val = Integer.valueOf(value.substring(9, 11));
		
		this.value = (hrs_val * 3600f) + (min_val * 60f) + (sec_val) + (frm_val / fps);
		this.fps = fps;
		
		if (frm_val >= fps) {
			throw new NumberFormatException("fps value is =< frame number");
		}
		
	}
	
	/**
	 * @return en secondes.
	 */
	public float getValue() {
		return value;
	}
	
	public float getFps() {
		return fps;
	}
	
	/**
	 * @return a Timecode like 00:00:00:00
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		// System.err.println(value);
		// System.err.println(fps);
		
		float hrs = (float) Math.floor((float) value / 3600f);
		if (hrs < 10) {
			sb.append(0);
		}
		sb.append((int) Math.floor(hrs));
		sb.append(":");
		
		float _diff_hours = (float) value / 3600f; // en heures,minutes
		int diff_hours = (int) Math.floor(_diff_hours); // en heures
		float min = ((float) _diff_hours - (float) diff_hours) * 60f;
		if (min < 10) {
			sb.append(0);
		}
		sb.append((int) Math.floor(min));
		sb.append(":");
		
		int secresult = (int) Math.floor((min - Math.floor(min)) * (float) 60);
		
		if (secresult < 10) {
			sb.append(0);
		}
		sb.append(secresult);
		sb.append(":");
		
		float frmresult = (value - (float) Math.floor(value)) * fps;
		if (Math.floor(frmresult) < 10) {
			sb.append(0);
		}
		
		/** T O D O bug arrondi avec perte d'une image, reecrire avec le calcul en image plutot qu'en secondes */
		sb.append(Math.round(Math.floor(frmresult)));
		
		return sb.toString();
	}
	
	public static float delta(Timecode from, Timecode to) throws NumberFormatException {
		if (from.fps != to.fps) {
			throw new NumberFormatException("Different fps values, incompatible timecodes.");
		}
		return to.value - from.value;
	}
	
}
