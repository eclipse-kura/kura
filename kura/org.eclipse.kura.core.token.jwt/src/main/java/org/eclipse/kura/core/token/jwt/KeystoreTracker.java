/*******************************************************************************
 * Copyright (c) 2026 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.token.jwt;

import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.security.keystore.KeystoreChangedEvent;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.osgi.service.event.Event;

public final class KeystoreTracker {

    private Optional<KeystoreService> keystoreService = Optional.empty();
    private Optional<String> keystoreServicePid = Optional.empty();

    public void bind(final KeystoreService keystoreService, final Map<String, Object> properties) {
        this.keystoreService = Optional.of(keystoreService);
        this.keystoreServicePid = Optional.ofNullable((String) properties.get(ConfigurationService.KURA_SERVICE_PID));
    }

    public boolean unbind(final KeystoreService keystoreService) {
        if (this.keystoreService.isEmpty() || this.keystoreService.get() != keystoreService) {
            return false;
        }

        this.keystoreService = Optional.empty();
        this.keystoreServicePid = Optional.empty();

        return true;
    }

    public boolean isContentChangedBy(final Event event) {
        if (!(event instanceof KeystoreChangedEvent keystoreChangedEvent)) {
            return false;
        }

        final Optional<String> senderPid = Optional.ofNullable(keystoreChangedEvent.getSenderPid());

        return senderPid.isPresent() && this.keystoreServicePid.equals(senderPid);
    }

    public Optional<KeystoreService> get() {
        return this.keystoreService;
    }

}
