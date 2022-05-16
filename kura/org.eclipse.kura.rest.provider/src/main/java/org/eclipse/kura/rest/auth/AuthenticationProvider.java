/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.auth;

import java.security.Principal;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A service interface that allows to register custom authentication providers for the {@code RestService}.
 * Registered {@link AuthenticationProvider} instances will be considered along with the currently enabled
 * built in authentication methods (e.g password and certificate authentication).<br>
 * 
 * The registered authentication providers will be called in order, the first provider whose
 * {@link AuthenticationProvider#authenticate(HttpServletRequest, ContainerRequestContext)} returns a non-empty optional
 * will determine a successful authentication.
 * If all providers return an empty optional the call failing with 401 status.<br>
 * 
 * The order in which the providers are called can be configured with the {@link javax.annotation.Priority} annotation.
 * Lower {@link javax.annotation.Priority#value()} values mean higher priority.
 * The priorities of the built-in authentication providers is the following:
 * <ul>
 * <li>Certificate authentication: 100</li>
 * <li>Password authentication: 200</li>
 * </ul>
 */
@ProviderType
public interface AuthenticationProvider {

    /**
     * This method is called when the {@code RestService} tracks the {@link AuthenticationProvider}
     */
    public void onEnabled();

    /**
     * This method is called when the {@code RestService} stops tracking the {@link AuthenticationProvider}
     */
    public void onDisabled();

    /**
     * Called by the {@code RestService} to authenticate a REST call.
     * This method should attempt to associate the request with a {@link Principal}, the {@link Principal#getName()}
     * method must
     * return a Kura identity name.
     * 
     * @param request
     *            The received {@link HttpServletRequest}
     * @param requestContext
     *            The received {@link ContainerRequestContext}
     * @return a non-empty optional reporting the identity name if the authentication is successful, or an empty
     *         optional if the request cannot be authenticated.
     */
    public Optional<Principal> authenticate(final HttpServletRequest request,
            final ContainerRequestContext requestContext);
}
