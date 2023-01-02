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
 ******************************************************************************/
package org.eclipse.kura.core.data;

public class AlwaysConnectedStrategy implements AutoConnectStrategy {

    private final ConnectionManager connectionManager;

    public AlwaysConnectedStrategy(final ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        connectionManager.startConnectionTask();
    }

    @Override
    public void onConnectionEstablished() {
        // do nothing
    }

    @Override
    public void onDisconnecting() {
        // do nothing
    }

    @Override
    public void onDisconnected() {
        connectionManager.startConnectionTask();
    }

    @Override
    public void onConnectionLost(Throwable cause) {
        connectionManager.stopConnectionTask();
        connectionManager.startConnectionTask();
    }

    @Override
    public void onMessageArrived(String topic, byte[] payload, int qos, boolean retained) {
        // do nothing
    }

    @Override
    public void onMessagePublished(int messageId, String topic) {
        // do nothing
    }

    @Override
    public void onMessageConfirmed(int messageId, String topic) {
        // do nothing
    }

    @Override
    public void shutdown() {
        connectionManager.stopConnectionTask();
    }

    @Override
    public void onPublishRequested(String topic, byte[] payload, int qos, boolean retain, int priority) {
        // do nothing
    }

}
