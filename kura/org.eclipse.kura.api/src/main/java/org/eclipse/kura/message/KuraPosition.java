/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.message;

import java.util.Date;

/**
 * EdcPosition is a data structure to capture a geo location. It can be
 * associated to an EdcPayload to geotag an EdcMessage before sending to the
 * Everyware Cloud. Refer to the description of each of the fields for more
 * information on the model of EdcPosition.
 */
public class KuraPosition 
{
	/**
	 * Longitude of this position in degrees. This is a mandatory field.
	 */
	private Double longitude;

	/**
	 * Latitude of this position in degrees. This is a mandatory field.
	 */
	private Double latitude;

	/**
	 * Altitude of the position in meters.
	 */
	private Double altitude;

	/**
	 * Dilution of the precision (DOP) of the current GPS fix.
	 */
	private Double precision;

	/**
	 * Heading (direction) of the position in degrees
	 */
	private Double heading;

	/**
	 * Speed for this position in meter/sec.
	 */
	private Double speed;

	/**
	 * Timestamp extracted from the GPS system
	 */
	private Date timestamp;

	/**
	 * Number of satellites seen by the systems
	 */
	private Integer satellites;

	/**
	 * Status of GPS system: 1 = no GPS response, 2 = error in response, 4 =
	 * valid.
	 */
	private Integer status;

	public KuraPosition() {
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public Double getAltitude() {
		return altitude;
	}

	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}

	public Double getPrecision() {
		return precision;
	}

	public void setPrecision(double precision) {
		this.precision = precision;
	}

	public Double getHeading() {
		return heading;
	}

	public void setHeading(double heading) {
		this.heading = heading;
	}

	public Double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public Integer getSatellites() {
		return satellites;
	}

	public void setSatellites(int satellites) {
		this.satellites = satellites;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
}