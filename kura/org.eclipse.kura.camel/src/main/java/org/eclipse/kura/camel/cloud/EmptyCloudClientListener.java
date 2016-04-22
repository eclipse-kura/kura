/*******************************************************************************
 * Copyright (c) 2011, 2016 Red Hat and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.camel.cloud;

import org.eclipse.kura.cloud.CloudClientListener;
import org.eclipse.kura.message.KuraPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmptyCloudClientListener implements CloudClientListener {

    private final static Logger LOG = LoggerFactory.getLogger(EmptyCloudClientListener.class);

    @Override
    public void onControlMessageArrived(String deviceId, String topic, KuraPayload kuraPayload, int qos, boolean retain) {
        LOG.debug("Executed empty onControlMessageArrived callback with deviceId {}, topic {}, payload {}, QOS {} and retain {}. ",
                new Object[]{deviceId, topic, kuraPayload, qos, retain});
    }

    @Override
    public void onMessageArrived(String deviceId, String topic, KuraPayload kuraPayload, int qos, boolean retain) {

    }

    @Override
    public void onConnectionLost() {

    }

    @Override
    public void onConnectionEstablished() {

    }

    @Override
    public void onMessageConfirmed(int i, String s) {

    }

    @Override
    public void onMessagePublished(int i, String s) {

    }

}
