/*******************************************************************************
 * Copyright (c) 2018, 2022 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  heyoulin <heyoulin@gmail.com>
 *******************************************************************************/
package org.eclipse.kura.asset.provider;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraRuntimeException;
import org.eclipse.kura.asset.provider.BaseAsset.ChannelListenerRegistration;
import org.eclipse.kura.channel.Channel;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.channel.listener.ChannelListener;
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

    public void syncChannelListeners(final Set<ChannelListenerRegistration> targetState,
            final Map<String, Channel> channels) {
        setChannelListenersInternal(new HashSet<>(targetState), channels);
    }

    private void setChannelListenersInternal(final Set<ChannelListenerRegistration> targetState,
            final Map<String, Channel> channels) {

        List<ChannelListenerRegistration> tobeRemoved = this.attachedListeners.stream()
                .filter(reg -> !targetState.contains(reg)).collect(Collectors.toList());
        if (!tobeRemoved.isEmpty()) {
            detach(tobeRemoved);
        }

        Map<Channel, ChannelListenerRegistration> regChannels = targetState.stream().filter(reg -> {
            if (this.attachedListeners.contains(reg)) {
                return false;
            }

            final Channel channel = channels.get(reg.getChannelName());

            return channel != null && channel.isEnabled();
        }).collect(Collectors.toMap(reg -> channels.get(reg.getChannelName()), reg -> reg));
        if (!regChannels.isEmpty()) {
            attach(regChannels);
        }

    }

    private void attach(final Map<Channel, ChannelListenerRegistration> regChannels) {
        logger.debug("Registering Channel Listener for monitoring...");
        Map<ChannelListenerRegistration, Map<String, Object>> listenerChannelConfigs = regChannels.entrySet().stream()
                .collect(Collectors.toMap(Entry::getValue, regChannel -> regChannel.getKey().getConfiguration()));
        try {
            this.driver.registerChannelListeners(listenerChannelConfigs.entrySet().stream().collect(Collectors.toMap(
                    listenerChannelConfig -> listenerChannelConfig.getKey().getChannelListener(), Entry::getValue)));
            Set<ChannelListenerRegistration> regs = regChannels.entrySet().stream().map(Entry::getValue)
                    .collect(Collectors.toSet());
            this.attachedListeners.addAll(regs);
        } catch (KuraRuntimeException kuraError) {
            if (kuraError.getCode() == KuraErrorCode.OPERATION_NOT_SUPPORTED) {
                for (Map.Entry<ChannelListenerRegistration, Map<String, Object>> entry : listenerChannelConfigs
                        .entrySet()) {
                    try {
                        this.driver.registerChannelListener(entry.getValue(), entry.getKey().getChannelListener());
                        this.attachedListeners.add(entry.getKey());
                    } catch (Exception regError) {
                        logger.warn("Failed to register channel listener", regError);
                    }
                }
            } else {
                logger.warn("Failed to register channel listeners", kuraError);
            }
        } catch (Exception e) {
            logger.warn("Failed to register channel listeners", e);
        }

        logger.debug("Registering Channel Listener for monitoring...done");

    }

    private void detach(final Collection<ChannelListenerRegistration> registrations) {
        logger.debug("Unregistering Asset Listener...");

        try {
            Collection<ChannelListener> listeners = registrations.stream()
                    .map(ChannelListenerRegistration::getChannelListener).collect(Collectors.toSet());
            this.driver.unregisterChannelListeners(listeners);
            this.attachedListeners.removeAll(registrations);
        } catch (KuraRuntimeException kuraError) {
            if (kuraError.getCode() == KuraErrorCode.OPERATION_NOT_SUPPORTED) {
                for (ChannelListenerRegistration registration : registrations) {
                    try {
                        this.driver.unregisterChannelListener(registration.getChannelListener());
                        this.attachedListeners.remove(registration);
                    } catch (Exception regError) {
                        logger.warn("Failed to unregister channel listener", regError);
                    }
                }
            } else {
                logger.warn("Failed to unregister channel listeners", kuraError);
            }
        } catch (Exception e) {
            logger.warn("Failed to unregister channel listeners", e);
        }
        logger.debug("Unregistering Asset Listener...done");
    }

    public synchronized void shutdown() {

        closePreparedRead();
        setChannelListenersInternal(Collections.emptySet(), Collections.emptyMap());
    }
}
