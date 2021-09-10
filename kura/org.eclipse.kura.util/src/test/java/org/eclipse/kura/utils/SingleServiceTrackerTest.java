/*******************************************************************************
 * Copyright (c) 2018, 2020 Red Hat Inc and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Red Hat Inc
 *  Stefan Bischof
 *******************************************************************************/

package org.eclipse.kura.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.function.Consumer;

import org.eclipse.kura.util.osgi.SingleServiceTracker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
public class SingleServiceTrackerTest {

    private static final class MockConsumer<T> implements Consumer<T> {

        private T service;

        @Override
        public void accept(T t) {
            this.service = t;
        }

        public T getService() {
            return service;
        }
    }

    private static final class MockService {
    }

    @InjectBundleContext
    BundleContext bc;

    @Test
    public void testInitOrder1() {

        final MockConsumer<MockService> consumer = new MockConsumer<>();
        final SingleServiceTracker<MockService> tracker = new SingleServiceTracker<>(this.bc, MockService.class,
                consumer);

        assertThat(consumer.getService()).isNull();

        final MockService service1 = new MockService();
        final MockService service2 = new MockService();

        final ServiceRegistration<MockService> handle2 = bc.registerService(MockService.class, service2,
                withRanking(2));
        final ServiceRegistration<MockService> handle1 = bc.registerService(MockService.class, service1,
                withRanking(1));

        tracker.open();
        assertThat(service2).isEqualTo(consumer.getService());
        tracker.close();

        assertThat(consumer.getService()).isNull();

    }

    @Test
    public void testOrderRemove1() {

        final BundleContext context = FrameworkUtil.getBundle(SingleServiceTrackerTest.class).getBundleContext();

        final MockConsumer<MockService> consumer = new MockConsumer<>();
        final SingleServiceTracker<MockService> tracker = new SingleServiceTracker<>(context, MockService.class,
                consumer);

        assertThat(consumer.getService()).isNull();

        final MockService service1 = new MockService();
        final MockService service2 = new MockService();

        final ServiceRegistration<MockService> handle2 = bc.registerService(MockService.class, service2,
                withRanking(2));
        final ServiceRegistration<MockService> handle1 = bc.registerService(MockService.class, service1,
                withRanking(1));

        tracker.open();
        assertThat(service2).isEqualTo(consumer.getService());
        handle2.unregister();
        assertThat(service1).isEqualTo(consumer.getService());
        tracker.close();

        assertThat(consumer.getService()).isNull();

    }

    @Test
    public void testOrderAdd1() {

        final BundleContext context = FrameworkUtil.getBundle(SingleServiceTrackerTest.class).getBundleContext();

        final MockConsumer<MockService> consumer = new MockConsumer<>();
        final SingleServiceTracker<MockService> tracker = new SingleServiceTracker<>(context, MockService.class,
                consumer);

        assertThat(consumer.getService()).isNull();

        final MockService service1 = new MockService();
        final MockService service2 = new MockService();

        final ServiceRegistration<MockService> handle1 = bc.registerService(MockService.class, service1,
                withRanking(1));

        tracker.open();
        assertThat(service1).isEqualTo(consumer.getService());

        final ServiceRegistration<MockService> handle2 = bc.registerService(MockService.class, service2,
                withRanking(2));

        assertThat(service2).isEqualTo(consumer.getService());
        tracker.close();

        assertThat(consumer.getService()).isNull();

    }

    /**
     * Add new service with same ranking
     */
    @Test
    public void testOrderAdd2() {

        final BundleContext context = FrameworkUtil.getBundle(SingleServiceTrackerTest.class).getBundleContext();

        final MockConsumer<MockService> consumer = new MockConsumer<>();
        final SingleServiceTracker<MockService> tracker = new SingleServiceTracker<>(context, MockService.class,
                consumer);

        assertThat(consumer.getService()).isNull();

        final MockService service1 = new MockService();
        final MockService service2 = new MockService();

        final ServiceRegistration<MockService> handle1 = bc.registerService(MockService.class, service1,
                withRanking(1));

        tracker.open();
        assertThat(service1).isEqualTo(consumer.getService());

        final ServiceRegistration<MockService> handle2 = bc.registerService(MockService.class, service2,
                withRanking(1));

        assertThat(service1).isEqualTo(consumer.getService());
        tracker.close();

        assertThat(consumer.getService()).isNull();

    }

    /**
     * Add new service with a lower ranking
     */
    @Test
    public void testOrderAdd3() {

        final BundleContext context = FrameworkUtil.getBundle(SingleServiceTrackerTest.class).getBundleContext();

        final MockConsumer<MockService> consumer = new MockConsumer<>();
        final SingleServiceTracker<MockService> tracker = new SingleServiceTracker<>(context, MockService.class,
                consumer);

        assertThat(consumer.getService()).isNull();

        final MockService service1 = new MockService();
        final MockService service2 = new MockService();
        final MockService service3 = new MockService();

        final ServiceRegistration<MockService> handle1 = bc.registerService(MockService.class, service1,
                withRanking(null));
        final ServiceRegistration<MockService> handle2 = bc.registerService(MockService.class, service2,
                withRanking(null));

        tracker.open();
        assertThat(service1).isEqualTo(consumer.getService());

        final ServiceRegistration<MockService> handle3 = bc.registerService(MockService.class, service3,
                withRanking(-1));

        assertThat(service1).isEqualTo(consumer.getService());
        tracker.close();

        assertThat(consumer.getService()).isNull();

    }

