/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.cloudconnection.raw.mqtt.publisher;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.cloudconnection.raw.mqtt.cloud.RawMqttCloudEndpoint;
import org.eclipse.kura.cloudconnecton.raw.mqtt.util.AbstractStackComponent;
import org.eclipse.kura.cloudconnecton.raw.mqtt.util.StackComponentOptions;
import org.eclipse.kura.cloudconnecton.raw.mqtt.util.StackComponentOptions.OptionsFactory;
import org.eclipse.kura.cloudconnecton.raw.mqtt.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RawMqttPublisher extends AbstractStackComponent<PublishOptions>
        implements CloudPublisher, CloudDeliveryListener {

    private static final Logger logger = LoggerFactory.getLogger(RawMqttPublisher.class);

    private final Set<CloudDeliveryListener> cloudDeliveryListeners = new CopyOnWriteArraySet<>();

    @Override
    protected void setCloudEndpoint(final RawMqttCloudEndpoint endpoint) {
        super.setCloudEndpoint(endpoint);
        endpoint.registerCloudDeliveryListener(this);
    }

    @Override
    protected void unsetCloudEndpoint(final RawMqttCloudEndpoint endpoint) {
        endpoint.unregisterCloudConnectionListener(this);
        super.unsetCloudEndpoint(endpoint);
    }

    @Override
    public String publish(final KuraMessage message) throws KuraException {
        final StackComponentOptions<PublishOptions> currentOptions = getOptions();

        final Optional<PublishOptions> publishOptions = currentOptions.getComponentOptions();

        if (!publishOptions.isPresent()) {
            throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "invalid publish configuration");
        }

        final Optional<RawMqttCloudEndpoint> currentEndpoint = getEndpoint();

        if (!currentEndpoint.isPresent()) {
            throw new KuraException(KuraErrorCode.NOT_FOUND, "cloud endpoint not bound");
        }

        return currentEndpoint.get().publish(publishOptions.get(), message.getPayload());
    }

    @Override
    public void registerCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        cloudDeliveryListeners.add(cloudDeliveryListener);
    }

    @Override
    public void unregisterCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        cloudDeliveryListeners.remove(cloudDeliveryListener);
    }

    @Override
    public void onMessageConfirmed(String messageId) {
        cloudDeliveryListeners.forEach(Utils.catchAll(l -> l.onMessageConfirmed(messageId)));
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    protected OptionsFactory<PublishOptions> getOptionsFactory() {
        return PublishOptions::new;
    }

}
