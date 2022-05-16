/*******************************************************************************
 * Copyright (c) 2011, 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.example.rest.authentication.provider;

import static java.util.Objects.isNull;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;

import org.eclipse.kura.audit.AuditConstants;
import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.rest.auth.AuthenticationProvider;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Priority(50)
public class ExampleRestAuthenticationProvider implements AuthenticationProvider {

    private static final String KURA_USER_PREFIX = "kura.user.";
    private static final String KURA_NEED_PASSWORD_CHANGE = "kura.need.password.change";
    private static final String KURA_PASSWORD_CREDENTIAL = "kura.password";

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");
    private static final Logger logger = LoggerFactory.getLogger(ExampleRestAuthenticationProvider.class);

    private UserAdmin userAdmin;
    private CryptoService cryptoService;

    public void bindUserAdmin(final UserAdmin userAdmin) {
        this.userAdmin = userAdmin;
    }

    public void bindCryptoService(final CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    @Override
    public void onEnabled() {
        logger.info("Example auth provider enabled");
    }

    @Override
    public void onDisabled() {
        logger.info("Example auth provider disabled");
    }

    @Override
    public Optional<Principal> authenticate(final HttpServletRequest request,
            final ContainerRequestContext requestContext) {

        final AuditContext auditContext = AuditContext.currentOrInternal();

        final Optional<String> username = getHeader(requestContext, "X-Example-Username");
        final Optional<String> password = getHeader(requestContext, "X-Example-Password");

        if (!username.isPresent() || !password.isPresent()) {
            return Optional.empty();
        }

        auditContext.getProperties().put(AuditConstants.KEY_IDENTITY.getValue(), username.get());

        final User user = (User) userAdmin.getRole(KURA_USER_PREFIX + username.get());

        if ("true".equals(user.getProperties().get(KURA_NEED_PASSWORD_CHANGE))) {
            return Optional.empty();
        }

        final String passwordHash = (String) user.getCredentials().get(KURA_PASSWORD_CREDENTIAL);

        if (isNull(passwordHash)) {
            return Optional.empty();
        }

        try {
            if (cryptoService.sha256Hash(password.get()).equals(passwordHash)) {
                auditLogger.info("{} Rest - Success - Example Password Authentication succeeded", auditContext);
                return Optional.of(username::get);
            } else {
                auditLogger.warn("{} Rest - Failure - Example Password Authentication failed", auditContext);
                return Optional.empty();
            }
        } catch (final Exception e) {
            auditLogger.warn("{} Rest - Failure - Example Password Authentication failed", auditContext);
            return Optional.empty();
        }
    }

    private final Optional<String> getHeader(final ContainerRequestContext requestContext, final String key) {
        final List<String> headers = requestContext.getHeaders().get(key);
        if (headers == null || headers.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(headers.get(0));
    }

}
