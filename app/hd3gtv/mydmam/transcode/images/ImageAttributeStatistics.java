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

public class ImageAttributeStatistics {
	
	float min;
	float max;
	float mean;
	float standardDeviation;
	float kurtosis;
	float skewness;
	
	public float getKurtosis() {
		return kurtosis;
	}
	
	public float getMax() {
		return max;
	}
	
	public float getMean() {
		return mean;
	}
	
	public float getMin() {
		return min;
	}
	
	public float getSkewness() {
		return skewness;
	}
	
	public float getStandardDeviation() {
		return standardDeviation;
	}
}
