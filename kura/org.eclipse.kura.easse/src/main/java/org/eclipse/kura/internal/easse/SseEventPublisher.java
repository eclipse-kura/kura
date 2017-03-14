/*******************************************************************************
 * Copyright (c) 2017 Amit Kumar Mondal and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.internal.easse;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.servlets.EventSource;
import org.eclipse.kura.util.collection.CollectionUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

/**
 * This {@link SseEventPublisher} is responsible for tracking all established {@link EventSource}s
 * and their associated {@link EventHandler}s. It also publishes the events to the connected
 * {@link EventSource}s
 */
public final class SseEventPublisher {

    /** Singleton Atomic Instance */
    private static AtomicReference<SseEventPublisher> instanceRef = new AtomicReference<>(null);

    /** Logger Instance */
    private static final Logger logger = LoggerFactory.getLogger(SseEventPublisher.class);

    /** Bundle Context */
    private final BundleContext bundleContext;

    /** All registered Event Sources */
    private final Map<EventSource, ServiceRegistration<?>> sourcesAndListeners;

    /** Constructor */
    private SseEventPublisher() {
        // no need for external access
        this.sourcesAndListeners = CollectionUtil.newConcurrentHashMap();
        this.bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    }

    /** Gets Singleton Instance */
    public static SseEventPublisher getInstance() {
        instanceRef.compareAndSet(null, new SseEventPublisher());
        return instanceRef.get();
    }

    /**
     * Registers the provided {@link EventSource} instance
     *
     * @param eventSource
     *            the {@link EventSource}
     * @throws NullPointerException
     *             if the {@link EventSource} is null
     */
    public void addEventSource(final SseEventSource eventSource) {
        requireNonNull(eventSource, "Event Source instance cannot be null");

        final ServiceRegistration<?> eventHandler = registerEventHandler(eventSource);
        this.sourcesAndListeners.put(eventSource, eventHandler);
    }

    /**
     * Destroys all the registered {@link EventHandler}s and clears the associated map
     */
    public void destroy() {
        this.sourcesAndListeners.entrySet().stream().forEach(e -> e.getValue().unregister());
        this.sourcesAndListeners.clear();
    }

    /**
     * Converts the provided {@link Map} instance associated properties to JSON format
     *
     * @param eventProperties
     *            the provided {@link Map} instance
     * @return the JSON representation
     * @throws NullPointerException
     *             if the {@link Map} instance is null
     */
    @SuppressWarnings("unchecked")
    private JsonObject getJsonFromEventProperties(final Map<String, Object> eventProperties) {
        requireNonNull(eventProperties, "Properties cannot be null");

        final JsonObject jsonData = Json.object();
        for (final Map.Entry<String, Object> entry : eventProperties.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            if (value instanceof Map<?, ?>) {
                getJsonFromEventProperties((Map<String, Object>) value);
            }
            jsonData.add(key, String.valueOf(value));
        }
        return jsonData;
    }

    /**
     * Returns the provided {@link Event} associated properties
     *
     * @param event
     *            the provided {@link Event}
     * @return the associated properties
     * @throws NullPointerException
     *             if the provided {@link Event} is null
     */
    private Map<String, Object> getProperties(final Event event) {
        requireNonNull(event, "The provided Event cannot be null");

        final Map<String, Object> eventProperties = new HashMap<>();
        for (final String prop : event.getPropertyNames()) {
            eventProperties.put(prop, event.getProperty(prop));
        }
        return eventProperties;
    }

    /**
     * Registers an Event Handler for the established Event Source
     *
     * @param eventSource
     *            the established {@link EventSource}
     * @return service registration
     * @throws NullPointerException
     *             if the {@link EventSource} is null
     */
    private ServiceRegistration<?> registerEventHandler(final SseEventSource eventSource) {
        requireNonNull(eventSource, "The provided Event Source cannot be null");

        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(EventConstants.EVENT_TOPIC, eventSource.getTopic());
        return this.bundleContext.registerService(EventHandler.class, event -> {
            synchronized (SseEventPublisher.class) {
                try {
                    final Map<String, Object> props = getProperties(event);
                    eventSource.emitEvent(getJsonFromEventProperties(props).toString());
                } catch (final IOException e) {
                    logger.error("Data emitting failed... Cause: " + e.getCause());
                }
            }
        }, properties);
    }

    /**
     * Unregisters the provided {@link EventSource} instance
     *
     * @param eventSource
     *            the {@link EventSource}
     * @throws NullPointerException
     *             if the {@link EventSource} is null
     */
    public void removeEventSource(final SseEventSource eventSource) {
        requireNonNull(eventSource, "Event Source instance cannot be null");

        final ServiceRegistration<?> serviceRegistration = this.sourcesAndListeners.get(eventSource);
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
        this.sourcesAndListeners.remove(eventSource);
    }
}