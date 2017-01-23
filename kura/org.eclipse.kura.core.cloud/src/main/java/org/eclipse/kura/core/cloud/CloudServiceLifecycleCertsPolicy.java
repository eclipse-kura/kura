/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
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
