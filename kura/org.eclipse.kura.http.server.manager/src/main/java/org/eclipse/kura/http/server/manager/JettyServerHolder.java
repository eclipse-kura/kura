/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.http.server.manager;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.PKIXRevocationChecker;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.Deflater;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.http.HttpFields.Mutable;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ErrorHandler.ErrorRequest;
import org.eclipse.jetty.session.DefaultSessionIdManager;
import org.eclipse.jetty.session.HouseKeeper;
import org.eclipse.jetty.util.compression.CompressionPool;
import org.eclipse.jetty.util.compression.DeflaterPool;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.http.server.manager.HttpServiceOptions.RevocationCheckMode;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.http.HttpServlet;

public class JettyServerHolder {

    private static final Logger logger = LoggerFactory.getLogger(JettyServerHolder.class);

    private DeflaterPool deflaterPool = new DeflaterPool(CompressionPool.DEFAULT_CAPACITY, Deflater.BEST_COMPRESSION,
            true);

    private final HttpServiceOptions options;
    private Server server;

    public JettyServerHolder(final HttpServiceOptions options, final Optional<KeystoreService> keystoreService,
            final HttpServlet httpServlet, final EventListener eventListener) {
        this.options = options;

        // TODO make it configurable from options
        this.server = new Server(new QueuedThreadPool(200, 8));
        // TODO restore after debugging
        // this.server.setErrorHandler(new KuraErrorHandler());

        for (int port : this.options.getHttpPorts()) {
            ServerConnector httpConnector = createHttpConnector(port);
            addConnector(httpConnector);
        }

        if (keystoreService.isPresent()) {
            for (int port : this.options.getHttpsPorts()) {
                try {
                    ServerConnector httpsConnector = createHttpsConnector(keystoreService.get(), port, false);
                    addConnector(httpsConnector);
                } catch (Exception e) {
                    logger.error("Unable to create https connector", e);
                }
            }
            for (int port : this.options.getHttpsClientAuthPorts()) {
                try {
                    ServerConnector httpsConnector = createHttpsConnector(keystoreService.get(), port, true);
                    addConnector(httpsConnector);
                } catch (Exception e) {
                    logger.error("Unable to create https connector", e);
                }
            }
        }

        ServletHolder holder = new ServletHolder(httpServlet);
        holder.setInitOrder(0);
        ServletContextHandler httpContext = createServletContextHandler();
        httpContext.addServlet(holder, "/*");
        httpContext.addEventListener(eventListener);

        this.server.setHandler(httpContext);

        DefaultSessionIdManager idMgr = new DefaultSessionIdManager(server);
        server.addBean(idMgr, true);

        HouseKeeper houseKeeper = new HouseKeeper();
        houseKeeper.setSessionIdManager(idMgr);
        idMgr.setSessionHouseKeeper(houseKeeper);
        try {
            server.start();
            SessionHandler sessionManager = httpContext.getSessionHandler();
            sessionManager.addEventListener(eventListener);
        } catch (Exception e) {
            logger.error("Unable to start Jetty server", e);
        }

    }

    private ServletContextHandler createServletContextHandler() {

        ServletContextHandler servletContextHandler = new ServletContextHandler();
        servletContextHandler.setClassLoader(this.getClass().getClassLoader());
        servletContextHandler.setContextPath("/"); // TODO evaluate configurability

        BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        File jettyWorkDir = new File(context.getDataFile(""), "jettyWorkDir_"); // TODO evaluate configurability
        jettyWorkDir.mkdir();

        servletContextHandler.setAttribute("jakarta.servlet.context.tempdir", jettyWorkDir);
        SessionHandler handler = new SessionHandler();
        // handler.setMaxInactiveInterval(-1); // TODO evaluate configurability
        servletContextHandler.setSessionHandler(handler);

        // TODO restore after debugging
        // final GzipHandler gzipHandler = new GzipHandler();
        // gzipHandler.setDeflaterPool(this.deflaterPool);
        //
        // servletContextHandler.insertHandler(gzipHandler);

        // TODO restore after debugging
        // servletContextHandler.setErrorHandler(new ErrorHandler() {
        //
        // @Override
        // protected void writeErrorPage(HttpServletRequest request, Writer writer, int code, String message,
        // boolean showStacks) throws IOException {
        // // do nothing
        // }
        //
        // });

        final SessionCookieConfig cookieConfig = servletContextHandler.getSessionHandler().getSessionCookieConfig();

        cookieConfig.setHttpOnly(true);

        return servletContextHandler;

    }

    private void addConnector(ServerConnector httpConnector) {
        try {
            httpConnector.open();
            this.server.addConnector(httpConnector);
        } catch (IOException e) {
            logger.error("Unable to start a connector {}", httpConnector, e);
        }
    }

