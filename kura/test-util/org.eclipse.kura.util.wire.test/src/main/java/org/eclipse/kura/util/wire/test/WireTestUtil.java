/*******************************************************************************
 * Copyright (c) 2020, 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.util.wire.test;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.graph.Constants;
import org.eclipse.kura.wire.graph.MultiportWireConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.wireadmin.Wire;
import org.osgi.service.wireadmin.WireAdminEvent;
import org.osgi.service.wireadmin.WireAdminListener;
import org.osgi.service.wireadmin.WireConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public final class WireTestUtil {

    private WireTestUtil() {
    }

    public static CompletableFuture<Void> updateWireComponentConfiguration(
            final ConfigurationService configurationService, final String pid, final Map<String, Object> properties) {

        try {
            final BundleContext bundleContext = FrameworkUtil.getBundle(WireTestUtil.class).getBundleContext();

            final CompletableFuture<Void> result = new CompletableFuture<>();

            final Filter filter = FrameworkUtil
                    .createFilter("(&(objectClass=org.eclipse.kura.wire.WireComponent)(kura.service.pid=" + pid + "))");

            final ServiceTracker<WireComponent, WireComponent> tracker = new ServiceTracker<>(bundleContext, filter,
                    new ServiceTrackerCustomizer<WireComponent, WireComponent>() {

                        @Override
                        public WireComponent addingService(final ServiceReference<WireComponent> ref) {
                            return bundleContext.getService(ref);
                        }

                        @Override
                        public void modifiedService(final ServiceReference<WireComponent> ref,
                                final WireComponent comp) {
                            result.complete(null);
                        }

                        @Override
                        public void removedService(final ServiceReference<WireComponent> ref,
                                final WireComponent comp) {
                            bundleContext.ungetService(ref);
                        }
                    });

            tracker.open();

            configurationService.updateConfiguration(pid, properties);

            return result.whenComplete((ok, ex) -> tracker.close());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static CompletableFuture<Void> updateComponentConfiguration(final ConfigurationService configurationService,
            final String pid, final Map<String, Object> properties) throws KuraException, InvalidSyntaxException {

        final CompletableFuture<Void> result = new CompletableFuture<Void>();
        final BundleContext context = FrameworkUtil.getBundle(WireTestUtil.class).getBundleContext();

        final ServiceTracker<?, ?> tracker = new ServiceTracker<Object, Object>(context,
                FrameworkUtil.createFilter("(kura.service.pid=" + pid + ")"),
                new ServiceTrackerCustomizer<Object, Object>() {

                    @Override
                    public Object addingService(ServiceReference<Object> reference) {
                        return context.getService(reference);
                    }

                    @Override
                    public void modifiedService(ServiceReference<Object> reference, Object service) {
                        result.complete(null);
                    }

                    @Override
                    public void removedService(ServiceReference<Object> reference, Object service) {
                        context.ungetService(reference);
                    }
                });

        tracker.open();

        configurationService.updateConfiguration(pid, properties);

        return result.whenComplete((ok, ex) -> tracker.close());
    }

    public static <T> CompletableFuture<T> createFactoryConfiguration(final ConfigurationService configurationService,
            final Class<T> classz, final String pid, final String factoryPid, final Map<String, Object> properties) {
        try {
            final BundleContext bundleContext = FrameworkUtil.getBundle(WireTestUtil.class).getBundleContext();

            final CompletableFuture<T> result = new CompletableFuture<>();

            final Filter filter = FrameworkUtil
                    .createFilter("(&(objectClass=" + classz.getName() + ")(kura.service.pid=" + pid + "))");

            final ServiceTracker<T, T> tracker = new ServiceTracker<>(bundleContext, filter,
                    new ServiceTrackerCustomizer<T, T>() {

                        @Override
                        public T addingService(final ServiceReference<T> ref) {
                            final T obj = bundleContext.getService(ref);

                            result.complete(obj);

                            return obj;

                        }

                        @Override
                        public void modifiedService(final ServiceReference<T> ref, final T comp) {
                            // nothing to do
                        }

                        @Override
                        public void removedService(final ServiceReference<T> ref, final T comp) {
                            bundleContext.ungetService(ref);
                        }
                    });

            tracker.open();

            configurationService.createFactoryConfiguration(factoryPid, pid, properties, false);

            return result.whenComplete((ok, ex) -> tracker.close());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static CompletableFuture<Void> deleteFactoryConfiguration(final ConfigurationService configurationService,
            final String pid) {

        try {
            final BundleContext bundleContext = FrameworkUtil.getBundle(WireTestUtil.class).getBundleContext();

            final CompletableFuture<Void> tracked = new CompletableFuture<>();
            final CompletableFuture<Void> removed = new CompletableFuture<>();

            final Filter filter = FrameworkUtil.createFilter("(kura.service.pid=" + pid + ")");

            final ServiceTracker<Object, Object> tracker = new ServiceTracker<>(bundleContext, filter,
                    new ServiceTrackerCustomizer<Object, Object>() {

                        @Override
                        public Object addingService(final ServiceReference<Object> ref) {
                            tracked.complete(null);
                            return bundleContext.getService(ref);
                        }

                        @Override
                        public void modifiedService(final ServiceReference<Object> ref, final Object comp) {
                            // nothing to do
                        }

                        @Override
                        public void removedService(final ServiceReference<Object> ref, final Object comp) {
                            removed.complete(null);
                            bundleContext.ungetService(ref);
                        }
                    });

            tracker.open();

            try {
                tracked.get(30, TimeUnit.SECONDS);
            } catch (final Exception e) {
                tracker.close();
                throw e;
            }

            configurationService.deleteFactoryConfiguration(pid, false);

            return removed.whenComplete((ok, ex) -> tracker.close());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static WireEnvelope unwrapEnvelope(final Future<WireEnvelope> wireEnvelope)
            throws InterruptedException, ExecutionException, TimeoutException {
        return wireEnvelope.get(30, TimeUnit.SECONDS);
    }

    private static WireRecord unwrapRecord(final Future<WireEnvelope> wireEnvelope, final int index)
            throws InterruptedException, ExecutionException, TimeoutException {
        return unwrapEnvelope(wireEnvelope).getRecords().get(index);
    }

    public static TypedValue<?> unwrapProperty(final Future<WireEnvelope> envelope, final String key)
            throws InterruptedException, ExecutionException, TimeoutException {
        return unwrapRecord(envelope, 0).getProperties().get(key);
    }

    public static CompletableFuture<Void> componentsActivated(final BundleContext context,
            final Set<String> expectedPids, final BiConsumer<String, Object> trackedObjectConsumer) {

        final Set<String> tracked = new HashSet<>();

        final CompletableFuture<Void> result = new CompletableFuture<>();

        final ServiceTracker<WireComponent, WireComponent> tracker = new ServiceTracker<>(context, WireComponent.class,
                new ServiceTrackerCustomizer<WireComponent, WireComponent>() {

                    @Override
                    public WireComponent addingService(final ServiceReference<WireComponent> ref) {

                        final WireComponent comp = context.getService(ref);

                        final String kuraServicePid = (String) ref.getProperty(ConfigurationService.KURA_SERVICE_PID);

                        tracked.add(kuraServicePid);
                        trackedObjectConsumer.accept(kuraServicePid, comp);

                        if (tracked.containsAll(expectedPids)) {
                            result.complete(null);
                        }

                        return comp;
                    }

                    @Override
                    public void modifiedService(final ServiceReference<WireComponent> ref, final WireComponent comp) {
                        // no need
                    }

                    @Override
                    public void removedService(final ServiceReference<WireComponent> ref, final WireComponent comp) {
                        context.ungetService(ref);
                    }
                });

        tracker.open();

        return result.whenComplete((ok, ex) -> tracker.close());
    }

    public static CompletableFuture<Void> wiresConnected(final BundleContext context,
            final Set<MultiportWireConfiguration> expected) {

        final Set<MultiportWireConfiguration> tracked = new HashSet<>();

        final CompletableFuture<Void> result = new CompletableFuture<>();

        final WireAdminListener listener = e -> {
            final Wire wire = e.getWire();

            if (e.getType() != WireAdminEvent.WIRE_CONNECTED) {
                return;
            }

            tracked.add(fromWire(wire));

            if (tracked.containsAll(expected)) {
                result.complete(null);
            }

        };

        final Dictionary<String, Object> listenerProperties = new Hashtable<>();
        listenerProperties.put(WireConstants.WIREADMIN_EVENTS, WireAdminEvent.WIRE_CONNECTED);

        final ServiceRegistration<WireAdminListener> registration = context.registerService(WireAdminListener.class,
                listener, listenerProperties);

        return result.whenComplete((ok, ex) -> registration.unregister());

    }

    private static MultiportWireConfiguration fromWire(final Wire wire) {
        @SuppressWarnings("unchecked")
        final Dictionary<String, ?> properties = wire.getProperties();

        final String emitterPid = (String) properties.get(Constants.EMITTER_KURA_SERVICE_PID_PROP_NAME.value());
        final int emitterPort = (Integer) properties.get(Constants.WIRE_EMITTER_PORT_PROP_NAME.value());
        final String receiverPid = (String) properties.get(Constants.RECEIVER_KURA_SERVICE_PID_PROP_NAME.value());
        final int receiverPort = (Integer) properties.get(Constants.WIRE_RECEIVER_PORT_PROP_NAME.value());

        return new MultiportWireConfiguration(emitterPid, receiverPid, emitterPort, receiverPort);
    }

}
