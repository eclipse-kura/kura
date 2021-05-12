/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.audit;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Provides some well known audit context properties.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 3.0
 */
@ProviderType
public enum AuditConstants {

    /**
     * Contains an identifier for the entry point associated with an audit context. It must always be present.
     */
    KEY_ENTRY_POINT("entrypoint"),
    /**
     * Reports the IP address of the device that performed a request. It can be missing.
     */
    KEY_IP("ip"),
    /**
     * Reports the IP address of the Kura Identity associated with a request. It can be missing.
     */
    KEY_IDENTITY("identity"),
    /**
     * A value for the <code>entrypoint</property> that indicates a context created by an internal framework
     * component and not associated with an external entity.
     */
    ENTRY_POINT_INTERNAL("Internal");

    private final String value;

    AuditConstants(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
