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
package org.eclipse.kura.internal.rest.provider;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Optional;

import javax.annotation.Priority;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;

import org.eclipse.kura.audit.AuditConstants;
import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.rest.auth.AuthenticationProvider;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Priority(100)
public class CertificateAuthenticationProvider implements AuthenticationProvider {

    private final UserAdmin userAdmin;

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");
    private static final String KURA_USER_PREFIX = "kura.user.";

    public CertificateAuthenticationProvider(final UserAdmin userAdmin) {
        this.userAdmin = userAdmin;
    }

    @Override
    public Optional<Principal> authenticate(final HttpServletRequest request,
            final ContainerRequestContext requestContext) {
        final AuditContext auditContext = AuditContext.currentOrInternal();

        try {

            final Object clientCertificatesRaw = requestContext.getProperty("javax.servlet.request.X509Certificate");

            if (!(clientCertificatesRaw instanceof X509Certificate[])) {
                return Optional.empty();
            }

            final X509Certificate[] clientCertificates = (X509Certificate[]) clientCertificatesRaw;

            if (clientCertificates.length == 0) {
                throw new IllegalArgumentException("Certificate chain is empty");
            }

            final LdapName ldapName = new LdapName(clientCertificates[0].getSubjectX500Principal().getName());

            final Optional<Rdn> commonNameRdn = ldapName.getRdns().stream()
                    .filter(r -> "cn".equalsIgnoreCase(r.getType())).findAny();

            if (!commonNameRdn.isPresent()) {
                throw new IllegalArgumentException("Certificate common name is not present");
            }

            final String commonName = (String) commonNameRdn.get().getValue();

            auditContext.getProperties().put(AuditConstants.KEY_IDENTITY.getValue(), commonName);

            if (this.userAdmin.getRole(KURA_USER_PREFIX + commonName) instanceof User) {
                auditLogger.info("{} Rest - Success - Certificate Authentication succeeded", auditContext);
                return Optional.of(() -> commonName);
            }

            auditLogger.warn("{} Rest - Failure - Certificate Authentication failed", auditContext);
            return Optional.empty();

        } catch (final Exception e) {
            auditLogger.warn("{} Rest - Failure - Certificate Authentication failed", auditContext);
            return Optional.empty();
        }
    }

    @Override
    public void onEnabled() {
        // do nothing
    }

    @Override
    public void onDisabled() {
        // do nothing
    }

}
