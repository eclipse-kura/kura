/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.cloud;

import org.eclipse.kura.cloudconnection.message.KuraMessage;
import org.osgi.annotation.versioning.ProviderType;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @deprecated Please consider using {@link KuraMessage} properties
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
            for (String m_resource : this.resources) {
                sb.append("/");
                sb.append(m_resource);
            }
        }
        return sb.toString();
    }
}
