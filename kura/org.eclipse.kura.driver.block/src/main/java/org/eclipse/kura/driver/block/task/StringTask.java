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

import java.nio.charset.StandardCharsets;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.driver.binary.Buffer;
import org.eclipse.kura.type.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringTask extends ChannelBlockTask {

    private static final Logger logger = LoggerFactory.getLogger(StringTask.class);

    public StringTask(ChannelRecord record, int start, int end, Mode mode) {
        super(record, start, end, mode);
    }

    @Override
    public void run() {
        final ToplevelBlockTask parent = getParent();
        final Buffer buffer = parent.getBuffer();

        if (getMode() == Mode.READ) {
            byte[] data = new byte[getEnd() - getStart()];
            buffer.read(getStart() - parent.getStart(), data);

            final String result = new String(data, StandardCharsets.US_ASCII);

            logger.debug("Read string: offset: {} length: {} result: {}", getStart(), data.length, result);

            this.record.setValue(new StringValue(result));
            onSuccess();
        } else {
            String value = (String) this.record.getValue().getValue();
            int writeLength = Math.min(getEnd() - getStart(), value.length());

            logger.debug("Write string: offset: {} length: {} value: {}", getStart(), writeLength, value);

            final byte[] data = value.getBytes(StandardCharsets.US_ASCII);

            buffer.write(getStart() - parent.getStart(), data);
        }
    }
}