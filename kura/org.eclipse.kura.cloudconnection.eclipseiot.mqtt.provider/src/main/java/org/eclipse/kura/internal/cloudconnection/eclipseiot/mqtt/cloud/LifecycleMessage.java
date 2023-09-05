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
package org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.cloud;

import org.eclipse.kura.internal.cloudconnection.eclipseiot.mqtt.message.MessageType;
import org.eclipse.kura.message.KuraPayload;

public class LifecycleMessage {
    
    private StringBuilder topicBuilder;
    private CloudConnectionManagerOptions options;
    private LifeCyclePayloadBuilder payloadBuilder;
    private KuraPayload payload;

    public LifecycleMessage(CloudConnectionManagerOptions options, CloudConnectionManagerImpl cloudServiceImpl) {
        this.options = options;

        this.topicBuilder = new StringBuilder(MessageType.EVENT.getTopicPrefix());
        this.topicBuilder.append(this.options.getTopicSeparator()).append(this.options.getTopicSeparator())
                .append(this.options.getTopicSeparator());
        
        this.payloadBuilder = new LifeCyclePayloadBuilder(cloudServiceImpl);
    }

    public LifecycleMessage asBirthCertificateMessage() {
        this.topicBuilder.append(this.options.getTopicBirthSuffix());
        this.payload = this.payloadBuilder.buildBirthPayload();
        return this;
    }

    public LifecycleMessage asDisconnectCertificateMessage() {
        this.topicBuilder.append(this.options.getTopicDisconnectSuffix());
        this.payload = this.payloadBuilder.buildDisconnectPayload();
        return this;
    }

    public String getTopic() {
        return this.topicBuilder.toString();
    }

    public KuraPayload getPayload() {
        return this.payload;
    }

}
