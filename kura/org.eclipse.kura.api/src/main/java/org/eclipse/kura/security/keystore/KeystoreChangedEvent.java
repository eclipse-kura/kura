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
 *
 *******************************************************************************/
package org.eclipse.kura.security.keystore;

import java.util.Collections;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.event.Event;

/**
 * A OSGi EventAdmin <code>Event</code> sent to report a change in a keystore managed by a {@link KeystoreService}.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @since 2.2
 */
@ProviderType
public class KeystoreChangedEvent extends Event {

    /**
     * The EventAdmin topic of this event.
     */
    public static final String EVENT_TOPIC = "org/eclipse/kura/security/keystore/KeystoreChangedEvent/KEYSTORE_CHANGED";

    /**
     * The key of a Event property containing the kura.service.pid of the sender {@link KeystoreService}
     */
    public static final String SENDER_PID_PROPERTY_KEY = "sender.pid";

    public KeystoreChangedEvent(final String keystoreServicePid) {
        super(EVENT_TOPIC, Collections.singletonMap(SENDER_PID_PROPERTY_KEY, keystoreServicePid));
    }

    /**
     * Returns the kura.service.pid of the sender {@link KeystoreService}.
     * 
     * @return the kura.service.pid of the sender {@link KeystoreService}.
     */
    public String getSenderPid() {
        return (String) getProperty(SENDER_PID_PROPERTY_KEY);
    }
}
