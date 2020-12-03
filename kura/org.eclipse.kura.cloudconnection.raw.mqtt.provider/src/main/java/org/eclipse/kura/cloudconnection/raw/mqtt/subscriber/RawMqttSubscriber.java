/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.cloudconnection.raw.mqtt.subscriber;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.raw.mqtt.cloud.RawMqttCloudEndpoint;
import org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber;
import org.eclipse.kura.cloudconnection.subscriber.listener.CloudSubscriberListener;
import org.eclipse.kura.cloudconnecton.raw.mqtt.util.AbstractStackComponent;
import org.eclipse.kura.cloudconnecton.raw.mqtt.util.StackComponentOptions;
import org.eclipse.kura.cloudconnecton.raw.mqtt.util.StackComponentOptions.OptionsFactory;
import org.eclipse.kura.cloudconnecton.raw.mqtt.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RawMqttSubscriber extends AbstractStackComponent<SubscribeOptions>
        implements CloudSubscriber, CloudSubscriberListener {

    private static final Logger logger = LoggerFactory.getLogger(RawMqttSubscriber.class);

    private final Set<CloudSubscriberListener> cloudSubscriberListeners = new CopyOnWriteArraySet<>();

    @Override
    protected void setCloudEndpoint(final RawMqttCloudEndpoint endpoint) {
        super.setCloudEndpoint(endpoint);
        trySubscribe();
    }

    @Override
    protected void unsetCloudEndpoint(final RawMqttCloudEndpoint endpoint) {
        tryUnsubscribe();
        super.unsetCloudEndpoint(endpoint);
    }

    @Override
    public void registerCloudSubscriberListener(final CloudSubscriberListener listener) {
        this.cloudSubscriberListeners.add(listener);
    }

    @Override
    public void unregisterCloudSubscriberListener(final CloudSubscriberListener listener) {
        this.cloudSubscriberListeners.remove(listener);
    }

    @Override
    public void onMessageArrived(final KuraMessage message) {
        this.cloudSubscriberListeners.forEach(Utils.catchAll(l -> l.onMessageArrived(message)));
    }

    private void trySubscribe() {

        final Optional<RawMqttCloudEndpoint> endpoint = getEndpoint();

        if (!endpoint.isPresent()) {
            return;
        }

        final RawMqttCloudEndpoint currentEndpoint = endpoint.get();

        currentEndpoint.unregisterSubscriber(this);

        final StackComponentOptions<SubscribeOptions> options = getOptions();

        final Optional<SubscribeOptions> subscribeOptions = options.getComponentOptions();

        if (subscribeOptions.isPresent()) {
            currentEndpoint.registerSubscriber(subscribeOptions.get(), this);
        }

    }

    private void tryUnsubscribe() {

        final Optional<RawMqttCloudEndpoint> endpoint = getEndpoint();

        if (endpoint.isPresent()) {
            endpoint.get().unregisterSubscriber(this);
        }

    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected OptionsFactory<SubscribeOptions> getOptionsFactory() {
        return SubscribeOptions::new;
    }
}
