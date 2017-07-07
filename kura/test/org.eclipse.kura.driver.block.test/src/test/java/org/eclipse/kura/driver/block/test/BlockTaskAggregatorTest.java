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

package org.eclipse.kura.driver.block.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.kura.driver.binary.Buffer;
import org.eclipse.kura.driver.block.Block;
import org.eclipse.kura.driver.block.BlockFactory;
import org.eclipse.kura.driver.block.ProhibitedBlock;
import org.eclipse.kura.driver.block.task.BlockTask;
import org.eclipse.kura.driver.block.task.BlockTaskAggregator;
import org.eclipse.kura.driver.block.task.Mode;
import org.eclipse.kura.driver.block.task.ToplevelBlockTask;
import org.eclipse.kura.driver.block.task.UpdateBlockTaskAggregator;
import org.junit.Assert;
import org.junit.Test;

public class BlockTaskAggregatorTest {

    @Test
    public void shouldSupportSingleTask() {
        new TestHelper().setInput(2, 5).expect(2, 5).exec();
    }

    @Test
    public void shouldSupportSingleProhibitedTask() {
        new TestHelper().prohibit(2, 5).expect().exec();
    }

    @Test
    public void shouldOptimizeAdjacentBlocks() {
        new TestHelper().setInput(0, 1, 1, 2, 2, 3).expect(0, 3).exec();
    }

    @Test
    public void shouldSupportBlockInsertion() {
        new TestHelper().setInput(0, 1).expect(0, 1).exec();
    }

    @Test
    public void shouldAggregateAdjacentBlocks() {
        new TestHelper().setInput(0, 1, 1, 2).expect(0, 2).exec();
        new TestHelper().setInput(1, 2, 0, 1).expect(0, 2).exec();
        new TestHelper().setInput(0, 1, 1, 2, 2, 10).expect(0, 10).exec();
        new TestHelper().setInput(2, 10, 1, 2, 0, 1).expect(0, 10).exec();
        new TestHelper().setInput(0, 1, 1, 2, 3, 4, 2, 3).expect(0, 4).exec();
        new TestHelper().setInput(0, 4, 4, 8, 8, 10, 10, 12).expect(0, 12).exec();
        new TestHelper().setInput(0, 4, 4, 8, 10, 12, 12, 14).expect(0, 8, 10, 14).exec();
    }

    @Test
    public void shouldSupportNonSortedList() {
        new TestHelper().setInput(10, 12, 12, 14, 4, 8, 0, 4).expect(0, 8, 10, 14).exec();
    }

    @Test
    public void shouldNotAggregateNonAdjacentBlocks() {
        new TestHelper().setInput(0, 1, 2, 3).expect(0, 1, 2, 3).exec();
    }

    @Test
    public void shouldSupportOverlappingBlocks() {
        new TestHelper().setInput(1, 9, 2, 4).expect(1, 9).exec();
        new TestHelper().setInput(1, 9, 2, 4, 3, 6).expect(1, 9).exec();
        new TestHelper().setInput(0, 1, 0, 1, 0, 1).expect(0, 1).exec();
        new TestHelper().setInput(0, 1, 1, 3, 20, 25, 15, 21).expect(0, 3, 15, 25).exec();
    }

