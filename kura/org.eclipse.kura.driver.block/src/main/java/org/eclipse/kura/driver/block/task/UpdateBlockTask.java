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

/**
 * <p>
 * This is an helper class that can be used to implement read-update-write operations that involve an same interval of
 * addresses. An example of such operation can
 * be reading a block of data from a device, updating that data and then writing it back.
 * </p>
 * <p>
 * If an instance of this class is provided as input to a {@link UpdateBlockTaskAggregator}, and its {@link Mode} is
 * {@link Mode#UPDATE}, it will be assigned to two parent {@link ToplevelBlockTask} instances by the aggregator.
 * The first {@link ToplevelBlockTask} will provide the data needed by this class for the read part of its operation,
 * and the second {@link ToplevelBlockTask} can be used by this class to write back the modified data.
 * </p>
 * <p>
 * In the scenario above, the {@link BlockTask#run()} method of this class will be called twice, the first time for the
 * read part of the operation and the second time for the write. Implementors are not required to implement the
 * {@link BlockTask#run()} method directly but must implement the {@link UpdateBlockTask#runRead()},
 * {@link UpdateBlockTask#runWrite())} and {@link UpdateBlockTask#runUpdate(ToplevelBlockTask, ToplevelBlockTask)}
 * methods instead. See the description of these methods for more information.
 * </p>
 */
public abstract class UpdateBlockTask extends ChannelBlockTask {

    private ToplevelBlockTask readParent;

    public UpdateBlockTask(ChannelRecord record, int start, int end, Mode mode) {
        super(record, start, end, mode);
    }

    /**
     * Called if the {@link Mode} of this instance is {@link Mode#READ}. This method should behave in the same way as
     * the {@link BlockTask#run()} method of a normal (non read-update-write) {@link BlockTask} instance in
     * {@link Mode#READ} mode.
     */
    protected abstract void runRead();

    /**
     * Called if the {@link Mode} of this instance is {@link Mode#WRITE}. This method should behave in the same way as
     * the {@link BlockTask#run()} method of a normal (non read-update-write) {@link BlockTask} instance in
     * {@link Mode#WRITE} mode.
     */
    protected abstract void runWrite();

    /**
     * <p>
     * Called if the {@link Mode} of this instance is {@link Mode#UPDATE}. The input data required for the operation can
     * be retrieved from the {@link Buffer} of the {@code read} task, the data should be updated by this method and
     * then written back to the {@link Buffer} of the {@code write} task.
     * </p>
     * <p>
     * If the {@link Mode} of this task is {@link Mode#UPDATE}, the {@link UpdateBlockTask#runRead()} and
     * {@link UpdateBlockTask#runWrite()} methods of this class will never be called.
     * </p>
     *
     * @param write
     *            the {@link ToplevelBlockTask} that contains the data for the read part of the read-update-write
     *            operation performed by this class
     * @param read
     *            the {@link ToplevelBlockTask} that can be used for writing back the data resulting from the
     *            operation performed by this class
     */
    protected abstract void runUpdate(ToplevelBlockTask write, ToplevelBlockTask read);

    @Override
    public void run() {
        if (getMode() == Mode.READ) {
            runRead();
        } else if (getMode() == Mode.WRITE) {
            runWrite();
        } else {
            final ToplevelBlockTask parent = getParent();
            if (parent.getMode() == Mode.READ) {
                this.readParent = parent;
                return;
            }

            if (this.readParent == null) {
                parent.abort(new IllegalStateException(
                        "UPDATE requested but read task did not succeed, operation aboreted"));
                return;
            }

            runUpdate(parent, this.readParent);
            this.readParent = null;
        }
    }
}
