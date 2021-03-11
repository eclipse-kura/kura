/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.example.web.extension.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.kura.audit.AuditContext;
import org.eclipse.kura.audit.AuditContext.Scope;
import org.eclipse.kura.example.web.extension.shared.service.DummyAuthenticationService;
import org.eclipse.kura.web.api.Console;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;

public class DummyAuthenticationServiceImpl extends RemoteServiceServlet implements DummyAuthenticationService {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private final Console console;

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        final ClassLoader orig = Thread.currentThread().getContextClassLoader();

        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        final AuditContext context = console.initAuditContext((HttpServletRequest) req);

        try (final Scope scope = AuditContext.openScope(context)) {
            super.service(req, res);
        } finally {
            Thread.currentThread().setContextClassLoader(orig);
        }
    }

    @Override
    protected SerializationPolicy doGetSerializationPolicy(HttpServletRequest request, String moduleBaseURL,
            String strongName) {

        try {
            final String modulePath = new URI(moduleBaseURL).getPath();
            final String policyPath = "/www" + modulePath + strongName + ".gwt.rpc";

            final InputStream in = getClass().getClassLoader().getResourceAsStream(policyPath);
            return SerializationPolicyLoader.loadFromStream(in, null);
        } catch (Exception e) {
            return null;
        }

    }

    public DummyAuthenticationServiceImpl(final Console console) {
        this.console = console;
    }

    @Override
    public String login(final String user) {
        return console.setAuthenticated(getThreadLocalRequest().getSession(false), user,
                AuditContext.currentOrInternal());
    }

}
