/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.emulator.position;

public class GpsPoint {
	private double latitude;
	private double longitude;
	private double altitude;
	private String time;
	
	public GpsPoint(double latitude, double longitude, double altitude, String time) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		this.time = time;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getAltitude() {
		return altitude;
	}

	public String getTime() {
		return time;
	}
}
