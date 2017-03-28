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
