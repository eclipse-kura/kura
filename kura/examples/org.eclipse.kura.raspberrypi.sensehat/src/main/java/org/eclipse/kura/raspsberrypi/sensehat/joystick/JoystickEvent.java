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
package org.eclipse.kura.raspsberrypi.sensehat.joystick;

import java.io.IOException;
import java.nio.ShortBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JoystickEvent {

    private static final Logger s_logger = LoggerFactory.getLogger(JoystickEvent.class);

    private long time_sec;
    private long time_usec;
    private short type;
    private short code;
    private int value;

    public JoystickEvent() {
        clear();
    }

    public void setTimeSec(long time_sec) {
        this.time_sec = time_sec;
    }

    public void setTimeUSec(long time_usec) {
        this.time_usec = time_usec;
    }

    public void setType(short type) {
        this.type = type;
    }

    public void setCode(short code) {
        this.code = code;
    }

    public void setValue(short value) {
        this.value = value;
    }

    public long getTimeSec() {
        return this.time_sec;
    }

    public long getTimeUSec() {
        return this.time_usec;
    }

    public short getType() {
        return this.type;
    }

    public short getCode() {
        return this.code;
    }

    public int getValue() {
        return this.value;
    }

    public void clear() {
        this.time_sec = 0L;
        this.time_usec = 0L;
        this.type = 0;
        this.code = 0;
        this.value = 0;
    }

    public void parse(ShortBuffer shortBuffer) throws IOException {
        short firstShort;
        short secondShort;

        firstShort = shortBuffer.get();
        secondShort = shortBuffer.get();
        this.time_sec = secondShort << 16 | secondShort;

        firstShort = shortBuffer.get();
        secondShort = shortBuffer.get();
        this.time_usec = secondShort << 16 | firstShort;

        this.type = shortBuffer.get();
        this.code = shortBuffer.get();

        firstShort = shortBuffer.get();
        secondShort = shortBuffer.get();
        this.value = secondShort << 16 | firstShort;

        s_logger.debug(this.type + " " + this.code + " " + this.value);

    }
}
