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
    private short temperatureUnit; /* 8 bit unsigned */
    private int serialNumber; /* 16 bit unsigned */
    private short productionWeek; /* 8 bit unsigned */
    private short productionYear; /* 8 bit unsigned */
    private short productCode; /* 8 bit unsigned */
    private short nextService; /* 8 bit unsigned */
    private int airFilterRemainingTime; /* 16 bit unsigned */
    private int oilFilterRemainingTime; /* 16 bit unsigned */
    private int oilRemainingTime; /* 16 bit unsigned */
    private int disoilFilterRemainingTime; /* 16 bit unsigned */
    private int dryerFilterRemainingTime; /* 16 bit unsigned */
    private short maxPressure; /* 16 bit signed */
    private short minPressure; /* 16 bit signed */

    public short getCompressorPressure() {
        return this.compressorPressure;
    }

    public void setCompressorPressure(short compressorPressure) {
        this.compressorPressure = compressorPressure;
    }

    public short getCompressorTemperature() {
        return this.compressorTemperature;
    }

    public void setCompressorTemperature(short compressorTemperature) {
        this.compressorTemperature = compressorTemperature;
    }

    public short getDryerTemperature() {
        return this.dryerTemperature;
    }

    public void setDryerTemperature(short dryerTemperature) {
        this.dryerTemperature = dryerTemperature;
    }

    public short getDryerStatus() {
        return this.dryerStatus;
    }

    public void setDryerStatus(short dryerStatus) {
        this.dryerStatus = dryerStatus;
    }

    public int getInputBitmap() {
        return this.inputBitmap;
    }

    public void setInputBitmap(int inputBitmap) {
        this.inputBitmap = inputBitmap;
    }

    public short getOutputBitmap() {
        return this.outputBitmap;
    }

    public void setOutputBitmap(short outputBitmap) {
        this.outputBitmap = outputBitmap;
    }

    public short getCompressorStatus() {
        return this.compressorStatus;
    }

    public void setCompressorStatus(short compressorStatus) {
        this.compressorStatus = compressorStatus;
    }

    public short getCompressorLoad() {
        return this.compressorLoad;
    }

    public void setCompressorLoad(short compressorLoad) {
        this.compressorLoad = compressorLoad;
    }

    public long getTotalHours() {
        return this.totalHours;
    }

    public void setTotalHours(long totalHours) {
        this.totalHours = totalHours;
    }

    public long getLoadHours() {
        return this.loadHours;
    }

    public void setLoadHours(long loadHours) {
        this.loadHours = loadHours;
    }

    public short getCurrentErrorStatus() {
        return this.currentErrorStatus;
    }

    public void setCurrentErrorStatus(short currentErrorStatus) {
        this.currentErrorStatus = currentErrorStatus;
    }

    public short getPressureUnit() {
        return this.pressureUnit;
    }

    public void setPressureUnit(short pressureUnit) {
        this.pressureUnit = pressureUnit;
    }

    public short getTemperatureUnit() {
        return this.temperatureUnit;
    }

    public void setTemperatureUnit(short temperatureUnit) {
        this.temperatureUnit = temperatureUnit;
    }

    public int getSerialNumber() {
        return this.serialNumber;
    }

    public void setSerialNumber(int serialNumber) {
        this.serialNumber = serialNumber;
    }

    public short getProductionWeek() {
        return this.productionWeek;
    }

    public void setProductionWeek(short productionWeek) {
        this.productionWeek = productionWeek;
    }

    public short getProductionYear() {
        return this.productionYear;
    }

    public void setProductionYear(short productionYear) {
        this.productionYear = productionYear;
    }

    public short getProductCode() {
        return this.productCode;
    }

    public void setProductCode(short productCode) {
        this.productCode = productCode;
    }

    public short getNextService() {
        return this.nextService;
    }

    public void setNextService(short nextService) {
        this.nextService = nextService;
    }

    public int getAirFilterRemainingTime() {
        return this.airFilterRemainingTime;
    }

    public void setAirFilterRemainingTime(int airFilterRemainingTime) {
        this.airFilterRemainingTime = airFilterRemainingTime;
    }

    public int getOilFilterRemainingTime() {
        return this.oilFilterRemainingTime;
    }

    public void setOilFilterRemainingTime(int oilFilterRemainingTime) {
        this.oilFilterRemainingTime = oilFilterRemainingTime;
    }

    public int getOilRemainingTime() {
        return this.oilRemainingTime;
    }

    public void setOilRemainingTime(int oilRemainingTime) {
        this.oilRemainingTime = oilRemainingTime;
    }

    public int getDisoilFilterRemainingTime() {
        return this.disoilFilterRemainingTime;
    }

    public void setDisoilFilterRemainingTime(int disoilFilterRemainingTime) {
        this.disoilFilterRemainingTime = disoilFilterRemainingTime;
    }

    public int getDryerFilterRemainingTime() {
        return this.dryerFilterRemainingTime;
    }

    public void setDryerFilterRemainingTime(int dryerFilterRemainingTime) {
        this.dryerFilterRemainingTime = dryerFilterRemainingTime;
    }

    public short getMaxPressure() {
        return this.maxPressure;
    }

    public void setMaxPressure(short maxPressure) {
        this.maxPressure = maxPressure;
    }

    public short getMinPressure() {
        return this.minPressure;
    }

    public void setMinPressure(short minPressure) {
        this.minPressure = minPressure;
    }

    @Override
    public String toString() {
        return "CompressorStatus [compressorPressure=" + this.compressorPressure + ", compressorTemperature="
                + this.compressorTemperature + ", dryerTemperature=" + this.dryerTemperature + ", dryerStatus="
                + this.dryerStatus + ", inputBitmap=" + this.inputBitmap + ", outputBitmap=" + this.outputBitmap
                + ", compressorStatus=" + this.compressorStatus + ", compressorLoad=" + this.compressorLoad
                + ", totalHours=" + this.totalHours + ", loadHours=" + this.loadHours + ", currentErrorStatus="
                + this.currentErrorStatus + ", pressureUnit=" + this.pressureUnit + ", temperatureUnit="
                + this.temperatureUnit + ", serialNumber=" + this.serialNumber + ", productionWeek="
                + this.productionWeek + ", productionYear=" + this.productionYear + ", productCode=" + this.productCode
                + ", nextService=" + this.nextService + ", airFilterRemainingTime=" + this.airFilterRemainingTime
                + ", oilFilterRemainingTime=" + this.oilFilterRemainingTime + ", oilRemainingTime="
                + this.oilRemainingTime + ", disoilFilterRemainingTime=" + this.disoilFilterRemainingTime
                + ", dryerFilterRemainingTime=" + this.dryerFilterRemainingTime + ", maxPressure=" + this.maxPressure
                + ", minPressure=" + this.minPressure + "]";
    }
}