    @Test
    public void testOrderModify1() {

        final BundleContext context = FrameworkUtil.getBundle(SingleServiceTrackerTest.class).getBundleContext();

        final MockConsumer<MockService> consumer = new MockConsumer<>();
        final SingleServiceTracker<MockService> tracker = new SingleServiceTracker<>(context, MockService.class,
                consumer);

        assertThat(consumer.getService()).isNull();

        final MockService service1 = new MockService();
        final MockService service2 = new MockService();

        final ServiceRegistration<MockService> handle2 = bc.registerService(MockService.class, service2,
                withRanking(2));
        final ServiceRegistration<MockService> handle1 = bc.registerService(MockService.class, service1,
                withRanking(1));

        tracker.open();
        assertThat(service2).isEqualTo(consumer.getService());
        handle1.setProperties(withRanking(3));
        assertThat(service1).isEqualTo(consumer.getService());
        tracker.close();

        assertThat(consumer.getService()).isNull();

    }

    @Test
    public void testOrderModify2() {

        final BundleContext context = FrameworkUtil.getBundle(SingleServiceTrackerTest.class).getBundleContext();

        final MockConsumer<MockService> consumer = new MockConsumer<>();
        final SingleServiceTracker<MockService> tracker = new SingleServiceTracker<>(context, MockService.class,
                consumer);

        assertThat(consumer.getService()).isNull();

        final MockService service1 = new MockService();
        final MockService service2 = new MockService();

        final ServiceRegistration<MockService> handle1 = bc.registerService(MockService.class, service1,
                withRanking(null));
        final ServiceRegistration<MockService> handle2 = bc.registerService(MockService.class, service2,
                withRanking(2));

        tracker.open();
        assertThat(service2).isEqualTo(consumer.getService());
        handle1.setProperties(withRanking(3));
        assertThat(service1).isEqualTo(consumer.getService());
        tracker.close();

        assertThat(consumer.getService()).isNull();

    }

    @Test
    public void testOrderModify3() throws InterruptedException {

        final BundleContext context = FrameworkUtil.getBundle(SingleServiceTrackerTest.class).getBundleContext();

        final MockConsumer<MockService> consumer = new MockConsumer<>();
        final SingleServiceTracker<MockService> tracker = new SingleServiceTracker<>(context, MockService.class,
                consumer);

        assertThat(consumer.getService()).isNull();

        final MockService service1 = new MockService();
        final MockService service2 = new MockService();

        final ServiceRegistration<MockService> handle1 = bc.registerService(MockService.class, service1,
                withRanking(1));
        final ServiceRegistration<MockService> handle2 = bc.registerService(MockService.class, service2,
                withRanking(1));

        tracker.open();

        // expect the first registered service
        assertThat(service1).isEqualTo(consumer.getService());

        // update, but don't change ranking
        handle1.setProperties(withRanking(1));

        // should still be the same service
        assertThat(service1).isEqualTo(consumer.getService());

        // change ranking

        handle2.setProperties(withRanking(2));
        assertThat(service2).isEqualTo(consumer.getService());

        tracker.close();

        assertThat(consumer.getService()).isNull();

    }

    /**
     * Add new service with same ranking
     */
    @Test
    public void testOrder1() {

        final BundleContext context = FrameworkUtil.getBundle(SingleServiceTrackerTest.class).getBundleContext();

        final MockConsumer<MockService> consumer = new MockConsumer<>();
        final SingleServiceTracker<MockService> tracker = new SingleServiceTracker<>(context, MockService.class,
                consumer);

        assertThat(consumer.getService()).isNull();

        final MockService service1 = new MockService();
        final MockService service2 = new MockService();
        final MockService service3 = new MockService();

        final ServiceRegistration<MockService> handle1 = bc.registerService(MockService.class, service1,
                withRanking(1));
        final ServiceRegistration<MockService> handle2 = bc.registerService(MockService.class, service2,
                withRanking(1));
        final ServiceRegistration<MockService> handle3 = bc.registerService(MockService.class, service3,
                withRanking(2));

        tracker.open();

        assertThat(service3).isEqualTo(consumer.getService());
        handle3.unregister();

        assertThat(service1).isEqualTo(consumer.getService());

        tracker.close();

        assertThat(consumer.getService()).isNull();

    }

    private Dictionary<String, ?> withRanking(final Integer ranking) {
        final Hashtable<String, Object> properties = new Hashtable<>();
        if (ranking != null) {
            properties.put(Constants.SERVICE_RANKING, ranking);
        }
        return properties;
    }
}
