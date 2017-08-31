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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver.ConnectionException;
import org.eclipse.kura.driver.binary.Buffer;
import org.eclipse.kura.driver.block.BlockFactory;
import org.eclipse.kura.driver.block.ProhibitedBlock;
import org.eclipse.kura.driver.block.task.AbstractBlockDriver;
import org.eclipse.kura.driver.block.task.AbstractBlockDriver.Pair;
import org.eclipse.kura.driver.block.task.BlockTask;
import org.eclipse.kura.driver.block.task.BlockTaskAggregator;
import org.eclipse.kura.driver.block.task.Mode;
import org.eclipse.kura.driver.block.task.ToplevelBlockTask;
import org.eclipse.kura.driver.block.task.UpdateBlockTask;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValues;
import org.junit.Test;

public class AbstractBlockDriverTest {

    @Test
    public void shouldAggregateTasksFromSameDomain() throws ConnectionException {
        List<Pair<Integer, BlockTask>> tasks = testTasks(1, Mode.READ, 0, 3, 3, 5, 5, 10);
        List<ChannelRecord> records = getRecords(tasks);
        TestBlockFactory factory = new TestBlockFactory(Mode.READ, 0, 10);
        TestDriver driver = new TestDriver().withTasks(tasks).withBlockFactoryProvider((domain, mode) -> factory);
        driver.read(records);
        assertEquals(1, factory.timesCalled);
        for (ChannelRecord record : records) {
            assertEquals(true, record.getValue().getValue());
        }
    }

    @Test
    public void shouldSupportPreparedRead() throws ConnectionException, KuraException {
        List<Pair<Integer, BlockTask>> tasks = testTasks(1, Mode.READ, 0, 3, 3, 5, 5, 10);
        List<ChannelRecord> records = getRecords(tasks);
        TestBlockFactory factory = new TestBlockFactory(Mode.READ, 0, 10);
        TestDriver driver = new TestDriver().withTasks(tasks).withBlockFactoryProvider((domain, mode) -> factory);
        driver.prepareRead(records).execute();
        assertEquals(1, factory.timesCalled);
        for (ChannelRecord record : records) {
            assertEquals(true, record.getValue().getValue());
        }
    }

    @Test
    public void shouldSupportWrite() throws ConnectionException, KuraException {
        List<Pair<Integer, BlockTask>> tasks = testTasks(1, Mode.WRITE, 0, 3, 3, 5, 5, 10);
        List<ChannelRecord> records = getRecords(tasks);
        TestBlockFactory factory = new TestBlockFactory(Mode.WRITE, 0, 10);
        TestDriver driver = new TestDriver().withTasks(tasks).withBlockFactoryProvider((domain, mode) -> factory);
        driver.write(records);
        assertEquals(1, factory.timesCalled);
        for (ChannelRecord record : records) {
            assertEquals(true, record.getValue().getValue());
        }
    }

    @Test
    public void shouldReportUnfeasibleProblemOnRead() throws ConnectionException {
        List<Pair<Integer, BlockTask>> tasks = testTasks(1, Mode.READ, 0, 4, 4, 6);
        List<ChannelRecord> records = getRecords(tasks);
        TestBlockFactory factory = new TestBlockFactory(Mode.READ, 0, 10);
        TestDriver driver = new TestDriver().withTasks(tasks).withBlockFactoryProvider((domain, mode) -> factory)
                .beforeAggregation((aggregator) -> aggregator.addBlock(new ProhibitedBlock(5, 10)));
        driver.read(records);
        assertEquals(1, factory.timesCalled);
        for (ChannelRecord record : records) {
            assertEquals(ChannelFlag.FAILURE, record.getChannelStatus().getChannelFlag());
        }
    }

    @Test
    public void shouldReportUnfeasibleProblemOnPrepareRead() throws ConnectionException, KuraException {
        List<Pair<Integer, BlockTask>> tasks = testTasks(1, Mode.READ, 0, 4, 4, 6);
        List<ChannelRecord> records = getRecords(tasks);
        TestBlockFactory factory = new TestBlockFactory(Mode.READ, 0, 10);
        TestDriver driver = new TestDriver().withTasks(tasks).withBlockFactoryProvider((domain, mode) -> factory)
                .beforeAggregation((aggregator) -> aggregator.addBlock(new ProhibitedBlock(5, 10)));
        driver.prepareRead(records).execute();
        assertEquals(1, factory.timesCalled);
        for (ChannelRecord record : records) {
            assertEquals(ChannelFlag.FAILURE, record.getChannelStatus().getChannelFlag());
        }
    }

