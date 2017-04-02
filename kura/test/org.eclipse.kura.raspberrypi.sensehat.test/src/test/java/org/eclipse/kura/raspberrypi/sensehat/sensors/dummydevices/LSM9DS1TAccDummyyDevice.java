/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.raspberrypi.sensehat.sensors.dummydevices;

import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.CTRL_REG1_G;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.CTRL_REG6_XL;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.OUT_X_H_G;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.OUT_X_H_XL;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.OUT_X_L_G;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.OUT_X_L_XL;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.OUT_Y_H_G;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.OUT_Y_H_XL;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.OUT_Y_L_G;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.OUT_Y_L_XL;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.OUT_Z_H_G;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.OUT_Z_H_XL;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.OUT_Z_L_G;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.OUT_Z_L_XL;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.WHO_AM_I_AG_ID;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.WHO_AM_I_XG;

public class LSM9DS1TAccDummyyDevice extends DummyDevice {

    public static final int ID = 3;

    @Override
    public int read() {
        switch (register) {
        case WHO_AM_I_XG:
            return WHO_AM_I_AG_ID;
        case CTRL_REG1_G:  // we can return any value, returning the same for the simplicity
        case CTRL_REG6_XL:
            return register;
        case OUT_X_L_XL:
        case OUT_Y_L_XL:
        case OUT_Z_L_XL:
        case OUT_X_L_G:
        case OUT_Y_L_G:
        case OUT_Z_L_G:
            return -10;
        case OUT_X_H_XL:
        case OUT_Y_H_XL:
        case OUT_Z_H_XL:
        case OUT_X_H_G:
        case OUT_Y_H_G:
        case OUT_Z_H_G:
            return 10;
        default:
            return 0;
        }
    }

}
