/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.raspberrypi.sensehat;

import org.eclipse.kura.raspberrypi.sensehat.ledmatrix.FrameBuffer;
import org.eclipse.kura.raspberrypi.sensehat.ledmatrix.FrameBufferRaw;
import org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221;
import org.eclipse.kura.raspberrypi.sensehat.sensors.LPS25H;
import org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1;
import org.eclipse.kura.raspsberrypi.sensehat.joystick.Joystick;

public interface SenseHat {

    public FrameBuffer getFrameBuffer();

    public FrameBufferRaw getFrameBufferRaw();

    public Joystick getJoystick();

    public HTS221 getHumiditySensor(int bus, int address, int addressSize, int frequency);

    public LPS25H getPressureSensor(int bus, int address, int addressSize, int frequency);

    public LSM9DS1 getIMUSensor(int bus, int accAddress, int magAddress, int addressSize, int frequency);

}
