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
package org.eclipse.kura.cloudconnection;


/**
 * Provides constants that are used by cloud connections to relate service instances to their respective
 * {@link CloudEndpoint} or {@link org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory}.
 *
 * @since 2.0
 */
public enum CloudConnectionConstants {

    /**
     * The key of the property that specifies the {@code kura.service.pid} of the associated
     * {@link CloudEndpoint} in {@link org.eclipse.kura.cloudconnection.publisher.CloudPublisher} 
     * or {@link org.eclipse.kura.cloudconnection.subscriber.CloudSubscriber} component configuration.
     */
    CLOUD_ENDPOINT_SERVICE_PID_PROP_NAME("cloud.endpoint.service.pid"),

    /**
     * The key of the property that specifies the {@code kura.service.pid} of the associated
     * {@link org.eclipse.kura.cloudconnection.factory.CloudConnectionFactory} in {@link CloudEndpoint} component definition.
     */
    CLOUD_CONNECTION_FACTORY_PID_PROP_NAME("cloud.connection.factory.pid");

    private String value;

    private CloudConnectionConstants(final String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }
}
