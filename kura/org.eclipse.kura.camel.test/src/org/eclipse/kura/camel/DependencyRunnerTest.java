/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.kura.camel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.camel.runner.DependencyRunner;
import org.eclipse.kura.camel.runner.DependencyRunner.Listener;
import org.eclipse.kura.camel.runner.ServiceDependency;
import org.junit.Test;

public class DependencyRunnerTest {

    private static class MockListener implements Listener<Map<String, Object>> {

        public static class Event {

            private final Map<String, Object> services;

            public Event() {
                this.services = null;
            }

            public Event(Map<String, Object> services) {
                this.services = new HashMap<>(services);
            }

            public Map<String, Object> getServices() {
                return this.services;
            }
        }

        private final LinkedList<Event> events = new LinkedList<>();

        @Override
        public void ready(final List<ServiceDependency.Handle<Map<String, Object>>> dependencies) {
            final Map<String, Object> services = new HashMap<>();

            for (final ServiceDependency.Handle<Map<String, Object>> dep : dependencies) {
                dep.consume(services);
            }

            System.out.println("Ready: " + services);
            this.events.add(new Event(services));
        }

        @Override
        public void notReady() {
            System.out.println("Not ready");
            this.events.add(new Event());
        }

        public LinkedList<Event> getEvents() {
            return this.events;
        }
    }

    private static class MockServiceDependency<T> implements ServiceDependency<T, Map<String, Object>> {

        private class Handle implements ServiceDependency.Handle<Map<String, Object>> {

            private final String name;

            private T service;

            private final Runnable listener;

            public Handle(final String name, T service, Runnable listener) {
                this.name = name;
                this.service = service;
                this.listener = listener;
            }

            public void setService(T service) {
                this.service = service;

                if (this.listener != null) {
                    this.listener.run();
                }
            }

            @Override
            public void stop() {
                MockServiceDependency.this.handles.remove(this);
            }

            @Override
            public boolean isSatisfied() {
                return this.service != null;
            }

            @Override
            public void consume(Map<String, Object> context) {
                context.put(this.name, this.service);
            }

        }

        private final String name;

        private T service;

        private final List<MockServiceDependency<T>.Handle> handles = new LinkedList<>();

        public MockServiceDependency(final String name) {
            this.name = name;
        }

        public MockServiceDependency(final String name, final T service) {
            this.name = name;
            this.service = service;
        }

        @Override
        public Handle start(final Runnable listener) {
            final Handle handle = new Handle(this.name, this.service, listener);
            this.handles.add(handle);
            return handle;
        }

        public void setService(T service) {
            this.service = service;
            for (Handle handle : this.handles) {
                handle.setService(service);
            }
        }
    }

    @Test
    public void testEmpty1() {
        final MockListener listener = new MockListener();
        final DependencyRunner<Map<String, Object>> runner = new DependencyRunner<>(
                Collections.<ServiceDependency<?, Map<String, Object>>> emptyList(), listener);

        // no events yet

        expectNoEvents(listener);

        // start

        runner.start();
        expectEvent(listener, Collections.<String, Object> emptyMap());

        // stop

        runner.stop();
        expectEvent(listener, null);

        // nothing in addition

        expectNoEvents(listener);
    }

    private static final Object A = new Object();
    private static final Object B = new Object();
    private static final Object C = new Object();

    @Test
    public void testSimple1() {
        final MockListener listener = new MockListener();

        final MockServiceDependency<Object> service1 = new MockServiceDependency<>("foo", A);
        final MockServiceDependency<Object> service2 = new MockServiceDependency<>("bar", B);

        Map<String, Object> all = new HashMap<>();
        all.put("foo", A);
        all.put("bar", B);

        List<ServiceDependency<?, Map<String, Object>>> deps = new LinkedList<>();
        deps.add(service1);
        deps.add(service2);

        final DependencyRunner<Map<String, Object>> runner = new DependencyRunner<>(deps, listener);

        // no events yet

        expectNoEvents(listener);

        // start

        runner.start();
        expectEvent(listener, all);

        // stop

        runner.stop();
        expectEvent(listener, null);

        // nothing in addition

        expectNoEvents(listener);
    }

    @Test
    public void testSimple2() {
        final MockListener listener = new MockListener();

        final MockServiceDependency<Object> service1 = new MockServiceDependency<>("foo");
        final MockServiceDependency<Object> service2 = new MockServiceDependency<>("bar");

        Map<String, Object> all = new HashMap<>();
        all.put("foo", A);
        all.put("bar", B);

        List<ServiceDependency<?, Map<String, Object>>> deps = new LinkedList<>();
        deps.add(service1);
        deps.add(service2);

        final DependencyRunner<Map<String, Object>> runner = new DependencyRunner<>(deps, listener);

        // no events yet

        expectNoEvents(listener);

        // start - but not ready

        runner.start();
        expectNoEvents(listener);

        // set A - no event - no change
        service1.setService(A);
        expectNoEvents(listener);

        // set B - change to ready - event
        service2.setService(B);
        expectEvent(listener, all);

        // stop

        runner.stop();
        expectEvent(listener, null);

        // nothing in addition

        expectNoEvents(listener);
    }

    public void testRebind1() {
        final MockListener listener = new MockListener();

        final MockServiceDependency<Object> service1 = new MockServiceDependency<>("foo");
        final MockServiceDependency<Object> service2 = new MockServiceDependency<>("bar");

        Map<String, Object> all1 = new HashMap<>();
        all1.put("foo", A);
        all1.put("bar", B);

        Map<String, Object> all2 = new HashMap<>();
        all2.put("foo", A);
        all2.put("bar", C);

        List<ServiceDependency<?, Map<String, Object>>> deps = new LinkedList<>();
        deps.add(service1);
        deps.add(service2);

        final DependencyRunner<Map<String, Object>> runner = new DependencyRunner<>(deps, listener);

        // no events yet

        expectNoEvents(listener);

        // start - but not ready

        runner.start();
        expectNoEvents(listener);

        // set A - no event - no change

        service1.setService(A);
        expectNoEvents(listener);

        // set B - change to ready - event

        service2.setService(B);
        expectEvent(listener, all1);

        // remove B

        service2.setService(null);
        expectEvent(listener, null);

        // add C - change to ready - event

        service2.setService(C);
        expectEvent(listener, all2);

        // stop

        runner.stop();
        expectEvent(listener, null);

        // nothing in addition

        expectNoEvents(listener);
    }

    private void expectNoEvents(MockListener listener) {
        assertTrue(listener.getEvents().isEmpty());
    }

    private void expectEvent(final MockListener listener, final Map<String, Object> expectedServices) {
        final MockListener.Event event = listener.getEvents().pollFirst();

        assertNotNull(event);
        assertEquals(expectedServices, event.getServices());
    }
}
