/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.raspberrypi.sensehat.sensors.dummydevices;

import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.CTRL_REG2_M;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.OUT_X_H_M;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.OUT_X_L_M;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.OUT_Y_H_M;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.OUT_Y_L_M;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.OUT_Z_H_M;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.OUT_Z_L_M;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.WHO_AM_I_M;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LSM9DS1.WHO_AM_I_M_ID;

public class LSM9DS1TMagDummyyDevice extends DummyDevice {

    public static final int ID = 4;

    @Override
    public int read() {
        switch (register) {
        case WHO_AM_I_M:
            return WHO_AM_I_M_ID;
        case CTRL_REG2_M:
            return register;
        case OUT_X_L_M:
        case OUT_Y_L_M:
        case OUT_Z_L_M:
            return -10;
        case OUT_X_H_M:
        case OUT_Y_H_M:
        case OUT_Z_H_M:
            return 10;
        default:
            return 0;
        }
    }

}
