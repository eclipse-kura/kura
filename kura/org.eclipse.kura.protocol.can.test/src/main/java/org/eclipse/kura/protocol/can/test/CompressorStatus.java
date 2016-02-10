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
package org.eclipse.kura.protocol.can.test;

public class CompressorStatus {
		
	private short compressorPressure; /* 16 bit signed */
	private short compressorTemperature; /* 16 bit signed */
	private short dryerTemperature; /* 16 bit signed */
	private short dryerStatus; /* 8 bit unsigned */
	private int inputBitmap; /* 16 bit unsigned */
	private short outputBitmap; /* 8 bit unsigned */
	private short compressorStatus; /* 8 bit unsigned */
	private short compressorLoad; /* 8 bit unsigned */
	private long totalHours; /* 32 bit unsigned */
	private long loadHours; /* 32 bit unsigned */
	private short currentErrorStatus; /* 8 bit unsigned */
	private short pressureUnit; /* 8 bit unsigned */
	private short temperatureUnit;  /* 8 bit unsigned */
	private int serialNumber; /* 16 bit unsigned */
	private short productionWeek; /* 8 bit unsigned */
	private short productionYear; /* 8 bit unsigned */
	private short productCode;  /* 8 bit unsigned */
	private short nextService; /* 8 bit unsigned */
	private int airFilterRemainingTime; /* 16 bit unsigned */
	private int oilFilterRemainingTime; /* 16 bit unsigned */
	private int oilRemainingTime; /* 16 bit unsigned */
	private int disoilFilterRemainingTime; /* 16 bit unsigned */
	private int dryerFilterRemainingTime; /* 16 bit unsigned */
	private short maxPressure; /* 16 bit signed */
	private short minPressure; /* 16 bit signed */
	
	public short getCompressorPressure() {
		return compressorPressure;
	}

	public void setCompressorPressure(short compressorPressure) {
		this.compressorPressure = compressorPressure;
	}
	
	public short getCompressorTemperature() {
		return compressorTemperature;
	}

	public void setCompressorTemperature(short compressorTemperature) {
		this.compressorTemperature = compressorTemperature;
	}

	public short getDryerTemperature() {
		return dryerTemperature;
	}

	public void setDryerTemperature(short dryerTemperature) {
		this.dryerTemperature = dryerTemperature;
	}

	public short getDryerStatus() {
		return dryerStatus;
	}

	public void setDryerStatus(short dryerStatus) {
		this.dryerStatus = dryerStatus;
	}

	public int getInputBitmap() {
		return inputBitmap;
	}

	public void setInputBitmap(int inputBitmap) {
		this.inputBitmap = inputBitmap;
	}

	public short getOutputBitmap() {
		return outputBitmap;
	}

	public void setOutputBitmap(short outputBitmap) {
		this.outputBitmap = outputBitmap;
	}

	public short getCompressorStatus() {
		return compressorStatus;
	}

	public void setCompressorStatus(short compressorStatus) {
		this.compressorStatus = compressorStatus;
	}

	public short getCompressorLoad() {
		return compressorLoad;
	}

	public void setCompressorLoad(short compressorLoad) {
		this.compressorLoad = compressorLoad;
	}

	public long getTotalHours() {
		return totalHours;
	}

	public void setTotalHours(long totalHours) {
		this.totalHours = totalHours;
	}

	public long getLoadHours() {
		return loadHours;
	}

	public void setLoadHours(long loadHours) {
		this.loadHours = loadHours;
	}

	public short getCurrentErrorStatus() {
		return currentErrorStatus;
	}

	public void setCurrentErrorStatus(short currentErrorStatus) {
		this.currentErrorStatus = currentErrorStatus;
	}

	public short getPressureUnit() {
		return pressureUnit;
	}

	public void setPressureUnit(short pressureUnit) {
		this.pressureUnit = pressureUnit;
	}

	public short getTemperatureUnit() {
		return temperatureUnit;
	}

	public void setTemperatureUnit(short temperatureUnit) {
		this.temperatureUnit = temperatureUnit;
	}

	public int getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(int serialNumber) {
		this.serialNumber = serialNumber;
	}
	
	public short getProductionWeek() {
		return productionWeek;
	}

	public void setProductionWeek(short productionWeek) {
		this.productionWeek = productionWeek;
	}

	public short getProductionYear() {
		return productionYear;
	}

	public void setProductionYear(short productionYear) {
		this.productionYear = productionYear;
	}
	
	public short getProductCode() {
		return productCode;
	}

	public void setProductCode(short productCode) {
		this.productCode = productCode;
	}

	public short getNextService() {
		return nextService;
	}

	public void setNextService(short nextService) {
		this.nextService = nextService;
	}

	public int getAirFilterRemainingTime() {
		return airFilterRemainingTime;
	}

	public void setAirFilterRemainingTime(int airFilterRemainingTime) {
		this.airFilterRemainingTime = airFilterRemainingTime;
	}

	public int getOilFilterRemainingTime() {
		return oilFilterRemainingTime;
	}

	public void setOilFilterRemainingTime(int oilFilterRemainingTime) {
		this.oilFilterRemainingTime = oilFilterRemainingTime;
	}

	public int getOilRemainingTime() {
		return oilRemainingTime;
	}

	public void setOilRemainingTime(int oilRemainingTime) {
		this.oilRemainingTime = oilRemainingTime;
	}

	public int getDisoilFilterRemainingTime() {
		return disoilFilterRemainingTime;
	}

	public void setDisoilFilterRemainingTime(int disoilFilterRemainingTime) {
		this.disoilFilterRemainingTime = disoilFilterRemainingTime;
	}

	public int getDryerFilterRemainingTime() {
		return dryerFilterRemainingTime;
	}

	public void setDryerFilterRemainingTime(int dryerFilterRemainingTime) {
		this.dryerFilterRemainingTime = dryerFilterRemainingTime;
	}

	public short getMaxPressure() {
		return maxPressure;
	}

	public void setMaxPressure(short maxPressure) {
		this.maxPressure = maxPressure;
	}

	public short getMinPressure() {
		return minPressure;
	}

	public void setMinPressure(short minPressure) {
		this.minPressure = minPressure;
	}

	@Override
	public String toString() {
		return "CompressorStatus [compressorPressure=" + compressorPressure
				+ ", compressorTemperature=" + compressorTemperature
				+ ", dryerTemperature=" + dryerTemperature + ", dryerStatus="
				+ dryerStatus + ", inputBitmap=" + inputBitmap
				+ ", outputBitmap=" + outputBitmap + ", compressorStatus="
				+ compressorStatus + ", compressorLoad=" + compressorLoad
				+ ", totalHours=" + totalHours + ", loadHours=" + loadHours
				+ ", currentErrorStatus=" + currentErrorStatus
				+ ", pressureUnit=" + pressureUnit + ", temperatureUnit="
				+ temperatureUnit + ", serialNumber=" + serialNumber
				+ ", productionWeek=" + productionWeek + ", productionYear="
				+ productionYear + ", productCode=" + productCode
				+ ", nextService=" + nextService + ", airFilterRemainingTime="
				+ airFilterRemainingTime + ", oilFilterRemainingTime="
				+ oilFilterRemainingTime + ", oilRemainingTime="
				+ oilRemainingTime + ", disoilFilterRemainingTime="
				+ disoilFilterRemainingTime + ", dryerFilterRemainingTime="
				+ dryerFilterRemainingTime + ", maxPressure=" + maxPressure
				+ ", minPressure=" + minPressure + "]";
	}
}
