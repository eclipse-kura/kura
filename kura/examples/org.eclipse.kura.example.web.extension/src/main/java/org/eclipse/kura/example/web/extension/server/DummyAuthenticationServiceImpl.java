/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.example.web.extension.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.eclipse.kura.example.web.extension.shared.service.DummyAuthenticationService;
import org.eclipse.kura.web.api.Console;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

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
        try {
            super.service(req, res);
        } finally {
            Thread.currentThread().setContextClassLoader(orig);
        }
    }

    public DummyAuthenticationServiceImpl(final Console console) {
        this.console = console;
    }

    @Override
    public String login(final String user) {
        return console.setAuthenticated(getThreadLocalRequest().getSession(false), user);
    }

}
