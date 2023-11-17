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
package org.eclipse.kura.internal.rest.cloudconnection.provider.dto;

public class PubSubFactoryInfo {

    private final String factoryPid;
    private final String cloudConnectionFactoryPid;
    private final String defaultPid;
    private final String defaultPidRegex;

    public PubSubFactoryInfo(String factoryPid, String cloudConnectionFactoryPid, String defaultPid,
            String defaultPidRegex) {

        this.factoryPid = factoryPid;
        this.cloudConnectionFactoryPid = cloudConnectionFactoryPid;
        this.defaultPid = defaultPid;
        this.defaultPidRegex = defaultPidRegex;
    }

    public String getFactoryPid() {
        return this.factoryPid;
    }

    public String getCloudConnectionFactoryPid() {
        return this.cloudConnectionFactoryPid;
    }

    public String getDefaultPid() {
        return this.defaultPid;
    }

    public String getDefaultPidRegex() {
        return this.defaultPidRegex;
    }

}
