/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.configuration.change.publisher;

import static org.eclipse.kura.core.message.MessageConstants.CONTROL;
import static org.eclipse.kura.core.message.MessageConstants.FULL_TOPIC;
import static org.eclipse.kura.core.message.MessageConstants.PRIORITY;
import static org.eclipse.kura.core.message.MessageConstants.QOS;
import static org.eclipse.kura.core.message.MessageConstants.RETAIN;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.change.publisher.utils.CloudStackHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationChangePublisher implements CloudPublisher, ConfigurableComponent {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationChangePublisher.class);

    private BundleContext bundleContext;

    private Set<CloudDeliveryListener> cloudDeliveryListeners = new HashSet<>();
    private Set<CloudConnectionListener> cloudConnectionListeners = new HashSet<>();

    private ConfigurationChangePublisherOptions options;

    private CloudStackHelper cloudHelper;

    /*
     * Activation APIs
     */

    public void activate(ComponentContext componentContext, Map<String, Object> properties)
            throws InvalidSyntaxException {
        logger.debug("Activating ConfigurationChangePublisher...");

        this.bundleContext = componentContext.getBundleContext();

        updated(properties);

        logger.debug("Activating ConfigurationChangePublisher... Done.");
    }

    public void updated(Map<String, Object> properties) throws InvalidSyntaxException {
        logger.debug("Updating ConfigurationChangePublisher...");
        
        this.options = new ConfigurationChangePublisherOptions(properties);
        this.cloudHelper = new CloudStackHelper(this.bundleContext, this.options.getCloudEndpointPid());

        logger.debug("Updating ConfigurationChangePublisher... Done.");
    }

    public void deactivate(ComponentContext componentContext) {
        logger.debug("Deactivating ConfigurationChangePublisher...");
        this.cloudHelper.close();
        logger.debug("Deactivating ConfigurationChangePublisher... Done.");
    }

    /*
     * CloudPublisher APIs
     */

    @Override
    public String publish(KuraMessage message) throws KuraException {
        if (message == null) {
            throw new IllegalArgumentException("Kura message cannot be null");
        }

        // $EVT/account/clientId/CONF/V1/

        String account = this.cloudHelper.getAccountName();
        String clientId = this.cloudHelper.getClientId();
        String fullTopic = this.options.getTopic().replace("$ACCOUNT_NAME", account).replace("$CLIENT_ID", clientId);

        Map<String, Object> publishMessageProps = new HashMap<>();
        publishMessageProps.put(FULL_TOPIC.name(), fullTopic);
        publishMessageProps.put(QOS.name(), this.options.getQos());
        publishMessageProps.put(RETAIN.name(), this.options.isRetain());
        publishMessageProps.put(PRIORITY.name(), this.options.getPriority());
        publishMessageProps.put(CONTROL.name(), false);

        logger.info("Message on topic: {}.", fullTopic);

        return this.cloudHelper.publish(new KuraMessage(message.getPayload(), publishMessageProps));
    }

    @Override
    public void registerCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        this.cloudConnectionListeners.add(cloudConnectionListener);
    }

    @Override
    public void unregisterCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        this.cloudConnectionListeners.remove(cloudConnectionListener);
    }

    @Override
    public void registerCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        this.cloudDeliveryListeners.add(cloudDeliveryListener);
    }

    @Override
    public void unregisterCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        this.cloudDeliveryListeners.remove(cloudDeliveryListener);
    }
}
