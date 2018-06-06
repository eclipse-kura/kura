/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.utils;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.kura.util.osgi.SingleServiceTracker;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

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

    private BundleContext context;

    private List<ServiceRegistration<?>> registrations = new LinkedList<>();

    @Before
    public void setup() {
        this.context = FrameworkUtil.getBundle(SingleServiceTrackerTest.class).getBundleContext();
    }

    @After
    public void unregisterAll() {
        this.registrations.forEach(ServiceRegistration::unregister);
    }

    protected <T> ServiceRegistration<T> register(final Class<T> clazz, final T service,
            final Dictionary<String, ?> properties) {

        final ServiceRegistration<T> registration = this.context.registerService(clazz, service, properties);
        this.registrations.add(registration);
        return registration;

    }

    protected void unregister(final ServiceRegistration<?> registration) {
        this.registrations.remove(registration);
        registration.unregister();
    }

    @Test
    public void testInitOrder1() {

        final MockConsumer<MockService> consumer = new MockConsumer<>();
        final SingleServiceTracker<MockService> tracker = new SingleServiceTracker<>(this.context, MockService.class,
                consumer);

        Assert.assertNull(consumer.getService());

        final MockService service1 = new MockService();
        final MockService service2 = new MockService();

        final ServiceRegistration<MockService> handle2 = register(MockService.class, service2, withRanking(2));
        final ServiceRegistration<MockService> handle1 = register(MockService.class, service1, withRanking(1));

        tracker.open();
        Assert.assertEquals(service2, consumer.getService());
        tracker.close();

        Assert.assertNull(consumer.getService());

    }

    @Test
    public void testOrderRemove1() {

        final BundleContext context = FrameworkUtil.getBundle(SingleServiceTrackerTest.class).getBundleContext();

        final MockConsumer<MockService> consumer = new MockConsumer<>();
        final SingleServiceTracker<MockService> tracker = new SingleServiceTracker<>(context, MockService.class,
                consumer);

        Assert.assertNull(consumer.getService());

        final MockService service1 = new MockService();
        final MockService service2 = new MockService();

        final ServiceRegistration<MockService> handle2 = register(MockService.class, service2, withRanking(2));
        final ServiceRegistration<MockService> handle1 = register(MockService.class, service1, withRanking(1));

        tracker.open();
        Assert.assertEquals(service2, consumer.getService());
        unregister(handle2);
        Assert.assertEquals(service1, consumer.getService());
        tracker.close();

        Assert.assertNull(consumer.getService());

    }

    @Test
    public void testOrderAdd1() {

        final BundleContext context = FrameworkUtil.getBundle(SingleServiceTrackerTest.class).getBundleContext();

        final MockConsumer<MockService> consumer = new MockConsumer<>();
        final SingleServiceTracker<MockService> tracker = new SingleServiceTracker<>(context, MockService.class,
                consumer);

        Assert.assertNull(consumer.getService());

        final MockService service1 = new MockService();
        final MockService service2 = new MockService();

        final ServiceRegistration<MockService> handle1 = register(MockService.class, service1, withRanking(1));

        tracker.open();
        Assert.assertEquals(service1, consumer.getService());

        final ServiceRegistration<MockService> handle2 = register(MockService.class, service2, withRanking(2));

        Assert.assertEquals(service2, consumer.getService());
        tracker.close();

        Assert.assertNull(consumer.getService());

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

        Assert.assertNull(consumer.getService());

        final MockService service1 = new MockService();
        final MockService service2 = new MockService();

        final ServiceRegistration<MockService> handle1 = register(MockService.class, service1, withRanking(1));

        tracker.open();
        Assert.assertEquals(service1, consumer.getService());

        final ServiceRegistration<MockService> handle2 = register(MockService.class, service2, withRanking(1));

        Assert.assertEquals(service1, consumer.getService());
        tracker.close();

        Assert.assertNull(consumer.getService());

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

        Assert.assertNull(consumer.getService());

        final MockService service1 = new MockService();
        final MockService service2 = new MockService();
        final MockService service3 = new MockService();

        final ServiceRegistration<MockService> handle1 = register(MockService.class, service1, withRanking(null));
        final ServiceRegistration<MockService> handle2 = register(MockService.class, service2, withRanking(null));

        tracker.open();
        Assert.assertEquals(service1, consumer.getService());

        final ServiceRegistration<MockService> handle3 = register(MockService.class, service3, withRanking(-1));

        Assert.assertEquals(service1, consumer.getService());
        tracker.close();

        Assert.assertNull(consumer.getService());

    }

    @Test
    public void testOrderModify1() {

        final BundleContext context = FrameworkUtil.getBundle(SingleServiceTrackerTest.class).getBundleContext();

        final MockConsumer<MockService> consumer = new MockConsumer<>();
        final SingleServiceTracker<MockService> tracker = new SingleServiceTracker<>(context, MockService.class,
                consumer);

        Assert.assertNull(consumer.getService());

        final MockService service1 = new MockService();
        final MockService service2 = new MockService();

        final ServiceRegistration<MockService> handle2 = register(MockService.class, service2, withRanking(2));
        final ServiceRegistration<MockService> handle1 = register(MockService.class, service1, withRanking(1));

        tracker.open();
        Assert.assertEquals(service2, consumer.getService());
        handle1.setProperties(withRanking(3));
        Assert.assertEquals(service1, consumer.getService());
        tracker.close();

        Assert.assertNull(consumer.getService());

    }

    @Test
    public void testOrderModify2() {

        final BundleContext context = FrameworkUtil.getBundle(SingleServiceTrackerTest.class).getBundleContext();

        final MockConsumer<MockService> consumer = new MockConsumer<>();
        final SingleServiceTracker<MockService> tracker = new SingleServiceTracker<>(context, MockService.class,
                consumer);

        Assert.assertNull(consumer.getService());

        final MockService service1 = new MockService();
        final MockService service2 = new MockService();

        final ServiceRegistration<MockService> handle1 = register(MockService.class, service1, withRanking(null));
        final ServiceRegistration<MockService> handle2 = register(MockService.class, service2, withRanking(2));

        tracker.open();
        Assert.assertEquals(service2, consumer.getService());
        handle1.setProperties(withRanking(3));
        Assert.assertEquals(service1, consumer.getService());
        tracker.close();

        Assert.assertNull(consumer.getService());

    }

    @Test
    public void testOrderModify3() throws InterruptedException {

        final BundleContext context = FrameworkUtil.getBundle(SingleServiceTrackerTest.class).getBundleContext();

        final MockConsumer<MockService> consumer = new MockConsumer<>();
        final SingleServiceTracker<MockService> tracker = new SingleServiceTracker<>(context, MockService.class,
                consumer);

        Assert.assertNull(consumer.getService());

        final MockService service1 = new MockService();
        final MockService service2 = new MockService();

        final ServiceRegistration<MockService> handle1 = register(MockService.class, service1, withRanking(1));
        final ServiceRegistration<MockService> handle2 = register(MockService.class, service2, withRanking(1));

        tracker.open();

        // expect the first registered service
        Assert.assertEquals(service1, consumer.getService());

        // update, but don't change ranking
        handle1.setProperties(withRanking(1));

        // should still be the same service
        Assert.assertEquals(service1, consumer.getService());

        // change ranking

        handle2.setProperties(withRanking(2));
        Assert.assertEquals(service2, consumer.getService());

        tracker.close();

        Assert.assertNull(consumer.getService());

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

        Assert.assertNull(consumer.getService());

        final MockService service1 = new MockService();
        final MockService service2 = new MockService();
        final MockService service3 = new MockService();

        final ServiceRegistration<MockService> handle1 = register(MockService.class, service1, withRanking(1));
        final ServiceRegistration<MockService> handle2 = register(MockService.class, service2, withRanking(1));
        final ServiceRegistration<MockService> handle3 = register(MockService.class, service3, withRanking(2));

        tracker.open();

        Assert.assertEquals(service3, consumer.getService());

        unregister(handle3);
        Assert.assertEquals(service1, consumer.getService());

        tracker.close();

        Assert.assertNull(consumer.getService());
    }

    private Dictionary<String, ?> withRanking(final Integer ranking) {
        final Hashtable<String, Object> properties = new Hashtable<>();
        if (ranking != null) {
            properties.put(Constants.SERVICE_RANKING, ranking);
        }
        return properties;
    }
}
