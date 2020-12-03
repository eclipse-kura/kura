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

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class DummyDevice {

    protected boolean open;
    protected int register = -1;

    public abstract int read();

    public void write(int value) throws IOException {
        register = value;
    }

    public void write(int register, int size, ByteBuffer value) throws IOException {
        if (value.array().length == 4) {
            this.register = value.getInt() & 0xFF;
        } else {
            this.register = value.get() & 0xFF;
        }
    }

    public void close() throws IOException {
        open = false;
    }

    public boolean isOpen() {
        return open;
    }

}
