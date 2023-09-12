/*******************************************************************************
 * Copyright (c) 2022, 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.auth;

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
import org.eclipse.kura.util.useradmin.UserAdminHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
@Priority(100)
public class CertificateAuthenticationProvider implements AuthenticationProvider {

    private final UserAdminHelper userAdminHelper;

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");

    public CertificateAuthenticationProvider(final UserAdminHelper userAdminHelper) {
        this.userAdminHelper = userAdminHelper;
    }

    @Override
    public Optional<Principal> authenticate(final HttpServletRequest request,
            final ContainerRequestContext requestContext) {

        return authenticate(requestContext, "Certificate Authentication");
    }

    public Optional<Principal> authenticate(final ContainerRequestContext requestContext,
            final String auditAuthenticationKind) {
        final AuditContext auditContext = AuditContext.currentOrInternal();

        try {
            final Principal principal = this.authenticate(requestContext);

            auditLogger.info("{} Rest - Success - {} succeeded", auditContext, auditAuthenticationKind);

            return Optional.of(principal);

        } catch (final CertificateAuthException e) {
            if (e.getReason() == CertificateAuthException.Reason.IDENTITY_NOT_FOUND) {
                auditLogger.warn("{} Rest - Failure - {} failed", auditContext, auditAuthenticationKind);
            }

            return Optional.empty();
        }
    }

    public Principal authenticate(final ContainerRequestContext requestContext) throws CertificateAuthException {
        final AuditContext auditContext = AuditContext.currentOrInternal();

        try {

            final Object clientCertificatesRaw = requestContext.getProperty("javax.servlet.request.X509Certificate");

            if (!(clientCertificatesRaw instanceof X509Certificate[])) {
                throw new CertificateAuthException(CertificateAuthException.Reason.CLIENT_CERTIFICATE_CHAIN_MISSING);
            }

            final X509Certificate[] clientCertificates = (X509Certificate[]) clientCertificatesRaw;

            if (clientCertificates.length == 0) {
                throw new CertificateAuthException(CertificateAuthException.Reason.CLIENT_CERTIFICATE_CHAIN_MISSING);
            }

            final LdapName ldapName = new LdapName(clientCertificates[0].getSubjectX500Principal().getName());

            final Optional<Rdn> commonNameRdn = ldapName.getRdns().stream()
                    .filter(r -> "cn".equalsIgnoreCase(r.getType())).findAny();

            if (!commonNameRdn.isPresent()) {
                throw new CertificateAuthException(CertificateAuthException.Reason.MISSING_COMMON_NAME);
            }

            final String commonName = (String) commonNameRdn.get().getValue();

            auditContext.getProperties().put(AuditConstants.KEY_IDENTITY.getValue(), commonName);

            if (this.userAdminHelper.getUser(commonName).isPresent()) {
                return () -> commonName;
            }

            throw new CertificateAuthException(CertificateAuthException.Reason.IDENTITY_NOT_FOUND);

        } catch (final CertificateAuthException e) {

            throw e;
        } catch (final Exception e) {
            throw new CertificateAuthException(CertificateAuthException.Reason.UNEXPECTED_ERROR);
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

    public static class CertificateAuthException extends Exception {

        public enum Reason {
            CLIENT_CERTIFICATE_CHAIN_MISSING,
            MISSING_COMMON_NAME,
            IDENTITY_NOT_FOUND,
            UNEXPECTED_ERROR
        }

        private final Reason reason;

        public CertificateAuthException(final Reason reason) {
            this.reason = reason;
        }

        public Reason getReason() {
            return reason;
        }
    }

}
