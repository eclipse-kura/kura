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

package org.eclipse.kura.driver.block.task;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.driver.binary.Buffer;
import org.eclipse.kura.type.BooleanValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitTask extends UpdateBlockTask {

    private static final Logger logger = LoggerFactory.getLogger(BitTask.class);
    private int bit;

    public BitTask(ChannelRecord record, int start, int bit, Mode mode) {
        super(record, start, start + 1, mode);
        this.bit = bit;
    }

    public void setBit(int bit) {
        this.bit = bit;
    }

    @Override
    protected void runRead() {
        final ToplevelBlockTask parent = getParent();
        Buffer buffer = parent.getBuffer();

        byte b = buffer.get(getStart() - parent.getStart());

        final boolean result = (b >> this.bit & 0x01) == 1;

        logger.debug("Reading Bit: offset {} bit index {} result {}", getStart(), this.bit, result);

        this.record.setValue(new BooleanValue(result));
        onSuccess();
    }

    @Override
    protected void runWrite() {
        logger.warn("Write mode not supported");
        onFailure(new UnsupportedOperationException(
                "BitTask does not support WRITE mode, only READ and UPDATE modes are supported"));
    }

    @Override
    protected void runUpdate(ToplevelBlockTask write, ToplevelBlockTask read) {
        Buffer outBuffer = write.getBuffer();
        Buffer inBuffer = read.getBuffer();

        final int previousValueOffset = getStart() - read.getStart();
        final boolean value = (Boolean) this.record.getValue().getValue();

        byte byteValue = inBuffer.get(previousValueOffset);

        if (value) {
            byteValue |= 1 << this.bit;
        } else {
            byteValue &= ~(1 << this.bit);
        }

        inBuffer.put(previousValueOffset, byteValue);
        logger.debug("Write Bit: offset: {} value: {}", getStart(), value);
        outBuffer.put(getStart() - write.getStart(), byteValue);
    }
}