    @Test
    public void shouldReportUnfeasibleProblemOnWrite() throws ConnectionException, KuraException {
        List<Pair<Integer, BlockTask>> tasks = testTasks(1, Mode.WRITE, 0, 4, 4, 6);
        List<ChannelRecord> records = getRecords(tasks);
        TestBlockFactory factory = new TestBlockFactory(Mode.WRITE, 0, 10);
        TestDriver driver = new TestDriver().withTasks(tasks).withBlockFactoryProvider((domain, mode) -> factory)
                .beforeAggregation((aggregator) -> aggregator.addBlock(new ProhibitedBlock(5, 10)));
        driver.write(records);
        assertEquals(1, factory.timesCalled);
        for (ChannelRecord record : records) {
            assertEquals(ChannelFlag.FAILURE, record.getChannelStatus().getChannelFlag());
        }
    }

    @Test
    public void shouldSupportMinimumGapSize() throws ConnectionException {
        List<Pair<Integer, BlockTask>> tasks = testTasks(1, Mode.READ, 0, 3, 5, 7, 9, 12);
        List<ChannelRecord> records = getRecords(tasks);
        TestBlockFactory factory = new TestBlockFactory(Mode.READ, 0, 12);
        TestDriver driver = new TestDriver().withTasks(tasks).withBlockFactoryProvider((domain, mode) -> factory)
                .withMinimumGapSize(3);
        driver.read(records);
        assertEquals(1, factory.timesCalled);
        for (ChannelRecord record : records) {
            assertEquals(true, record.getValue().getValue());
        }
    }

    @Test
    public void shouldNotAggregateTasksFromDifferentDomains() throws ConnectionException {
        List<Pair<Integer, BlockTask>> tasks = new ArrayList<>();
        tasks.addAll(testTasks(1, Mode.READ, 0, 3, 3, 5));
        tasks.addAll(testTasks(2, Mode.READ, 4, 6, 6, 10));
        List<ChannelRecord> records = getRecords(tasks);
        TestBlockFactory factory1 = new TestBlockFactory(Mode.READ, 0, 5);
        TestBlockFactory factory2 = new TestBlockFactory(Mode.READ, 4, 10);
        TestDriver driver = new TestDriver().withTasks(tasks).withBlockFactoryProvider((domain, mode) -> {
            if (domain == 1) {
                return factory1;
            } else if (domain == 2) {
                return factory2;
            }
            return null;
        }).afterAggregation((result) -> assertEquals(2, result.size()));
        driver.read(records);
        assertEquals(1, factory1.timesCalled);
        assertEquals(1, factory2.timesCalled);
        for (ChannelRecord record : records) {
            assertEquals(true, record.getValue().getValue());
        }
    }

    @Test
    public void shouldSupportUpdateTasks() throws ConnectionException {
        List<Pair<Integer, BlockTask>> tasks = testTasks(1, Mode.UPDATE, 0, 3, 3, 5, 5, 10);
        List<ChannelRecord> records = getRecords(tasks);
        TestBlockFactory readFactory = new TestBlockFactory(Mode.READ, 0, 10);
        TestBlockFactory writeFactory = new TestBlockFactory(Mode.WRITE, 0, 10);
        TestDriver driver = new TestDriver().withTasks(tasks).withBlockFactoryProvider((domain, mode) -> {
            if (mode == Mode.READ) {
                return readFactory;
            } else if (mode == Mode.WRITE) {
                return writeFactory;
            }
            return null;
        });
        driver.write(records);
        assertEquals(1, readFactory.timesCalled);
        assertEquals(1, writeFactory.timesCalled);
        for (ChannelRecord record : records) {
            assertEquals(true, record.getValue().getValue());
        }
    }

    private List<Pair<Integer, BlockTask>> testTasks(int domain, Mode mode, int... ranges) {
        assertTrue(ranges.length % 2 == 0);
        List<Pair<Integer, BlockTask>> result = new ArrayList<>(ranges.length / 2);
        for (int i = 0; i < ranges.length; i += 2) {
            result.add(new Pair<>(domain, new TestTask(ranges[i], ranges[i + 1], mode)));
        }
        return result;
    }

    private List<ChannelRecord> getRecords(List<Pair<Integer, BlockTask>> tasks) {
        return tasks.stream().map(pair -> ((TestTask) pair.getSecond()).getRecord()).collect(Collectors.toList());
    }

    private class TestBlockFactory implements BlockFactory<ToplevelBlockTask> {

