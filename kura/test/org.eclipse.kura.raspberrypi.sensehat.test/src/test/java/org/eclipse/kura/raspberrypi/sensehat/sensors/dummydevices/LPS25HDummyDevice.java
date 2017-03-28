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

import static org.eclipse.kura.raspberrypi.sensehat.sensors.LPS25H.PRESS_OUT_H;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LPS25H.PRESS_OUT_L;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LPS25H.PRESS_OUT_XL;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LPS25H.TEMP_OUT_H;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LPS25H.TEMP_OUT_L;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LPS25H.WHO_AM_I;
import static org.eclipse.kura.raspberrypi.sensehat.sensors.LPS25H.WHO_AM_I_ID;

public class LPS25HDummyDevice extends DummyDevice {

    public static final int ID = 1;

    @Override
    public int read() {
        switch (register) {
        case WHO_AM_I:
            return WHO_AM_I_ID;
        case PRESS_OUT_L:   // we can return any value, returning the same for the simplicity
        case PRESS_OUT_H:
        case PRESS_OUT_XL:
        case TEMP_OUT_L:
        case TEMP_OUT_H:
            return register;
        default:
            return 0;
        }
    }

}
