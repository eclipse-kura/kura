/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.api;

import java.util.Optional;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.audit.AuditContext;
import org.osgi.service.http.NamespaceException;

public interface Console {

    public void registerLoginExtensionBundle(final ClientExtensionBundle extension);

    public void unregisterLoginExtensionBundle(final ClientExtensionBundle extension);

    public void registerConsoleExtensionBundle(final ClientExtensionBundle extension);

    public void unregisterConsoleExtensionBundle(final ClientExtensionBundle extension);

    public void registerSecuredServlet(final String path, final Servlet servlet)
            throws NamespaceException, ServletException;

    public void registerLoginServlet(final String path, final Servlet servlet)
            throws NamespaceException, ServletException;

    public void unregisterServlet(final String path) throws NamespaceException, ServletException;

    public Optional<String> getUsername(final HttpSession session);

    public AuditContext initAuditContext(final HttpServletRequest req);

    public String setAuthenticated(final HttpSession session, final String user, final AuditContext context);

    public void checkXSRFToken(final HttpServletRequest req, final String token) throws KuraException;
}
