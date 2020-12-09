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
package org.eclipse.kura.raspberrypi.sensehat.impl;

import java.io.IOException;

import org.eclipse.kura.raspberrypi.sensehat.SenseHat;
import org.eclipse.kura.raspberrypi.sensehat.ledmatrix.FrameBuffer;
import org.eclipse.kura.raspberrypi.sensehat.ledmatrix.FrameBufferRaw;
import org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221;
import org.eclipse.kura.raspberrypi.sensehat.sensors.LPS25H;
import org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1;
import org.eclipse.kura.raspsberrypi.sensehat.joystick.Joystick;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SenseHatImpl implements SenseHat {

    private static final Logger s_logger = LoggerFactory.getLogger(SenseHatImpl.class);
    private ComponentContext m_ctx;

    protected void activate(ComponentContext componentContext) {
        s_logger.info("Activate SenseHat Service.");
        this.m_ctx = componentContext;
    }

    protected void deactivate(ComponentContext componentContext) {
        s_logger.info("Deactivate SenseHat Service.");
        FrameBuffer.closeFrameBuffer();
        Joystick.closeJoystick();
        HTS221.closeDevice();
        LPS25H.closeDevice();
        LSM9DS1.closeDevice();
    }

    @Override
    public FrameBuffer getFrameBuffer() {
        return FrameBuffer.getFrameBuffer(this.m_ctx);
    }

    @Override
    public Joystick getJoystick() {
        return Joystick.getJoystick();
    }

    @Override
    public HTS221 getHumiditySensor(int bus, int address, int addressSize, int frequency) {
        return HTS221.getHumiditySensor(bus, address, addressSize, frequency);
    }

    @Override
    public LPS25H getPressureSensor(int bus, int address, int addressSize, int frequency) {
        return LPS25H.getPressureSensor(bus, address, addressSize, frequency);
    }

    @Override
    public LSM9DS1 getIMUSensor(int bus, int accAddress, int magAddress, int addressSize, int frequency) {
        return LSM9DS1.getIMUSensor(bus, accAddress, magAddress, addressSize, frequency);
    }

    @Override
    public FrameBufferRaw getFrameBufferRaw() {
        try {
            return FrameBufferRaw.getFrameBuffer(m_ctx);
        } catch (IOException e) {
            return null;
        }
    }

}
