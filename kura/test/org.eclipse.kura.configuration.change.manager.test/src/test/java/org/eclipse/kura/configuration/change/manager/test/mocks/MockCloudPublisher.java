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

package org.eclipse.kura.configuration.change.manager.test.mocks;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloudconnection.listener.CloudConnectionListener;
import org.eclipse.kura.cloudconnection.listener.CloudDeliveryListener;
import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class MockCloudPublisher implements CloudPublisher {

    private List<CountDownLatch> publishLatches = new LinkedList<>();
    private List<String> publishedPids = new LinkedList<>();

    public void addPublishCountLatch(CountDownLatch publishLatch) {
        this.publishLatches.add(publishLatch);
    }

    @Override
    public String publish(KuraMessage message) throws KuraException {
        JsonArray array = JsonParser.parseString(new String(message.getPayload().getBody())).getAsJsonArray();
        for (JsonElement element : array) {
            this.publishedPids.add(element.getAsJsonObject().get("pid").getAsString());
        }

        this.publishLatches.remove(0).countDown();
        return null;
    }

    @Override
    public void registerCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        // nothing to do

    }

    @Override
    public void unregisterCloudConnectionListener(CloudConnectionListener cloudConnectionListener) {
        // nothing to do

    }

    @Override
    public void registerCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        // nothing to do

    }

    @Override
    public void unregisterCloudDeliveryListener(CloudDeliveryListener cloudDeliveryListener) {
        // nothing to do
    }
    
    public synchronized boolean isPidPublished(String pid) {
        return this.publishedPids.remove(pid);
    }
}
