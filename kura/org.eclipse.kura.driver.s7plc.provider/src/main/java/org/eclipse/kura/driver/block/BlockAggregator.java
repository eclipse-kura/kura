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

package org.eclipse.kura.driver.block;

import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This class can be used to perform an aggregation process over a given list of blocks.
 *
 * The block set resulting from the aggregation always has the following properties:
 * <ul>
 * <li>It does not contain prohibited blocks (see {@link ProhibitedBlock})</li>
 * <li>It does not contain overlapping blocks</li>
 * <li>For each pair of blocks (b1, b2) belonging to the set, {@code b1.getStart() != b2.getEnd()}</li>
 * </ul>
 *
 * This class accepts a parameter, {@code minimumGapSize >= 0}:
 * <ul>
 * <li>If {@code minimumGapSize == 0 || minimumGapSize == 1} the resulting block set will represent the union of the
 * intervals represented by
 * the input block set.</li>
 * <li>If {@code minimumGapSize > 1} two non overlapping input blocks i1 and i2 such that
 * {@code i1.getEnd() < i2.getStart()} might be aggregated into a new block b2 such that
 * {@code b2.getStart() = i1.getStart()} and {@code b2.getEnd() = i2.getEnd()} if {@code i2.getStart() - i1.getEnd()}
 * is lesser than {@code minimumGapSize}</li>
 * </ul>
 *
 * <p>
 * The input block list must not contain conflicting blocks (two overlapping blocks such as one is prohibited and the
 * other is not). If this requirement is not satisfied the aggregation process will fail (see
 * {@link BlockAggregator#stream()}).
 * </p>
 *
 * <p>
 * The input block list can represent for example a list of addresses that need to be read/written from/to a remote
 * device using a specific protocol.
 * If the protocol allows to performs bulk read/writes (for example Modbus allows to read multiple consecutive
 * coils/register using a single request) this class can be used to aggregate a set of multiple "small" requests
 * spanning consecutive addresses into fewer and "larger" requests, reducing I/O time.
 * </p>
 *
 * <p>
 * The {@code minimumGapSize} parameter can be used to aggregate requests involving non consecutive intervals if they
 * are "close enough" according to the above criterion. In this case non explicitly requested data will be
 * transfered.
 * </p>
 *
 * @param <T>
 *            The type of the blocks obtained as result of the aggregation process.
 */
public class BlockAggregator<T extends Block> {

    protected List<Block> blocks;
    private final BlockFactory<T> factory;
    private int minimumGapSize;

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
    public BlockAggregator(List<Block> inputBlocks, BlockFactory<T> factory) {
        requireNonNull(inputBlocks, "Input block list cannot be null");
        requireNonNull(factory, "Block factory cannot be null");
        this.blocks = inputBlocks;
        this.factory = factory;
    }

    /**
     * <p>
     * Sorts the input block list and the returns a {@link Stream} that yields the output blocks obtained from the
     * aggregation process.
     * </p>
     * <p>
     * The aggregation of the input blocks is performed lazily: the output blocks are produced on the fly when a
     * terminal operation is applied to the resulting {@link Stream}.
     * A terminal operation can be the iteration over an {@link Iterator} obtained from the {@link Stream}, or the
     * invocation of the {@link Stream#collect(java.util.stream.Collector)} method.
     * </p>
     * <p>
     * <b>Note</B>: If the input block list contains conflicting blocks an {@link IllegalArgumentException} will be
     * thrown when the stream is consumed as soon as the conflict is detected.
     * </p>
     *
     * @return the resulting {@link Stream}
     */
    @SuppressWarnings("unchecked")
    public Stream<T> stream() {
        this.blocks.sort((Block o1, Block o2) -> o1.getStart() - o2.getStart());
        return (Stream<T>) StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(new AggregatingIterator(this.blocks.listIterator()),
                        Spliterator.ORDERED), false)
                .filter(block -> !(block instanceof ProhibitedBlock));
    }

    /**
     * Specifies the {@code minimumGapSize} parameter. The default for this parameter is 0.
     *
     * @param minimumGapSize
     * @throws IllegalArgumentException
     *             If the provided argument is negative
     */
    public void setMinimumGapSize(int minimumGapSize) {
        if (minimumGapSize < 0) {
            throw new IllegalArgumentException("Minimum gap size paramenter must be non negative");
        }
        this.minimumGapSize = minimumGapSize;
    }

    /**
     * Inserts a new {@link Block} into the input blocks list.
     *
     * @param block
     *            the block to be inserted.
     */
    public void addBlock(Block block) {
        requireNonNull(block, "The provided block cannot be null");
        this.blocks.add(block);
    }

    private class AggregatingIterator implements Iterator<Block> {

        private final ListIterator<Block> source;
        private Block last;

        public AggregatingIterator(ListIterator<Block> source) {
            this.source = source;
        }

        private void extend(Block block, int end) {
            block.setEnd(Math.max(block.getEnd(), end));
        }

        private void getNext() {
            if (!this.source.hasNext()) {
                return;
            }
            this.last = this.source.next();
            if (!(this.last instanceof ProhibitedBlock)) {
                this.last = BlockAggregator.this.factory.build(this.last.getStart(), this.last.getEnd());
            }
            while (this.source.hasNext()) {
                final Block next = this.source.next();
                final boolean isTypeDifferent = this.last instanceof ProhibitedBlock ^ next instanceof ProhibitedBlock;

                if (this.last.getEnd() < next.getStart()) {
                    if (BlockAggregator.this.minimumGapSize > 0
                            && next.getStart() - this.last.getEnd() < BlockAggregator.this.minimumGapSize
                            && !isTypeDifferent) {
                        extend(this.last, next.getEnd());
                        continue;
                    } else {
                        this.source.previous();
                        break;
                    }
                }

                if (this.last.getEnd() > next.getStart() && isTypeDifferent) {
                    throw new IllegalArgumentException("Conflicting blocks: " + this.last + " " + next);
                }

                if (isTypeDifferent) {
                    this.source.previous();
                    break;
                }

                extend(this.last, next.getEnd());
            }
        }

        @Override
        public boolean hasNext() {
            if (this.last != null) {
                return true;
            }
            getNext();
            return this.last != null;
        }

        @Override
        public Block next() {
            if (this.last == null) {
                getNext();
            }
            if (this.last == null) {
                throw new NoSuchElementException();
            }
            Block result = this.last;
            this.last = null;
            return result;
        }

    }
}
