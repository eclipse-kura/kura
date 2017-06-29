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
import org.eclipse.kura.type.ByteArrayValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ByteArrayTask extends ChannelBlockTask {

    private static final Logger logger = LoggerFactory.getLogger(ByteArrayTask.class);

    public ByteArrayTask(ChannelRecord record, int start, int end, Mode mode) {
        super(record, start, end, mode);
    }

    @Override
    public void run() {
        final ToplevelBlockTask parent = getParent();
        Buffer buffer = parent.getBuffer();

        if (getMode() == Mode.READ) {
            byte[] data = new byte[getEnd() - getStart()];
            logger.debug("Read byte array: offset: {} length: {}", getStart(), data.length);

            buffer.read(getStart() - parent.getStart(), data);

            this.record.setValue(new ByteArrayValue(data));
            onSuccess();
        } else {
            byte[] value = (byte[]) this.record.getValue().getValue();
            int writeLength = Math.min(getEnd() - getStart(), value.length);

            logger.debug("Write byte array: offset: {} length: {}", getStart(), writeLength);

            buffer.write(getStart() - parent.getStart(), writeLength, value);
        }
    }
}