    @Test
    public void shouldSupportProhibitedBlocks() {
        new TestHelper().prohibit(0, 1).expect().exec();
        new TestHelper().setInput(0, 1, 1, 2, 3, 4).prohibit(2, 3).expect(0, 2, 3, 4).exec();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldReportUnfeasibleProblem1() {
        new TestHelper().setInput(0, 2).prohibit(0, 1).exec();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldReportUnfeasibleProblem2() {
        new TestHelper().setInput(0, 2).prohibit(1, 2).exec();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldReportUnfeasibleProblem3() {
        new TestHelper().setInput(0, 2).prohibit(1, 3).exec();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldReportUnfeasibleProblem4() {
        new TestHelper().setInput(0, 2, 3, 4, 8, 10).prohibit(4, 9).exec();
    }

    @Test
    public void shouldAggregateAccordingToMinimumGapSize() {
        new TestHelper().setInput(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).expect(0, 9).setMinimumGapSize(2).exec();
        new TestHelper().setInput(0, 1, 2, 3, 6, 7, 8, 9).expect(0, 3, 6, 9).setMinimumGapSize(2).exec();
    }

    @Test
    public void shouldSupportProhibitedBlocksWithMinimumGapSize() {
        new TestHelper().setInput(0, 1, 2, 3, 8, 9).expect(0, 9).setMinimumGapSize(10).exec();
        new TestHelper().setInput(0, 1, 2, 3, 8, 9).prohibit(4, 7).expect(0, 3, 8, 9).setMinimumGapSize(10).exec();
    }

    @Test
    public void shouldSupportUpdateTask() {
        new TestHelper().setInput(0, 1, 8, 9).setUpdate(1, 3).expect(1, 3, 0, 3, 8, 9).exec();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldSupportProhibitedBlocksWithUpdateTask() {
        new TestHelper().setInput(0, 1, 8, 9).setUpdate(1, 3).prohibit(2, 3).exec();
    }

    @Test
    public void shouldSupportMinimumGapSizeWithUpdateTask() {
        new TestHelper().setInput(0, 1, 8, 9).setUpdate(2, 3, 4, 5).setMinimumGapSize(6)
                .expect(2, 5, 0, 1, 2, 3, 4, 5, 8, 9).exec();
    }

    private interface TaskListener {

        public void onRun(BlockTask task, BlockTask parent);

        public void onFail(BlockTask task, BlockTask parent);
    }

    private class TestTask extends ToplevelBlockTask {

        private TaskListener listener;

        public TestTask(int start, int end, TaskListener listener) {
            super(start, end, Mode.WRITE);
            this.listener = listener;
        }

        public TestTask(int start, int end, Mode mode, TaskListener listener) {
            super(start, end, mode);
            this.listener = listener;
        }

        @Override
        public void processBuffer() {
        }

        @Override
        public void onFailure(Exception reason) {
            if (listener != null) {
                listener.onFail(this, getParent());
            }
            super.onFailure(reason);
        }

        public Buffer getBuffer() {
            return null;
        }
    }

    private class TestHelper {

        private TaskListener taskListener = new TaskListener() {

            @Override
            public void onRun(BlockTask task, BlockTask parent) {
                if (parent != null) {
                    Assert.assertTrue(task.getStart() >= parent.getStart());
                    Assert.assertTrue(task.getEnd() <= parent.getEnd());
                }
            }

            @Override
            public void onFail(BlockTask task, BlockTask parent) {
                Assert.fail();
            }
        };

        private int[] updateBlocks = new int[0];
        private int[] inputBlocks = new int[0];
        private int[] prohibitedBlocks = new int[0];
        private int[] outputBlocks;
        private int minimumGapSize = 0;

        private TestHelper() {
        }

        public TestHelper setInput(int... inputBlocks) {
            if (inputBlocks.length % 2 != 0) {
                fail("block list size must be a multiple of 2");
            }
            this.inputBlocks = inputBlocks;
            return this;
        }

        public TestHelper setUpdate(int... updateBlocks) {
            if (inputBlocks.length % 2 != 0) {
                fail("block list size must be a multiple of 2");
            }
            this.updateBlocks = updateBlocks;
            return this;
        }

        public TestHelper expect(int... outputBlocks) {
            if (outputBlocks.length % 2 != 0) {
                fail("block list size must be a multiple of 2");
            }
            this.outputBlocks = outputBlocks;
            return this;
        }

        public TestHelper prohibit(int... prohibitedBlocks) {
            if (prohibitedBlocks.length % 2 != 0) {
                fail("block list size must be a multiple of 2");
            }
            this.prohibitedBlocks = prohibitedBlocks;
            return this;
        }

        public TestHelper setMinimumGapSize(int minimumGapSize) {
            this.minimumGapSize = minimumGapSize;
            return (this);
        }

        public TestHelper exec() {
            List<Block> tasks = new ArrayList<Block>();
            for (int i = 0; i < inputBlocks.length; i += 2) {
                tasks.add(new TestTask(inputBlocks[i], inputBlocks[i + 1], taskListener));
            }
            for (int i = 0; i < updateBlocks.length; i += 2) {
                tasks.add(new TestTask(updateBlocks[i], updateBlocks[i + 1], Mode.UPDATE, taskListener));
            }

            BlockTaskAggregator aggregator;

            BlockFactory<ToplevelBlockTask> factory = (start, end) -> new TestTask(start, end, taskListener);

            if (updateBlocks.length == 0) {
                aggregator = new BlockTaskAggregator(tasks, factory);
            } else {
                aggregator = new UpdateBlockTaskAggregator(tasks, factory, factory);
            }

            aggregator.setMinimumGapSize(minimumGapSize);
            for (int i = 0; i < prohibitedBlocks.length; i += 2) {
                aggregator.addBlock(new ProhibitedBlock(prohibitedBlocks[i], prohibitedBlocks[i + 1]));
            }

            final Iterator<ToplevelBlockTask> blocks = aggregator.stream().iterator();

            int childCount = 0;
            if (outputBlocks != null) {
                int i = 0;
                while (blocks.hasNext()) {
                    final ToplevelBlockTask next = blocks.next();
                    assertEquals(outputBlocks[i * 2], next.getStart());
                    assertEquals(outputBlocks[i * 2 + 1], next.getEnd());
                    childCount += next.getChildren().size();
                    try {
                        next.run();
                    } catch (IOException e) {
                    }
                    i++;
                }
                assertEquals(outputBlocks.length / 2, i);
                assertEquals(inputBlocks.length / 2 + updateBlocks.length, childCount);
            } else {
                // needed for the shouldReportUnfeasibleProblem tests
                for (; blocks.hasNext(); blocks.next())
                    ;
            }

            return this;
        }
    }

}
