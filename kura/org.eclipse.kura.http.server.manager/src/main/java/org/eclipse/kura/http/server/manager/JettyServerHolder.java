/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
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
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.PKIXRevocationChecker;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.Deflater;

import org.eclipse.jetty.ee10.servlet.ErrorHandler;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
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

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JettyServerHolder {

    private static final Logger logger = LoggerFactory.getLogger(JettyServerHolder.class);

    private DeflaterPool deflaterPool = new DeflaterPool(CompressionPool.DEFAULT_CAPACITY, Deflater.BEST_COMPRESSION,
            true);

    private final HttpServiceOptions options;
    private final Server server;
    private final File workDir;

    public JettyServerHolder(final HttpServiceOptions options, final Optional<KeystoreService> keystoreService,
            final HttpServlet httpServlet, final EventListener eventListener) {
        this.options = options;

        // TODO make it configurable from options
        this.server = new Server(new QueuedThreadPool(200, 8));
        this.server.setErrorHandler(new KuraErrorHandler());

        final BundleContext context = FrameworkUtil.getBundle(JettyServerHolder.class).getBundleContext();
        this.workDir = new File(context.getDataFile(""), "jettyWorkDir_" + System.nanoTime()); // TODO evaluate
                                                                                               // configurability
        this.workDir.mkdir();

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
        holder.setAsyncSupported(true);
        holder.setInitOrder(0);

        ServletContextHandler httpContext = createServletContextHandler();
        httpContext.addServlet(holder, "/*");
        httpContext.addFilter(new BlockHttpMethods(EnumSet.of(HttpMethod.TRACE)), "/*",
                EnumSet.of(DispatcherType.REQUEST));
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
        servletContextHandler.setClassLoader(JettyServerHolder.class.getClassLoader());
        servletContextHandler.setContextPath("/"); // TODO evaluate configurability

        servletContextHandler.setAttribute("jakarta.servlet.context.tempdir", this.workDir);
        SessionHandler handler = new SessionHandler();
        // handler.setMaxInactiveInterval(-1); // TODO evaluate configurability
        servletContextHandler.setSessionHandler(handler);

        final GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setDeflaterPool(this.deflaterPool);

        servletContextHandler.insertHandler(gzipHandler);

        servletContextHandler.setErrorHandler(new ErrorHandler() {

            @Override
            protected void writeErrorPage(HttpServletRequest request, Writer writer, int code, String message,
                    boolean showStacks) throws IOException {
                // do nothing
            }

        });

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

    private static class BlockHttpMethods implements Filter {

        private final Set<String> blockedMethods;

        public BlockHttpMethods(Set<HttpMethod> methods) {
            this.blockedMethods = methods.stream().map(m -> m.toString().toLowerCase()).collect(Collectors.toSet());
        }

        @Override
        public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain)
                throws IOException, ServletException {
            if (req instanceof HttpServletRequest && res instanceof HttpServletResponse) {
                final HttpServletRequest httpReq = (HttpServletRequest) req;
                final HttpServletResponse httpRes = (HttpServletResponse) res;

                if (blockedMethods.contains(httpReq.getMethod().toLowerCase())) {
                    httpRes.sendError(HttpStatus.METHOD_NOT_ALLOWED_405);
                    return;
                }

            }

            chain.doFilter(req, res);

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

        try (final Stream<Path> stream = Files.walk(this.workDir.toPath())) {
            stream.map(Path::toFile).sorted(Comparator.reverseOrder()).forEach(File::delete);
        } catch (Exception e) {
            logger.warn("Unable to cleanp jetty workdir", e);
        }
    }

}
