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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.transport;

import java.util.Map;

import org.eclipse.kura.KuraConnectException;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraNotConnectedException;
import org.eclipse.kura.KuraTimeoutException;
import org.eclipse.kura.KuraTooManyInflightMessagesException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.data.DataTransportToken;
import org.eclipse.kura.data.transport.listener.DataTransportListener;

public class SparkplugDataTransport implements ConfigurableComponent, DataTransportService {

    /*
     * Activation APIs
     */

    public void activate(Map<String, Object> properties) {

    }

    public void update(Map<String, Object> properties) {

    }

    public void deactivate() {

    }

    /*
     * DataTransportService APIs
     */

    @Override
    public void connect() throws KuraConnectException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isConnected() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getBrokerUrl() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getAccountName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getUsername() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getClientId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void disconnect(long quiesceTimeout) {
        // TODO Auto-generated method stub

    }

    @Override
    public void subscribe(String topic, int qos) throws KuraTimeoutException, KuraException, KuraNotConnectedException {
        // TODO Auto-generated method stub

    }

    @Override
    public void unsubscribe(String topic) throws KuraTimeoutException, KuraException, KuraNotConnectedException {
        // TODO Auto-generated method stub

    }

    @Override
    public DataTransportToken publish(String topic, byte[] payload, int qos, boolean retain)
            throws KuraTooManyInflightMessagesException, KuraException, KuraNotConnectedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addDataTransportListener(DataTransportListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeDataTransportListener(DataTransportListener listener) {
        // TODO Auto-generated method stub

    }

}
