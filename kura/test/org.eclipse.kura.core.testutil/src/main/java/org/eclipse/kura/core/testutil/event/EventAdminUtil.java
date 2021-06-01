/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.core.testutil.event;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CompletableFuture;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

public class EventAdminUtil {

    private EventAdminUtil() {
    }

    public static ServiceRegistration<EventHandler> registerEventHandler(final String[] topics,
            final EventHandler eventHandler, final BundleContext bundleContext) {
        final Dictionary<String, Object> properties = new Hashtable<>();

        properties.put(EventConstants.EVENT_TOPIC, topics);

        return bundleContext.registerService(EventHandler.class, eventHandler, properties);
    }

    public static <T extends Event> CompletableFuture<T> nextEvent(final String[] topics, final Class<T> classz,
            final BundleContext bundleContext) {
        final CompletableFuture<T> result = new CompletableFuture<>();

        @SuppressWarnings("unchecked")
        final ServiceRegistration<?> reg = registerEventHandler(topics, e -> {
            if (classz.isInstance(e)) {
                result.complete((T) e);
            }
        }, bundleContext);

        return result.whenComplete((ok, ex) -> reg.unregister());
    }
}
