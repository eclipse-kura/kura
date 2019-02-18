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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.kura.driver.binary.Buffer;

/**
 * <p>
 * Represent a {@link BlockTask} that has a list of child tasks and maintains a {@link Buffer}.
 * </p>
 * <p>
 * Each child task operates on a subset of the interval described by this task and generally use the buffer provided by
 * this class to perform their operation.
 * </p>
 * <p>
 * The operation described by a {@link ToplevelBlockTask} generally involves transferring data
 * between a {@link Buffer} and device.
 * The children of a {@link ToplevelBlockTask} generally do not perform actual I/O operations but simply use the buffer
 * managed by an instance of this class by either filling or consuming it depending on the {@link Mode}. In particular:
 * </p>
 * <ul>
 * <li>If the {@link Mode} of a {@link ToplevelBlockTask} is {@link Mode#READ} calling the {@link BlockTask#run()}
 * method of this class will trigger the following operations:
 * <ol>
 * <li>The {@link ToplevelBlockTask#processBuffer()} method is called, its implementation must fill the {@link Buffer}
 * of this class with some data, usually obtained performing a read operation from some device.</li>
 * <li>
 * If previous step completes successfully the {@link BlockTask#run()} method of each children will be called. Each
 * child will use the data of the {@link Buffer} filled at previous step to perform its operation (for example
 * extracting some information from it). If the previous step fails the {@link BlockTask#onFailure(Exception)} method of
 * the children will be called to notify the failure.
 * </li>
 * </ol>
 * </li>
 * <li>If the {@link Mode} of a {@link ToplevelBlockTask} is {@link Mode#WRITE} calling the {@link BlockTask#run()}
 * method of this class will trigger the following operations:
 * <ol>
 * <li>
 * The {@link BlockTask#run()} method of each children will be called. Each
 * child should fill a portion of the {@link Buffer} of this class.
 * </li>
 * <li>
 * The {@link ToplevelBlockTask#processBuffer()} method is called, its implementation should use the {@link Buffer}
 * previously filled by the children, for example by transferring it to some device.
 * </li>
 * <li>
 * If previous step completes successfully the {@link BlockTask#onSuccess()} method of each children will be called. If
 * the previous step fails the {@link BlockTask#onFailure(Exception)} method of
 * the children will be called to notify the failure.
 * </li>
 * </ol>
 * </li>
 * </ul>
 */
public abstract class ToplevelBlockTask extends BlockTask {

    private final ArrayList<BlockTask> children = new ArrayList<>();
    private boolean isAborted;

    public ToplevelBlockTask(int start, int end, Mode mode) {
        super(start, end, mode);
    }

    /**
     * Returns the {@link Buffer} managed by this {@link ToplevelBlockTask}. The result must not be null and its size
     * must be equal to {@code this.getEnd() - this.getStart()}
     *
     * @return The {@link Buffer} managed by this {@link ToplevelBlockTask}.
     */
    public abstract Buffer getBuffer();

    /**
     * Performs an operation on the {@link Buffer} managed by this {@link ToplevelBlockTask}. See the class description
     * for more information on the expected behavior of this method.
     *
     * @throws IOException
     *             If some I/O error occur
     */
    public abstract void processBuffer() throws IOException;

    /**
     * Clears the list of children of this {@link ToplevelBlockTask}
     */
    public void clear() {
        this.children.clear();
    }

    /**
     * Returns the children of this {@link ToplevelBlockTask} as an unmodifiable list.
     *
     * @return The children of this task
     */
    public List<BlockTask> getChildren() {
        return Collections.unmodifiableList(this.children);
    }

    /**
     * Invokes the {@link BlockTask#run()} method on the children of this class.
     *
     * @throws IOException
     *             If the {@link BlockTask#run()} method of a child throw an {@link IOException}
     */
    protected void runChildren() throws IOException {
        this.isAborted = false;
        for (BlockTask child : getChildren()) {
            child.setParent(this);
            child.run();
            if (this.isAborted) {
                break;
            }
        }
    }

    /**
     * Adds a child to this {@link ToplevelBlockTask}
     *
     * @param child
     *            the child to be added.
     * @throws IllegalArgumentException
     *             if the interval specified by the provided {@link BlockTask} is not contained by the interval
     *             specified by this {@link ToplevelBlockTask}
     */
    public void addChild(BlockTask child) {
        if (!this.contains(child)) {
            throw new IllegalArgumentException("The child block must be contained by this block");
        }
        this.children.add(child);
    }

    /**
     * Aborts the operation performed by this {@link ToplevelBlockTask}, it can be called by a child if some
     * non-recoverable error is detected. The {@link BlockTask#onFailure(Exception)} method of the children will be
     * called to notify the error.
     *
     * @param exception
     *            the reason for which the execution must be aborted.
     */
    public void abort(Exception exception) {
        this.isAborted = true;
        onFailure(exception);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() throws IOException {
        try {
            if (getMode() == Mode.READ) {
                processBuffer();
                runChildren();
            } else {
                runChildren();
                processBuffer();
                onSuccess();
            }
        } catch (Exception e) {
            onFailure(e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}}
     */
    @Override
    public void onSuccess() {
        for (BlockTask child : getChildren()) {
            child.onSuccess();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFailure(Exception exception) {
        for (BlockTask child : getChildren()) {
            child.onFailure(exception);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(super.toString());

        if (!this.children.isEmpty()) {
            builder.append(" children: ");
            for (BlockTask b : getChildren()) {
                builder.append(b.toString());
                builder.append(' ');
            }
        }

        return builder.toString();
    }
}
