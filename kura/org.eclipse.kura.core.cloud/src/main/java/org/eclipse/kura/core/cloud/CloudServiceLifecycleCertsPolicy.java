/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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

/**
 * This enum provides the necessary constants to denote the policy for the lifecycle messages.
 */
public enum CloudServiceLifecycleCertsPolicy {

    DISABLE_PUBLISHING("disable"),
    PUBLISH_BIRTH_CONNECT_ONLY("birth-connect"),
    PUBLISH_BIRTH_CONNECT_RECONNECT("birth-connect-reconnect");

    private String birthPolicy;

    private CloudServiceLifecycleCertsPolicy(String policy) {
        this.birthPolicy = policy;
    }

    public String getValue() {
        return this.birthPolicy;
    }
}
