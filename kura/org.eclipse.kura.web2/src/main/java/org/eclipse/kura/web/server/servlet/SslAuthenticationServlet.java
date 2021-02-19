/*******************************************************************************
 * Copyright (c) 2020, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.server.servlet;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Optional;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.audit.AuditConstants;
import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.web.Console;
import org.eclipse.kura.web.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SslAuthenticationServlet extends HttpServlet {

    private static final Logger auditLogger = LoggerFactory.getLogger("AuditLogger");
    private static final Logger logger = LoggerFactory.getLogger(SslAuthenticationServlet.class);

    /**
     * 
     */
    private static final long serialVersionUID = -2371828320004624864L;

    private final String redirectPath;
    private final UserManager userManager;

    public SslAuthenticationServlet(final String redirectPath, final UserManager userManager) {
        this.redirectPath = redirectPath;
        this.userManager = userManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        final Console console = Console.instance();
        final HttpSession session = console.createSession(req);

        if (console.getUsername(session).isPresent()) {
            sendRedirect(resp, redirectPath);
            return;
        }

        final AuditContext auditContext = Console.instance().initAuditContext(req);

        try {
            if (!Console.getConsoleOptions().isAuthenticationMethodEnabled("Certificate")) {
                throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION);
            }

            final X509Certificate[] clientCertificates = (X509Certificate[]) req
                    .getAttribute("javax.servlet.request.X509Certificate");

            if (clientCertificates == null || clientCertificates.length == 0) {
                throw new IllegalArgumentException("Certificate chain is empty");
            }

            final LdapName ldapName = new LdapName(clientCertificates[0].getSubjectX500Principal().getName());

            final Optional<Rdn> commonNameRdn = ldapName.getRdns().stream()
                    .filter(r -> "cn".equalsIgnoreCase(r.getType())).findAny();

            if (!commonNameRdn.isPresent()) {
                throw new IllegalArgumentException("Certificate common name is not present");
            }

            final String commonName = (String) commonNameRdn.get().getValue();

            if (!userManager.getUserConfig(commonName).isPresent()) {
                throw new IllegalArgumentException("Common Name " + commonName + " is not associated with an user");
            }

            auditContext.getProperties().put(AuditConstants.KEY_IDENTITY.getValue(), commonName);

            console.setAuthenticated(session, commonName, auditContext);

            auditLogger.info("{} UI Login - Success - Certificate login", auditContext);
            sendRedirect(resp, redirectPath);

        } catch (final Exception e) {
            auditLogger.info("{} UI Login - Failure - Certificate login", auditContext);
            logger.warn("certificate authentication failed", e);
            sendUnauthorized(resp);
        }

    }

    private void sendUnauthorized(final HttpServletResponse resp) {
        try {
            resp.sendError(401);
        } catch (IOException e) {
            logger.warn("failed to send error", e);
        }
    }

    private void sendRedirect(final HttpServletResponse resp, final String path) {
        try {
            resp.sendRedirect(path);
        } catch (final IOException e) {
            logger.warn("failed to send redirect", e);
        }
    }
}
