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
import java.util.Objects;
import java.util.UUID;

import org.eclipse.jetty.servlets.EventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This represents an established {@link EventSource} connection between a server and a client.
 */
public final class SseEventSource implements EventSource {

    /** Logger Instance. */
    private static final Logger logger = LoggerFactory.getLogger(SseEventSource.class);

    /** Event Emitter to the established Event Source instance. */
    private Emitter emitter;

    /** Event Source Connection ID. */
    private final String id;

    /** Event Publisher instance. */
    private final SseEventPublisher publisher;

    /** Event Topic for subscription. */
    private final String topic;

    /**
     * Constructor
     *
     * @param topic
     *            the event topic
     * @throws NullPointerException
     *             if the event topic is null
     */
    private SseEventSource(final String topic) {
        requireNonNull(topic, "Event Topic cannot be null");
        this.id = UUID.randomUUID().toString();
        this.publisher = SseEventPublisher.getInstance();
        this.topic = topic;
    }

    /**
     * Creates new {@link SseEventSource} instance
     *
     * @param topic
     *            the event topic
     * @throws NullPointerException
     *             if the event topic is null
     */
    public static SseEventSource of(final String topic) {
        return new SseEventSource(topic);
    }

    /**
     * Emits the data to the established {@link EventSource} connection instance.
     *
     * @param dataToSend
     *            the data to be sent
     * @throws IOException
     *             if the connection is erroneous
     */
    public synchronized void emitEvent(final String dataToSend) throws IOException {
        logger.debug("Data Emitted for " + this.id);
        if ((this.emitter != null) && (dataToSend != null)) {
            this.emitter.data(dataToSend);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof SseEventSource) {
            final SseEventSource that = (SseEventSource) obj;
            return Objects.equals(this.id, that.id) && Objects.equals(this.topic, that.topic);
        }
        return false;
    }

    /**
     * Gets the Event Topic.
     *
     * @return the event topic
     */
    public String getTopic() {
        return this.topic;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.topic);
    }

    /** {@inheritDoc} */
    @Override
    public void onClose() {
        logger.debug("Connection Closed for " + this.id);
        this.publisher.removeEventSource(this);
    }

    /** {@inheritDoc} */
    @Override
    public void onOpen(final Emitter emitter) throws IOException {
        logger.debug("Connection Established for " + this.id);
        this.emitter = emitter;
    }
}