/*******************************************************************************
 * Copyright (c) 2011, 2025 Eurotech and/or its affiliates and others
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

import java.nio.ShortBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JoystickEvent {

    private static final Logger logger = LoggerFactory.getLogger(JoystickEvent.class);

    private long timeSec;
    private long timeUsec;
    private short type;
    private short code;
    private int value;

    public JoystickEvent() {
        clear();
    }

    public void setTimeSec(long timeSec) {
        this.timeSec = timeSec;
    }

    public void setTimeUSec(long timeUsec) {
        this.timeUsec = timeUsec;
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
        return this.timeSec;
    }

    public long getTimeUSec() {
        return this.timeUsec;
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
        this.timeSec = 0L;
        this.timeUsec = 0L;
        this.type = 0;
        this.code = 0;
        this.value = 0;
    }

    public void parse(ShortBuffer shortBuffer) {
        short firstShort;
        short secondShort;

        firstShort = shortBuffer.get();
        secondShort = shortBuffer.get();
        this.timeSec = secondShort << 16 | firstShort;

        firstShort = shortBuffer.get();
        secondShort = shortBuffer.get();
        this.timeUsec = secondShort << 16 | firstShort;

        this.type = shortBuffer.get();
        this.code = shortBuffer.get();

        firstShort = shortBuffer.get();
        secondShort = shortBuffer.get();
        this.value = secondShort << 16 | firstShort;

        logger.debug("{}, {}, {} ", this.type, this.code, this.value);

    }
}
