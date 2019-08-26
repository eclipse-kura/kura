/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.wire.devel.driver.dummy;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.listener.ChannelEvent;
import org.eclipse.kura.channel.listener.ChannelListener;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.devel.driver.dummy.DummyDriver.BaseRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelListenerManager {

    private static final Logger logger = LoggerFactory.getLogger(ChannelListenerManager.class);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Set<ChannelListenerRegistration> registeredListeners = new CopyOnWriteArraySet<>();
    private final DummyDriver driver;

    public ChannelListenerManager(final DummyDriver driver) {
        this.driver = driver;
    }

    public void start() {
        this.executor.scheduleWithFixedDelay(this::simulateChannelEvents, 0, 1, TimeUnit.SECONDS);
    }

    public void shutdown() {
        this.executor.shutdown();
    }

    public void registerChannelListener(final Map<String, Object> channelConfig, final ChannelListener listener) {
        logger.debug("registering channel listener...");
        this.registeredListeners.add(new ChannelListenerRegistration(new BaseRequest(channelConfig), listener));
        logger.debug("registering channel listener...done");
    }

    public void unregisterChannelListener(final ChannelListener listener) {
        logger.debug("unregistering channel listener...");
        final boolean removed = this.registeredListeners.removeIf(reg -> reg.listener == listener);
        if (!removed) {
            logger.debug("listener not found");
        }
        logger.debug("unregistering channel listener...done");
    }

    private void simulateChannelEvents() {

        if (!this.driver.isConnected()) {
            return;
        }

        for (final ChannelListenerRegistration reg : this.registeredListeners) {

            try {
                final TypedValue<?> value = this.driver.readInternal(reg.request);

                String channelName = reg.request.getChannelName();
                final ChannelRecord record = ChannelRecord.createReadRecord(channelName, value.getType());
                record.setValue(value);
                record.setChannelStatus(DummyDriver.SUCCESS);
                record.setTimestamp(System.currentTimeMillis());

                reg.listener.onChannelEvent(new ChannelEvent(record));
            } catch (Exception e) {
                logger.debug("Error dispatching channel event", e);
            }
        }
    }

    private static final class ChannelListenerRegistration {

        private final BaseRequest request;
        private final ChannelListener listener;

        public ChannelListenerRegistration(final BaseRequest request, final ChannelListener listener) {
            this.request = request;
            this.listener = listener;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            String channelName = this.request.getChannelName();
            result = prime * result + (channelName == null ? 0 : channelName.hashCode());
            result = prime * result + (this.listener == null ? 0 : this.listener.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ChannelListenerRegistration other = (ChannelListenerRegistration) obj;
            if (this.request.getChannelName() == null) {
                if (other.request.getChannelName() != null) {
                    return false;
                }
            } else if (!this.request.getChannelName().equals(other.request.getChannelName())) {
                return false;
            }
            if (this.listener == null) {
                if (other.listener != null) {
                    return false;
                }
            } else if (!this.listener.equals(other.listener)) {
                return false;
            }
            return true;
        }
    }
}
