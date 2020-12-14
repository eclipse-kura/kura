/*******************************************************************************
 * Copyright (c) 2018, 2020 Red Hat Inc and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Red Hat Inc
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.jetty.customizer;

import java.net.URI;
import java.security.KeyStore;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.X509CertSelector;
import java.util.Collection;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.servlet.SessionCookieConfig;

import org.eclipse.equinox.http.jetty.JettyConstants;
import org.eclipse.equinox.http.jetty.JettyCustomizer;
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
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class KuraJettyCustomizer extends JettyCustomizer {

    @Override
    public Object customizeContext(Object context, Dictionary<String, ?> settings) {
        if (!(context instanceof ServletContextHandler)) {
            return context;
        }

        final ServletContextHandler servletContextHandler = (ServletContextHandler) context;

        final GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setCompressionLevel(9);

        servletContextHandler.setGzipHandler(gzipHandler);

        servletContextHandler.setErrorHandler(new KuraErrorHandler());

        final SessionCookieConfig cookieConfig = servletContextHandler.getSessionHandler().getSessionCookieConfig();

        cookieConfig.setHttpOnly(true);

        return context;
    }

    @Override
    public Object customizeHttpConnector(final Object connector, final Dictionary<String, ?> settings) {
        customizeConnector(connector);
        return connector;
    }

    @Override
    public Object customizeHttpsConnector(final Object connector, final Dictionary<String, ?> settings) {
        if (!(connector instanceof ServerConnector)) {
            return connector;
        }

        final ServerConnector serverConnector = (ServerConnector) connector;

        if (getOrDefault(settings, "kura.https.no.client.auth.disabled", false)) {
            return createClientAuthSslConnector(serverConnector.getServer(), settings).orElse(null);
        }

        customizeConnector(connector);

        if (getOrDefault(settings, "kura.https.client.auth.enabled", false)) {

            final Optional<ServerConnector> httpsClientAuthConnector = createClientAuthSslConnector(
                    serverConnector.getServer(), settings);

            if (httpsClientAuthConnector.isPresent()) {
                serverConnector.getServer().addConnector(httpsClientAuthConnector.get());
            }
        }

        return connector;
    }

    private Optional<ServerConnector> createClientAuthSslConnector(final Server server,
            final Dictionary<String, ?> settings) {

        final SslContextFactory.Server sslContextFactory = new SslContextFactory.Server() {

            @Override
            protected PKIXBuilderParameters newPKIXBuilderParameters(KeyStore trustStore,
                    Collection<? extends java.security.cert.CRL> crls) throws Exception {
                PKIXBuilderParameters pbParams = new PKIXBuilderParameters(trustStore, new X509CertSelector());

                pbParams.setMaxPathLength(getMaxCertPathLength());
                pbParams.setRevocationEnabled(false);

                if (isEnableOCSP()) {

                    final PKIXRevocationChecker revocationChecker = (PKIXRevocationChecker) CertPathValidator
                            .getInstance("PKIX").getRevocationChecker();

                    final String responderURL = getOcspResponderURL();
                    if (responderURL != null) {
                        revocationChecker.setOcspResponder(new URI(responderURL));
                    }
                    final Object softFail = getOrDefault(settings, "org.eclipse.kura.revocation.soft.fail", false);
                    if (softFail instanceof Boolean && (boolean) softFail) {
                        revocationChecker.setOptions(EnumSet.of(PKIXRevocationChecker.Option.SOFT_FAIL,
                                PKIXRevocationChecker.Option.NO_FALLBACK));
                    }

                    pbParams.addCertPathChecker(revocationChecker);
                }

                if (getPkixCertPathChecker() != null) {
                    pbParams.addCertPathChecker(getPkixCertPathChecker());
                }

                if (crls != null && !crls.isEmpty()) {
                    pbParams.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(crls)));
                }

                if (isEnableCRLDP()) {
                    // Enable Certificate Revocation List Distribution Points (CRLDP) support
                    System.setProperty("com.sun.security.enableCRLDP", "true");
                }

                return pbParams;
            }
        };

        final Optional<String> keyStorePath = getOptional(settings, JettyConstants.SSL_KEYSTORE, String.class);
        final Optional<String> keyStorePassword = getOptional(settings, JettyConstants.SSL_PASSWORD, String.class);

        if (!(keyStorePath.isPresent() || !keyStorePassword.isPresent())) {
            return Optional.empty();
        }

        final boolean isRevocationEnabled = getOrDefault(settings, "org.eclipse.kura.revocation.check.enabled", true);

        sslContextFactory.setKeyStorePath(keyStorePath.get());
        sslContextFactory.setKeyStorePassword(keyStorePassword.get());
        sslContextFactory.setKeyStoreType("JKS");
        sslContextFactory.setProtocol("TLS");
        sslContextFactory.setTrustManagerFactoryAlgorithm("PKIX");

        sslContextFactory.setWantClientAuth(true);
        sslContextFactory.setNeedClientAuth(true);

        sslContextFactory.setEnableOCSP(isRevocationEnabled);
        sslContextFactory.setValidatePeerCerts(isRevocationEnabled);

        if (isRevocationEnabled) {
            getOptional(settings, "org.eclipse.kura.revocation.ocsp.uri", String.class)
                    .ifPresent(sslContextFactory::setOcspResponderURL);
            getOptional(settings, "org.eclipse.kura.revocation.crl.path", String.class)
                    .ifPresent(sslContextFactory::setCrlPath);
        }

        final HttpConfiguration httpsConfig = new HttpConfiguration();
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        final ServerConnector connector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(httpsConfig));
        connector.setPort(getOrDefault(settings, "kura.https.client.auth.port", 4443));

        return Optional.of(connector);
    }

    private void customizeConnector(Object connector) {
        if (!(connector instanceof ServerConnector)) {
            return;
        }

        final ServerConnector serverConnector = (ServerConnector) connector;

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

    @SuppressWarnings("unchecked")
    private static <T> T getOrDefault(final Dictionary<String, ?> properties, final String key, final T defaultValue) {
        final Object raw = properties.get(key);

        if (defaultValue.getClass().isInstance(raw)) {
            return (T) raw;
        }

        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private static <T> Optional<T> getOptional(final Dictionary<String, ?> properties, final String key,
            final Class<T> classz) {
        final Object raw = properties.get(key);

        if (classz.isInstance(raw)) {
            return Optional.of((T) raw);
        }

        return Optional.empty();
    }

}
