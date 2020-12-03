/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.example.driver.sensehat;

import java.util.function.Function;

import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;
import org.eclipse.kura.type.TypedValue;

public class ReadTask {

    private static final ChannelStatus CHANNEL_STATUS_OK = new ChannelStatus(ChannelFlag.SUCCESS);

    private final ChannelRecord record;
    private final Function<SenseHatInterface, TypedValue<?>> reader;

    public ReadTask(final ChannelRecord record, final Function<SenseHatInterface, TypedValue<?>> reader) {
        this.record = record;
        this.reader = reader;
    }

    public void exec(SenseHatInterface sensehat) {
        try {
            record.setValue(this.reader.apply(sensehat));
            record.setChannelStatus(CHANNEL_STATUS_OK);
        } catch (Exception e) {
            record.setChannelStatus(new ChannelStatus(ChannelFlag.FAILURE, e.getMessage(), e));
        }
        record.setTimestamp(System.currentTimeMillis());
    }

}