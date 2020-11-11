/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.server.servlet;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
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
import org.eclipse.kura.web.Console;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SslAuthenticationServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(SslAuthenticationServlet.class);

    /**
     * 
     */
    private static final long serialVersionUID = -2371828320004624864L;

    private final String redirectPath;

    public SslAuthenticationServlet(final String redirectPath) {
        this.redirectPath = redirectPath;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        final Console console = Console.instance();
        final HttpSession session = console.createSession(req);

        if (console.getUsername(session).isPresent()) {
            sendRedirect(resp, redirectPath);
            return;
        }

        try {
            if (!Arrays.stream(Console.getConsoleOptions().getEnabledAuthMethods()).anyMatch("Certificate"::equals)) {
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

            console.setAuthenticated(session, Console.getConsoleOptions().getUsername());
            sendRedirect(resp, redirectPath);

        } catch (final Exception e) {
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
