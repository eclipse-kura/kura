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
package org.eclipse.kura.core.cloud;

import org.eclipse.kura.message.KuraPayload;

public class LifecycleMessage {
    
    private StringBuilder topicBuilder;
    private LifeCyclePayloadBuilder payloadBuilder;
    private KuraPayload payload;
    private boolean isAppCertificateMessage = false;
    private boolean isBirthCertificateMessage = false;

    public LifecycleMessage(CloudServiceOptions options, CloudServiceImpl cloudServiceImpl) {
        this.topicBuilder = new StringBuilder(options.getTopicControlPrefix());
        this.topicBuilder.append(CloudServiceOptions.getTopicSeparator())
                .append(CloudServiceOptions.getTopicAccountToken())
                .append(CloudServiceOptions.getTopicSeparator())
                .append(CloudServiceOptions.getTopicClientIdToken())
                .append(CloudServiceOptions.getTopicSeparator());

        this.payloadBuilder = new LifeCyclePayloadBuilder(cloudServiceImpl);
    }

    public LifecycleMessage asBirthCertificateMessage() {
        this.topicBuilder.append(CloudServiceOptions.getTopicBirthSuffix());
        this.payload = this.payloadBuilder.buildBirthPayload();
        this.isBirthCertificateMessage = true;
        return this;
    }

    public LifecycleMessage asAppCertificateMessage() {
        this.topicBuilder.append(CloudServiceOptions.getTopicAppsSuffix());
        this.payload = this.payloadBuilder.buildBirthPayload();
        this.isAppCertificateMessage = true;
        return this;
    }

    public LifecycleMessage asDisconnectCertificateMessage() {
        this.topicBuilder.append(CloudServiceOptions.getTopicDisconnectSuffix());
        this.payload = this.payloadBuilder.buildDisconnectPayload();
        return this;
    }

    public String getTopic() {
        return this.topicBuilder.toString();
    }

    public KuraPayload getPayload() {
        return this.payload;
    }

    public boolean isAppCertificateMessage() {
        return this.isAppCertificateMessage;
    }

    public boolean isBirthCertificateMessage() {
        return this.isBirthCertificateMessage;
    }

}
