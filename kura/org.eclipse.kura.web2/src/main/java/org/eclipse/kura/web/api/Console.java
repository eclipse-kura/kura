/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.api;

import java.util.Optional;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.eclipse.kura.KuraException;
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

    public String setAuthenticated(final HttpSession session, final String user);

    public void checkXSRFToken(final HttpSession session, final String token) throws KuraException;
}
