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

package org.eclipse.kura.driver.block.task;

import static java.util.Objects.requireNonNull;

import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.ChannelStatus;

/**
 * A {@link BlockTask} that performs an operation involving a {@link ChannelRecord}. For example a
 * {@link ChannelBlockTask} instance in {@link Mode#READ} mode could extract some data from the {@link Buffer} of its
 * parent and store it in the {@link ChannelRecord}.
 *
 */
public abstract class ChannelBlockTask extends BlockTask {

    protected final ChannelRecord record;

    /**
     * Creates a new {@link ChannelBlockTask} instance.
     *
     * @param record
     *            the {@link ChannelRecord} instance
     * @param start
     *            the start address for this task
     * @param end
     *            the end address for this task
     * @param mode
     *            the mode of this task
     */
    public ChannelBlockTask(ChannelRecord record, int start, int end, Mode mode) {
        super(start, end, mode);
        requireNonNull(record, "The provided channel record cannot be null");
        this.record = record;
    }

    public ChannelRecord getRecord() {
        return record;
    }

    /**
     * Sets the {@link ChannelStatus} of the associated {@link ChannelRecord} to report a success state
     * and updates the timestamp.
     */
    @Override
    public void onSuccess() {
        this.record.setChannelStatus(new ChannelStatus(ChannelFlag.SUCCESS));
        this.record.setTimestamp(System.currentTimeMillis());
    }

    /**
     * Sets the {@link ChannelStatus} of the associated {@link ChannelRecord} to report the exception reported as
     * parameter and updates the timestamp.
     */
    @Override
    public void onFailure(Exception exception) {
        this.record.setChannelStatus(new ChannelStatus(ChannelFlag.FAILURE, null, exception));
        this.record.setTimestamp(System.currentTimeMillis());
    }
}
