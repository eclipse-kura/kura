/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.endpoint;

import java.util.Map;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraDisconnectException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.CloudConnectionManager;
import org.eclipse.kura.cloudconnection.CloudEndpoint;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.subscriber.listener.CloudSubscriberListener;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.data.DataService;
import org.eclipse.kura.data.listener.DataServiceListener;
import org.osgi.service.event.EventAdmin;

public class SparkplugCloudEndpoint
        implements ConfigurableComponent, CloudEndpoint, CloudConnectionManager, DataServiceListener {

    /*
     * Activation APIs
     */

    private DataService dataService;
    private EventAdmin eventAdmin;

    public void setDataService(DataService dataService) {
        this.dataService = dataService;
    }

    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void activate(Map<String, Object> properties) {

    }

    public void update(Map<String, Object> properties) {

    }

    public void deactivate() {

    }

    /*
     * CloudEndpoint APIs
     */

    @Override
    public String publish(KuraMessage message) throws KuraException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void registerSubscriber(Map<String, Object> subscriptionProperties,
            CloudSubscriberListener cloudSubscriberListener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unregisterSubscriber(CloudSubscriberListener cloudSubscriberListener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void registerCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unregisterCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        // TODO Auto-generated method stub

    }

    /*
     * CloudConnectionManager APIs
     */

    @Override
    public void connect() throws KuraConnectException {
        // TODO Auto-generated method stub

    }

    @Override
    public void disconnect() throws KuraDisconnectException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isConnected() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void registerCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unregisterCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        // TODO Auto-generated method stub

    }

    /*
     * DataServiceListener APIs
     */

    @Override
    public void onConnectionEstablished() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDisconnecting() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDisconnected() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onConnectionLost(Throwable cause) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessagePublished(int messageId, String topic) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessageConfirmed(int messageId, String topic) {
        // TODO Auto-generated method stub

    }

}
