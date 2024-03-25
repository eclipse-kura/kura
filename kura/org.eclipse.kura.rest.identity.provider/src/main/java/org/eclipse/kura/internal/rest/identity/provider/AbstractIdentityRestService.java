/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.identity.provider;

import org.eclipse.kura.cloudconnection.request.RequestHandler;
import org.eclipse.kura.cloudconnection.request.RequestHandlerRegistry;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.request.handler.jaxrs.JaxRsRequestHandlerProxy;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;

public abstract class AbstractIdentityRestService {

    protected static final String DEBUG_MESSAGE = "Processing request for method '{}'";

    protected static final String REST_ROLE_NAME = "identity";
    private static final String KURA_PERMISSION_REST_ROLE = "kura.permission.rest." + REST_ROLE_NAME;

    private final RequestHandler requestHandler = new JaxRsRequestHandlerProxy(this);

    protected LegacyIdentityService legacyIdentityService;

    private CryptoService cryptoService;
    private UserAdmin userAdmin;
    private ConfigurationService configurationService;

    protected abstract Logger getLogger();

    protected abstract String getMqttApplicationId();

    public void bindCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void bindConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void bindUserAdmin(UserAdmin userAdmin) {
        this.userAdmin = userAdmin;
        this.userAdmin.createRole(KURA_PERMISSION_REST_ROLE, Role.GROUP);
    }

    public void bindRequestHandlerRegistry(RequestHandlerRegistry registry) {
        try {
            registry.registerRequestHandler(getMqttApplicationId(), this.requestHandler);
        } catch (final Exception e) {
            getLogger().warn("Failed to register {} request handler", getMqttApplicationId(), e);
        }
    }

    // Added mainly for testing purposes. Currently the service is created by this endpoint.
    public void bindLegacyIdentityService(LegacyIdentityService legacyIdentityService) {
        this.legacyIdentityService = legacyIdentityService;
    }

    public void unbindRequestHandlerRegistry(RequestHandlerRegistry registry) {
        try {
            registry.unregister(getMqttApplicationId());
        } catch (final Exception e) {
            getLogger().warn("Failed to unregister {} request handler", getMqttApplicationId(), e);
        }
    }

    public void activate() {
        // create only if not set externally. Added mainly for testing purposes.
        if (this.legacyIdentityService == null) {
            this.legacyIdentityService = new LegacyIdentityService(this.cryptoService, this.userAdmin,
                    this.configurationService);
        }
    }
}
