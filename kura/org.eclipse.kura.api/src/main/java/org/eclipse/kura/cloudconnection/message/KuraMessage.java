/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.cloudconnection.message;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.cloudconnection.publisher.CloudPublisher;
import org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber;
import org.eclipse.kura.message.KuraPayload;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The class KuraMessage represents a message that is shared between an application running in the framework and, for
 * example, a {@link CloudPublisher} or {@link CloudSubscriber}.
 * It is composed by a {@link KuraPayload} and properties that enrich the context of the message.
 * The content of the {@code properties} field is not fixed or mandatory and represent message-related options.
 * Depending on the application, the value in the {@code properties} field can be used or not.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.0
 */
@ProviderType
public class KuraMessage {

    private final Map<String, Object> properties;
    private final KuraPayload payload;

    public KuraMessage(KuraPayload payload) {
        this.properties = new HashMap<>();
        this.payload = payload;
    }

    public KuraMessage(KuraPayload payload, Map<String, Object> properties) {
        this.properties = new HashMap<>(properties);
        this.payload = payload;
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }

    public KuraPayload getPayload() {
        return this.payload;
    }
}
