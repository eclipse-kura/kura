/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.cloudconnection.message;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.message.KuraPayload;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The class KuraMessage represents a message that is shared between an application running in the framework and, for
 * example, a {@link org.eclipse.kura.cloudconnection.publisher.CloudPublisher} or
 * {@link org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber}.
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
