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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.kura.driver.block.Block;
import org.eclipse.kura.driver.block.BlockFactory;
import org.eclipse.kura.driver.block.ProhibitedBlock;

/**
 * <p>
 * Represents a specialized version of {@link BlockTaskAggregator} that supports the aggregation of tasks whose
 * {@link Mode} is {@link Mode#UPDATE}. This class only supports the aggregation of tasks that are either in
 * {@link Mode#WRITE} or {@link Mode#UPDATE} mode.
 * </p>
 * <p>
 * <p>
 * If a task whose {@link Mode} is {@link Mode#UPDATE} is found in the input block list, it will be assigned to two
 * {@link ToplevelBlockTask} instances, both of them will contain the interval specified by the update task.
 * The first one will operate in {@link Mode#READ}, the second one in {@link Mode#WRITE}.
 * </p>
 * <p>
 * If the {@link ToplevelBlockTask} instances described above are executed in sequence, the {@link BlockTask#run()}
 * method of the update task will be called twice, the first time the parent of the update task will be assigned to the
 * {@link ToplevelBlockTask} in {@link Mode#READ} mode, the second time it will be assigned to the
 * {@link ToplevelBlockTask} in {@link Mode#WRITE} mode.
 * </p>
 * <p>
 * If a task that operates in {@link Mode#UPDATE} needs to be defined, the
 * {@link UpdateBlockTask} class should be extended.
 * </p>
 * <p>
 * If some tasks in {@link Mode#UPDATE} are found in the input block list, this class will perform two
 * separate aggregation processes: one for the {@link ToplevelBlockTask} instances required to fetch the data needed for
 * the read part of the read-update-write operations, and the other for the {@link ToplevelBlockTask} instances required
 * for the write part.
 * </p>
 *
 * @see ToplevelBlockTask
 * @see BlockTask
 */
public class UpdateBlockTaskAggregator extends BlockTaskAggregator {

    private final BlockTaskAggregator readTaskAggregator;

    /**
     * Creates a new {@link UpdateBlockTaskAggregator} instance
     *
     * @param tasks
     *            the list of input tasks
     * @param readTaskFactory
     *            a {@link BlockFactory} that will be used for creating the {@link ToplevelBlockTask} instances in
     *            {@link Mode#READ} mode
     * @param writeTaskFactory
     *            a {@link BlockFactory} that will be used for creating the {@link ToplevelBlockTask} instances in
     *            {@link Mode#WRITE} mode
     * @throws IllegalArgumentException
     *             if any task in {@link Mode#READ} is found in the input task list
     */
    public UpdateBlockTaskAggregator(List<Block> tasks, BlockFactory<ToplevelBlockTask> readTaskFactory,
            BlockFactory<ToplevelBlockTask> writeTaskFactory) {
        super(tasks, writeTaskFactory);
        requireNonNull(readTaskFactory, "Read tasks factory cannot be null");
        this.readTaskAggregator = createReadTaskAggregator(tasks, readTaskFactory);
    }

    private static BlockTaskAggregator createReadTaskAggregator(List<Block> tasks,
            BlockFactory<ToplevelBlockTask> readTaskFactory) {
        ArrayList<Block> updateTasks = tasks.stream().filter(block -> {
            if (!(block instanceof BlockTask)) {
                return false;
            }
            BlockTask task = (BlockTask) block;
            if (task.getMode() == Mode.READ) {
                throw new IllegalArgumentException("Read task are not supported by UpdateBlockTaskAggregator");
            }
            return task.getMode() == Mode.UPDATE;
        }).collect(Collectors.toCollection(ArrayList::new));
        return new BlockTaskAggregator(updateTasks, readTaskFactory);
    }

    /**
     * Sets the {@code minimumGapSize} that will be used for aggregating the {@link ToplevelBlockTask} tasks
     * in {@link Mode#READ} mode, the {@link ToplevelBlockTask} instances in {@link Mode#WRITE} will always be
     * aggregated with {@code minimumGapSize = 0}.
     */
    @Override
    public void setMinimumGapSize(int minimumGapSize) {
        this.readTaskAggregator.setMinimumGapSize(minimumGapSize);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException
     *             if the provided task is in {@link Mode#READ} mode.
     */
    @Override
    public void addBlock(Block block) {
        boolean isBlockTask = block instanceof BlockTask;
        if (isBlockTask && ((BlockTask) block).getMode() == Mode.READ) {
            throw new IllegalArgumentException("Read task are not supported by UpdateBlockTaskAggregator");
        }
        super.addBlock(block);
        if (block instanceof ProhibitedBlock || isBlockTask && ((BlockTask) block).getMode() == Mode.UPDATE) {
            this.readTaskAggregator.addBlock(block);
        }
    }

    /**
     * Returns a {@link Stream} yielding the result of the aggregation. The returned {@link Stream} will produce the
     * {@link ToplevelBlockTask} instances in {@link Mode#READ} first and then the {@link ToplevelBlockTask} instances
     * in {@link Mode#WRITE} mode.
     */
    @Override
    public Stream<ToplevelBlockTask> stream() {
        return Stream.concat(this.readTaskAggregator.stream(), super.stream());
    }
}
