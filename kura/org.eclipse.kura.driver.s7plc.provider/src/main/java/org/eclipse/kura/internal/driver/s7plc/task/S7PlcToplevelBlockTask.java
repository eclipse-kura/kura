/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.internal.driver.s7plc.task;

import java.io.IOException;

import org.eclipse.kura.driver.binary.Buffer;
import org.eclipse.kura.driver.binary.ByteArrayBuffer;
import org.eclipse.kura.driver.block.task.Mode;
import org.eclipse.kura.driver.block.task.ToplevelBlockTask;
import org.eclipse.kura.internal.driver.s7plc.S7PlcDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S7PlcToplevelBlockTask extends ToplevelBlockTask {

    private static final Logger logger = LoggerFactory.getLogger(S7PlcDriver.class);

    private int areaNo;
    private ByteArrayBuffer data;
    private S7PlcDriver driver;

    public S7PlcToplevelBlockTask(S7PlcDriver driver, Mode mode, int dbNumber, int start, int end) {
        super(start, end, mode);
        this.areaNo = dbNumber;
        this.driver = driver;
    }

    public void processBuffer() throws IOException {
        if (getMode() == Mode.READ) {
            logger.debug("Reading from PLC, DB{} offset: {} length: {}", areaNo, getStart(), getBuffer().getLength());
            driver.read(areaNo, getStart(), ((ByteArrayBuffer) getBuffer()).getBackingArray());
        } else {
            logger.debug("Writing to PLC, DB{} offset: {} length: {}", areaNo, getStart(), getBuffer().getLength());
            driver.write(areaNo, getStart(), ((ByteArrayBuffer) getBuffer()).getBackingArray());
        }
    }

    @Override
    public Buffer getBuffer() {
        if (data == null) {
            data = new ByteArrayBuffer(new byte[getEnd() - getStart()]);
        }
        return data;
    }

}
