/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.cloud;

import org.osgi.annotation.versioning.ProviderType;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @deprecated Please consider using {@link org.eclipse.kura.cloudconnection.message.KuraMessage} properties
 */
@ProviderType
@Deprecated
public class CloudletTopic {

    public enum Method {
        GET,
        PUT,
        POST,
        DEL,
        EXEC;
    }

    private Method method;
    private String[] resources;

    public static CloudletTopic parseAppTopic(String appTopic) {
        CloudletTopic edcApplicationTopic = new CloudletTopic();

        String[] parts = appTopic.split("/");
        edcApplicationTopic.method = Method.valueOf(parts[0]);
        if (parts.length > 1) {

            edcApplicationTopic.resources = new String[parts.length - 1];
            for (int i = 0; i < edcApplicationTopic.resources.length; i++) {
                edcApplicationTopic.resources[i] = parts[i + 1];
            }
        }
        return edcApplicationTopic;
    }

    private CloudletTopic() {
        super();
    }

    public Method getMethod() {
        return this.method;
    }

    public String[] getResources() {
        return this.resources;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.method.name());
        if (this.resources != null) {
            for (String resource : this.resources) {
                sb.append("/");
                sb.append(resource);
            }
        }
        return sb.toString();
    }
}
