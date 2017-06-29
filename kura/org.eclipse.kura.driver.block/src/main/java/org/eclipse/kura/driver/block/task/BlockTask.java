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

import static java.util.Objects.requireNonNull;

import java.io.IOException;

import org.eclipse.kura.driver.block.Block;

/**
 * This class represents an generic operation that involves a specific interval of addresses on a given address space.
 *
 * @see BlockTaskAggregator
 * @see ToplevelBlockTask
 */
public abstract class BlockTask extends Block {

    private final Mode mode;
    private ToplevelBlockTask parent;

    /**
     * Creates a new {@link BlockTask}
     *
     * @param start
     *            the start address of the interval involved by the operation
     * @param end
     *            the end address of the interval involved by the operation
     * @param mode
     *            the {@link Mode} of the operation
     */
    public BlockTask(int start, int end, Mode mode) {
        super(start, end);
        requireNonNull(mode, "The provided mode cannot be null");
        this.mode = mode;
    }

    /**
     * Sets the parent of this task.
     *
     * @see BlockTaskAggregator
     * @param parent
     *            the parent task
     */
    public void setParent(ToplevelBlockTask parent) {
        this.parent = parent;
    }

    /**
     * Returns the parent of this task.
     *
     * @return the parent task, or {@code null} if this task has no parent.
     */
    public ToplevelBlockTask getParent() {
        return this.parent;
    }

    /**
     * Returns the {@link Mode} of this task.
     *
     * @return the {@link Mode} of this task
     */
    public Mode getMode() {
        return this.mode;
    }

    /**
     * Performs the operation described by this task.
     *
     * @throws IOException
     *             If an I/O error occurs during the operation
     */
    public abstract void run() throws IOException;

    /**
     * Notifies this task that the operation performed by the parent task is failed.
     *
     * @param reason
     *            An {@link Exception} instance describing the reason of the failure
     */
    public abstract void onFailure(Exception exception);

    /**
     * Notifies this task that the operation performed by the parent task failed.
     */
    public abstract void onSuccess();

}
