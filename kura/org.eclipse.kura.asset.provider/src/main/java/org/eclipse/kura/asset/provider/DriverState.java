/*******************************************************************************
 * Copyright (c) 2018, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.asset.provider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.asset.provider.BaseAsset.ChannelListenerHolder;
import org.eclipse.kura.channel.Channel;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.PreparedRead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DriverState {

    private static final Logger logger = LoggerFactory.getLogger(DriverState.class);

    private final Driver driver;
    private final Set<ChannelListenerHolder> attachedListeners;

    private PreparedRead preparedRead;

    public DriverState(final Driver driver) {
        this.driver = driver;
        this.attachedListeners = new HashSet<>();
    }

    public Driver getDriver() {
        return this.driver;
    }

    public PreparedRead getPreparedRead() {
        return this.preparedRead;
    }

    public PreparedRead tryPrepareRead(final List<ChannelRecord> records) {
        PreparedRead pRead;

        try {
            pRead = this.driver.prepareRead(records);
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

    public void syncChannelListeners(final Set<ChannelListenerHolder> targetState,
            final Map<String, Channel> channels) {
        setChannelListenersInternal(new HashSet<>(targetState), channels);
    }

    private void setChannelListenersInternal(final Set<ChannelListenerHolder> targetState,
            final Map<String, Channel> channels) {

        final Iterator<ChannelListenerHolder> iter = this.attachedListeners.iterator();

        while (iter.hasNext()) {
            final ChannelListenerHolder reg = iter.next();

            if (!targetState.contains(reg)) {
                detach(iter, reg);
            }
        }

        for (final ChannelListenerHolder holder : targetState) {

            if (this.attachedListeners.contains(holder)) {
                continue;
            }

            final Channel channel = channels.get(holder.getChannelName());

            if (channel != null && channel.isEnabled()) {
                attach(holder, channel);
            }
        }
    }

    private void attach(final ChannelListenerHolder holder, final Channel channel) {
        try {
            logger.debug("Registering Channel Listener for monitoring...");
            this.driver.registerChannelListener(channel.getConfiguration(), holder);
            this.attachedListeners.add(holder);
            logger.debug("Registering Channel Listener for monitoring...done");
        } catch (Exception e) {
            logger.warn("Failed to register channel listener", e);
        }
    }

    private void detach(final Iterator<ChannelListenerHolder> iter, final ChannelListenerHolder holder) {
        try {
            logger.debug("Unregistering Asset Listener...");
            this.driver.unregisterChannelListener(holder);
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
