/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     
 *******************************************************************************/
package org.eclipse.kura.asset.provider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.asset.provider.BaseAsset.ChannelListenerRegistration;
import org.eclipse.kura.channel.Channel;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.PreparedRead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DriverState {

    private static final Logger logger = LoggerFactory.getLogger(DriverState.class);

    private final Driver driver;
    private final Set<ChannelListenerRegistration> attachedListeners;

    private PreparedRead preparedRead;

    public DriverState(final Driver driver) {
        this.driver = driver;
        this.attachedListeners = new HashSet<>();
    }

    public Driver getDriver() {
        return driver;
    }

    public PreparedRead getPreparedRead() {
        return preparedRead;
    }

    public PreparedRead tryPrepareRead(final List<ChannelRecord> records) {
        PreparedRead pRead;

        try {
            pRead = driver.prepareRead(records);
        } catch (final Exception e) {
            pRead = null;
        }

        if (pRead != null) {
            this.preparedRead = pRead;
        }

        return pRead;
    }

    private void closePreparedRead() {
        if (this.preparedRead != null) {
            try {
                this.preparedRead.close();
            } catch (Exception e) {
                logger.warn("Failed to close prepared read", e);
            }
            this.preparedRead = null;
        }
    }

    public void syncChannelListeners(final Set<ChannelListenerRegistration> targetState,
            final Map<String, Channel> channels) {
        setChannelListenersInternal(new HashSet<>(targetState), channels);
    }

    private void setChannelListenersInternal(final Set<ChannelListenerRegistration> targetState,
            final Map<String, Channel> channels) {

        final Iterator<ChannelListenerRegistration> iter = attachedListeners.iterator();

        while (iter.hasNext()) {
            final ChannelListenerRegistration reg = iter.next();

            if (!targetState.contains(reg)) {
                detach(iter, reg);
            }
        }

        for (final ChannelListenerRegistration reg : targetState) {

            if (attachedListeners.contains(reg)) {
                continue;
            }

            final Channel channel = channels.get(reg.getChannelName());

            if (channel != null && channel.isEnabled()) {
                attach(reg, channel);
            }
        }
    }

    private void attach(final ChannelListenerRegistration reg, final Channel channel) {
        try {
            logger.debug("Registering Channel Listener for monitoring...");
            driver.registerChannelListener(channel.getConfiguration(), reg.getChannelListener());
            attachedListeners.add(reg);
            logger.debug("Registering Channel Listener for monitoring...done");
        } catch (Exception e) {
            logger.warn("Failed to register channel listener", e);
        }
    }

    private void detach(final Iterator<ChannelListenerRegistration> iter, final ChannelListenerRegistration reg) {
        try {
            logger.debug("Unregistering Asset Listener...");
            driver.unregisterChannelListener(reg.getChannelListener());
            iter.remove();
            logger.debug("Unregistering Asset Listener...done");
        } catch (Exception e) {
            logger.warn("Failed to unregister channel listener", e);
        }
    }

    public synchronized void shutdown() {

        closePreparedRead();
        setChannelListenersInternal(Collections.emptySet(), Collections.emptyMap());
    }
}
