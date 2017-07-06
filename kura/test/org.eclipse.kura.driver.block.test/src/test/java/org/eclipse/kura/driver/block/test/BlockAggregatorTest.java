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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.kura.driver.block.Block;
import org.eclipse.kura.driver.block.BlockAggregator;
import org.eclipse.kura.driver.block.ProhibitedBlock;
import org.junit.Test;

public class BlockAggregatorTest {

    @Test
    public void shouldSupportEmptyList() {
        assertEquals(false, new BlockAggregator<Block>(Collections.emptyList(), (start, end) -> new Block(start, end))
                .stream().iterator().hasNext());
    }

    @Test
    public void shouldSupportSingleBlock() {
        new TestHelper().setInput(2, 5).expect(2, 5).exec();
    }

    @Test
    public void shouldSupportSingleProhibitedBlock() {
        new TestHelper().prohibit(2, 5).expect().exec();
    }

    @Test
    public void shouldAggregateAdjacentBlocks() {
        new TestHelper().setInput(0, 1, 1, 2).expect(0, 2).exec();
        new TestHelper().setInput(1, 2, 0, 1).expect(0, 2).exec();
        new TestHelper().setInput(0, 1, 1, 2, 2, 10).expect(0, 10).exec();
        new TestHelper().setInput(2, 10, 1, 2, 0, 1).expect(0, 10).exec();
        new TestHelper().setInput(0, 1, 1, 2, 3, 4, 2, 3).expect(0, 4).exec();
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
        new TestHelper().setInput(0, 2, 3, 4, 8, 10).prohibit(3, 9).exec();
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

    private static class TestHelper {

        private int[] inputBlocks;
        private int[] prohibitedBlocks;
        private int[] outputBlocks;
        private int minimumGapSize;
        BlockAggregator<Block> aggregator;

        private TestHelper() {
        }

        public TestHelper setInput(int... inputBlocks) {
            if (inputBlocks.length % 2 != 0) {
                fail("block list size must be a multiple of 2");
            }
            this.inputBlocks = inputBlocks;
            return this;
        }

        public TestHelper expect(int... outputBlocks) {
            if (outputBlocks.length % 2 != 0) {
                fail("block list size must be a multiple of 2");
            }
            this.outputBlocks = outputBlocks;
            return this;
        }

        public TestHelper setMinimumGapSize(int minimumGapSize) {
            this.minimumGapSize = minimumGapSize;
            return (this);
        }

        public TestHelper prohibit(int... prohibitedBlocks) {
            if (prohibitedBlocks.length % 2 != 0) {
                fail("block list size must be a multiple of 2");
            }
            this.prohibitedBlocks = prohibitedBlocks;
            return this;
        }

        public TestHelper exec() {

            List<Block> inputBlocksTemp = new ArrayList<Block>();
            if (inputBlocks != null) {
                for (int i = 0; i < inputBlocks.length; i += 2) {
                    inputBlocksTemp.add(new Block(inputBlocks[i], inputBlocks[i + 1]));
                }
            }
            if (prohibitedBlocks != null) {
                for (int i = 0; i < prohibitedBlocks.length; i += 2) {
                    inputBlocksTemp.add(new ProhibitedBlock(prohibitedBlocks[i], prohibitedBlocks[i + 1]));
                }
            }
            aggregator = new BlockAggregator<Block>(inputBlocksTemp, (start, end) -> new Block(start, end));
            aggregator.setMinimumGapSize(minimumGapSize);
            Iterator<Block> blocks = aggregator.stream().iterator();

            if (outputBlocks != null) {
                int i = 0;
                while (blocks.hasNext()) {
                    final Block next = blocks.next();
                    assertEquals(outputBlocks[i * 2], next.getStart());
                    assertEquals(outputBlocks[i * 2 + 1], next.getEnd());
                    i++;
                }
                assertEquals(outputBlocks.length / 2, i);
            } else {
                // needed for the shouldReportUnfeasibleProblem tests
                for (; blocks.hasNext(); blocks.next())
                    ;
            }

            return this;
        }
    }

}
