/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.core.testutil.http;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class TestServer implements Closeable {

    private final Server server;
    private final Optional<Consumer<String>> downloadListener;
    private final Map<String, byte[]> resources = new HashMap<>();

    public TestServer(final int port, final Optional<Consumer<String>> downloadListener) throws Exception {
        this.server = new Server();
        this.downloadListener = downloadListener;

        final ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        this.server.setConnectors(new ServerConnector[] { connector });

        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        this.server.setHandler(context);

        context.addServlet(new ServletHolder(new DownloadServlet()), "/*");
        this.server.start();
    }

    public void setResource(final String path, final byte[] data) {
        this.resources.put(path, data);
    }

    private class DownloadServlet extends HttpServlet {

        /**
         * 
         */
        private static final long serialVersionUID = -3832275612143600045L;

        @Override
        protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
                throws ServletException, IOException {

            final String uri = req.getRequestURI();

            downloadListener.ifPresent(l -> l.accept(uri));

            final Optional<byte[]> data = Optional.ofNullable(resources.get(uri));

            try {
                if (data.isPresent()) {
                    resp.setStatus(200);
                    resp.setContentType("application/pkix-crl");
                    final OutputStream out = resp.getOutputStream();
                    out.write(data.get());
                    out.flush();
                } else {
                    resp.sendError(404);
                }
            } catch (final IOException e) {
                // do nothing
            }
        }

    }

    @Override
    public void close() throws IOException {
        try {
            this.server.stop();
        } catch (final Exception e) {
            throw new IOException(e);
        }
    }

}
