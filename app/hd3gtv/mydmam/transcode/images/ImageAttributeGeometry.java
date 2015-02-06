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
package hd3gtv.mydmam.transcode.images;

import hd3gtv.mydmam.transcode.TranscodeProfile;

public class ImageAttributeGeometry {
	int x;
	int y;
	int width;
	int height;
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public String toString() {
		StringBuffer db = new StringBuffer();
		
		if (x > 0 | y > 0) {
			db.append("+");
			db.append(x);
			db.append("+");
			db.append(y);
			db.append("; ");
		}
		db.append(width);
		db.append("x");
		db.append(height);
		
		return db.toString();
	}
	
	public static enum Compare {
		IS_BIGGER_THAN_PROFILE, IS_SMALLER_THAN_PROFILE, EQUALS_PROFILE
	}
	
	Compare compare(TranscodeProfile reference) {
		int profile_res = (int) (reference.getOutputformat().getResolution().getX() * reference.getOutputformat().getResolution().getY());
		int this_res = height * width;
		
		if (profile_res > this_res) {
			return Compare.IS_SMALLER_THAN_PROFILE;
		} else if (profile_res < this_res) {
			return Compare.IS_BIGGER_THAN_PROFILE;
		}
		return Compare.EQUALS_PROFILE;
	}
}
