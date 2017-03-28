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

import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.CTRL_REG1;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.H0_T0_OUT_H;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.H0_T0_OUT_L;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.H0_rH_x2;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.H1_T0_OUT_H;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.H1_T0_OUT_L;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.H1_rH_x2;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.HUMIDITY_H_REG;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.HUMIDITY_L_REG;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.STATUS_REG;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.T0_OUT_H;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.T0_OUT_L;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.T0_T1_msb;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.T0_degC_x8;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.T1_OUT_H;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.T1_OUT_L;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.T1_degC_x8;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.TEMP_H_REG;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.TEMP_L_REG;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.WHO_AM_I;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.HTS221.WHO_AM_I_ID;

import java.util.Random;

public class HTS221DummyDevice extends DummyDevice {

    public static final int ID = 2;

    private Random rnd = new Random();

    @Override
    public int read() {
        switch (register) {
        case STATUS_REG:
            return 3;  // temparature (bit 1) and humiditiy (bit 2) ready
        case WHO_AM_I:
            return WHO_AM_I_ID;
        case HUMIDITY_H_REG:   // we can return any value, returning the same for the simplicity
        case HUMIDITY_L_REG:
        case TEMP_H_REG:
        case TEMP_L_REG:
        case CTRL_REG1:
            return register;
        case H0_rH_x2:  // random values are OK for calibration
        case H1_rH_x2:
        case T0_T1_msb:
        case T0_degC_x8:
        case T1_degC_x8:
        case H0_T0_OUT_H:
        case H0_T0_OUT_L:
        case H1_T0_OUT_H:
        case H1_T0_OUT_L:
        case T0_OUT_H:
        case T0_OUT_L:
        case T1_OUT_H:
        case T1_OUT_L:
            byte[] rndVal = new byte[1];
            rnd.nextBytes(rndVal);
            return rndVal[0];
        default:
            return 0;
        }
    }

}