    public ServerConnector createHttpConnector(int port) {

        HttpConfiguration httpConfiguration = new HttpConfiguration();
        httpConfiguration.addCustomizer(new BlockHttpMethods(EnumSet.of(HttpMethod.TRACE)));

        final ServerConnector newConnector = new ServerConnector(this.server,
                new HttpConnectionFactory(httpConfiguration));

        customizeConnector(newConnector, port);

        return newConnector;
    }

    private void customizeConnector(final ServerConnector serverConnector, final int port) {
        serverConnector.setPort(port);
        // serverConnector.setHost("0.0.0.0"); // TODO evaluate configurability
        for (final ConnectionFactory factory : serverConnector.getConnectionFactories()) {
            if (!(factory instanceof HttpConnectionFactory)) {
                continue;
            }

            ((HttpConnectionFactory) factory).getHttpConfiguration().setSendServerVersion(false);
        }
        addCustomizer(serverConnector, new ForwardedRequestCustomizer());
    }

    private void addCustomizer(final ServerConnector connector, final Customizer customizer) {
        for (final ConnectionFactory factory : connector.getConnectionFactories()) {
            if (!(factory instanceof HttpConnectionFactory)) {
                continue;
            }

            final HttpConnectionFactory httpConnectionFactory = (HttpConnectionFactory) factory;

            httpConnectionFactory.getHttpConfiguration().setSendServerVersion(false);

            List<Customizer> customizers = httpConnectionFactory.getHttpConfiguration().getCustomizers();
            if (customizers == null) {
                customizers = new LinkedList<>();
                httpConnectionFactory.getHttpConfiguration().setCustomizers(customizers);
            }

            customizers.add(customizer);
        }
    }

    private static class BlockHttpMethods implements HttpConfiguration.Customizer {

        private final Set<HttpMethod> blockedMethods;

        public BlockHttpMethods(Set<HttpMethod> methods) {
            this.blockedMethods = methods;
        }

        @Override
        public Request customize(Request request, Mutable responseHeaders) {
            if (this.blockedMethods.contains(HttpMethod.TRACE)) {
                return new ErrorRequest(request, HttpStatus.METHOD_NOT_ALLOWED_405, "Method now allowed.", null);
            }

            return request;
        }

    }

    private ServerConnector createHttpsConnector(final KeystoreService keystoreService, final int port,
            final boolean enableClientAuth) throws KuraException {

        final BaseSslContextFactory sslContextFactory;

        if (enableClientAuth) {
            sslContextFactory = new ClientAuthSslContextFactoryImpl(keystoreService, this.options.isRevocationEnabled(),
                    getRevocationCheckerOptions());
        } else {
            sslContextFactory = new BaseSslContextFactory(keystoreService);
        }

        final KeyStore keystore = keystoreService.getKeyStore();

        sslContextFactory.setKeyStore(keystore);
        sslContextFactory.setTrustStore(keystore);

        sslContextFactory.setProtocol("TLS");
        sslContextFactory.setTrustManagerFactoryAlgorithm("PKIX");

        sslContextFactory.setWantClientAuth(enableClientAuth);
        sslContextFactory.setNeedClientAuth(enableClientAuth);

        final HttpConfiguration httpsConfig = new HttpConfiguration();

        httpsConfig.addCustomizer(new SecureRequestCustomizer(false)); // TODO to check
        httpsConfig.addCustomizer(new BlockHttpMethods(EnumSet.of(HttpMethod.TRACE)));

        final ServerConnector connector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(httpsConfig));
        customizeConnector(connector, port);

        return connector;
    }

    private EnumSet<PKIXRevocationChecker.Option> getRevocationCheckerOptions() {
        if (!this.options.isRevocationEnabled()) {
            return EnumSet.noneOf(PKIXRevocationChecker.Option.class);
        }

        final EnumSet<PKIXRevocationChecker.Option> checkerOptions;

        if (this.options.getRevocationCheckMode() == RevocationCheckMode.CRL_ONLY) {
            checkerOptions = EnumSet.of(PKIXRevocationChecker.Option.PREFER_CRLS,
                    PKIXRevocationChecker.Option.NO_FALLBACK);
        } else if (this.options.getRevocationCheckMode() == RevocationCheckMode.PREFER_CRL) {
            checkerOptions = EnumSet.of(PKIXRevocationChecker.Option.PREFER_CRLS);
        } else {
            checkerOptions = EnumSet.noneOf(PKIXRevocationChecker.Option.class);
        }

        if (this.options.isRevocationSoftFailEnabled()) {
            checkerOptions.add(PKIXRevocationChecker.Option.SOFT_FAIL);
        }

        return checkerOptions;
    }

    public void stop() {
        try {
            this.server.stop();
        } catch (Exception e) {
            logger.warn("Unable to stop the Jetty server", e);
        }
    }

}
