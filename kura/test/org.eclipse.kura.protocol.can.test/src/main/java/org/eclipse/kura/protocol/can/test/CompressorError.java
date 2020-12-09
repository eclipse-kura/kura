/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.protocol.can.test;

public class CompressorError {

    private short code; /* 8 bit unsigned */
    private short day; /* 8 bit unsigned */
    private short month; /* 8 bit unsigned */
    private short year; /* 8 bit unsigned */

    public short getCode() {
        return this.code;
    }

    public void setCode(short code) {
        this.code = code;
    }

    public short getDay() {
        return this.day;
    }

    public void setDay(short day) {
        this.day = day;
    }

    public short getMonth() {
        return this.month;
    }

    public void setMonth(short month) {
        this.month = month;
    }

    public short getYear() {
        return this.year;
    }

    public void setYear(short year) {
        this.year = year;
    }

    @Override
    public String toString() {
        return "CompressorError [code=" + this.code + ", day=" + this.day + ", month=" + this.month + ", year="
                + this.year + "]";
    }
}