        private final int expectedStart;
        private final int expectedEnd;
        private int timesCalled;
        private final Mode mode;

        public TestBlockFactory(Mode mode, int expectedStart, int expectedEnd) {
            this.expectedStart = expectedStart;
            this.expectedEnd = expectedEnd;
            this.mode = mode;
        }

        @Override
        public ToplevelBlockTask build(int start, int end) {
            this.timesCalled++;
            return new ToplevelBlockTask(start, end, this.mode) {

                @Override
                public void processBuffer() throws IOException {
                    assertEquals(TestBlockFactory.this.expectedStart, getStart());
                    assertEquals(TestBlockFactory.this.expectedEnd, getEnd());
                }

                @Override
                public Buffer getBuffer() {
                    return null;
                }
            };
        }

    }

    private class TestTask extends UpdateBlockTask {

        public TestTask(int start, int end, Mode mode) {
            super(ChannelRecord.createReadRecord("test", DataType.BOOLEAN), start, end, mode);
        }

        public ChannelRecord getRecord() {
            return this.record;
        }

        @Override
        protected void runRead() {
            final ToplevelBlockTask parent = getParent();
            assertNotNull(parent);
            assertTrue(parent.getStart() <= getStart());
            assertTrue(parent.getEnd() >= getEnd());
            this.record.setValue(TypedValues.newBooleanValue(true));
        }

        @Override
        protected void runWrite() {
            final ToplevelBlockTask parent = getParent();
            assertNotNull(parent);
            assertTrue(parent.getStart() <= getStart());
            assertTrue(parent.getEnd() >= getEnd());
            this.record.setValue(TypedValues.newBooleanValue(true));
        }

        @Override
        protected void runUpdate(ToplevelBlockTask write, ToplevelBlockTask read) {
            assertTrue(read != write);
            assertTrue(read.getStart() <= getStart());
            assertTrue(read.getEnd() >= getEnd());
            assertTrue(write.getStart() <= getStart());
            assertTrue(write.getEnd() >= getEnd());
            this.record.setValue(TypedValues.newBooleanValue(true));
        }

    }

    private class TestDriver extends AbstractBlockDriver<Integer> {

        private List<Pair<Integer, BlockTask>> tasks;
        private BiFunction<Integer, Mode, BlockFactory<ToplevelBlockTask>> blockFactoryProvider;
        private Consumer<List<BlockTask>> afterAggregation;
        private Consumer<BlockTaskAggregator> beforeAggregation;
        private int minimumGapSize;

        public TestDriver withTasks(List<Pair<Integer, BlockTask>> tasks) {
            this.tasks = tasks;
            return this;
        }

        public TestDriver withMinimumGapSize(int minimumGapSize) {
            this.minimumGapSize = minimumGapSize;
            return this;
        }

        public TestDriver withBlockFactoryProvider(
                BiFunction<Integer, Mode, BlockFactory<ToplevelBlockTask>> blockFactoryProvider) {
            this.blockFactoryProvider = blockFactoryProvider;
            return this;
        }

        public TestDriver afterAggregation(Consumer<List<BlockTask>> afterAggregation) {
            this.afterAggregation = afterAggregation;
            return this;
        }

        public TestDriver beforeAggregation(Consumer<BlockTaskAggregator> beforeAggregation) {
            this.beforeAggregation = beforeAggregation;
            return this;
        }

        @Override
        protected int getReadMinimumGapSizeForDomain(Integer domain) {
            return this.minimumGapSize;
        }

        @Override
        protected void beforeAggregation(Integer domain, Mode mode, BlockTaskAggregator aggregator) {
            if (this.beforeAggregation != null) {
                this.beforeAggregation.accept(aggregator);
            }
        }

        @Override
        protected List<BlockTask> optimize(List<ChannelRecord> records, Mode mode) throws KuraException {
            List<BlockTask> result = super.optimize(records, mode);
            if (this.afterAggregation != null) {
                this.afterAggregation.accept(result);
            }
            return result;
        }

        @Override
        public void connect() throws ConnectionException {
        }

        @Override
        public void disconnect() throws ConnectionException {
        }

        @Override
        public ChannelDescriptor getChannelDescriptor() {
            return null;
        }

        @Override
        protected BlockFactory<ToplevelBlockTask> getTaskFactoryForDomain(Integer domain, Mode mode) {
            return this.blockFactoryProvider.apply(domain, mode);
        }

        @Override
        protected Stream<Pair<Integer, BlockTask>> toTasks(List<ChannelRecord> records, Mode mode) {
            return this.tasks.stream();
        }

    }
}
