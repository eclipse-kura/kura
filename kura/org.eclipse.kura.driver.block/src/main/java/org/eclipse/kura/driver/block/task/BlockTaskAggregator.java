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

import java.util.List;
import java.util.ListIterator;
import java.util.stream.Stream;

import org.eclipse.kura.driver.block.Block;
import org.eclipse.kura.driver.block.BlockAggregator;
import org.eclipse.kura.driver.block.BlockFactory;

/**
 * <p>
 * Represents a specialized version of {@link BlockAggregator} that operates on {@link BlockTask} instances.
 * This class performs the aggregation of the input blocks in the same way as {@link BlockAggregator} does, and always
 * returns {@link ToplevelBlockTask} instances as result of the aggregation.
 * </p>
 *
 * <p>
 * If any {@link BlockTask} is found in the input block list, it will be assigned to the proper parent task in the
 * result list, basing on the start and end addresses of the two tasks.
 * <p>
 *
 * <p>
 * The result of the aggregation performed by this class is therefore a generally two level tree structure: the first
 * level of
 * the tree is composed by {@link ToplevelBlockTask} instances responsible of managing a data buffer, the second level
 * is composed by {@link BlockTask} instances that perform an operation using the buffer of the parent. See
 * {@link ToplevelBlockTask} for more information on this structure.
 * <p>
 *
 * @see ToplevelBlockTask
 * @see BlockTask
 */
public class BlockTaskAggregator extends BlockAggregator<ToplevelBlockTask> {

    /**
     * Creates a new {@link BlockAggregator} instance that operates on the given list of blocks.
     * The provided list must be mutable as it will be sorted every time the {@link BlockAggregator#stream()} method is
     * called.
     *
     * @param inputBlocks
     *            a mutable list of input blocks.
     * @param factory
     *            a {@link BlockFactory} instance that will be used to create the output blocks during the aggregation
     *            process.
     */
    public BlockTaskAggregator(List<Block> tasks, BlockFactory<ToplevelBlockTask> factory) {
        super(tasks, factory);
    }

    private void assignTasks(ToplevelBlockTask toplevelTask, ListIterator<Block> tasks) {
        Block next = null;
        while (tasks.hasNext() && (next = tasks.next()).getEnd() <= toplevelTask.getEnd()) {
            if (next instanceof BlockTask) {
                toplevelTask.addChild((BlockTask) next);
            }
        }
        if (next != null) {
            tasks.previous();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<ToplevelBlockTask> stream() {
        final Stream<ToplevelBlockTask> result = super.stream();
        final ListIterator<Block> blocks = this.blocks.listIterator();
        return result.map(toplevelTask -> {
            assignTasks(toplevelTask, blocks);
            return toplevelTask;
        });
    }

}
