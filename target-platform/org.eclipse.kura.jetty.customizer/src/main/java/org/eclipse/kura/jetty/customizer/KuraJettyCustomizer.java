/*******************************************************************************
 * Copyright (c) 2018, 2021 Red Hat Inc and others
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
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;

import javax.net.ssl.KeyManager;
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

        servletContextHandler.getServer().setErrorHandler(new KuraErrorHandler());

        final GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setCompressionLevel(9);

        servletContextHandler.setGzipHandler(gzipHandler);

        servletContextHandler.setErrorHandler(new KuraErrorHandler());

        final SessionCookieConfig cookieConfig = servletContextHandler.getSessionHandler().getSessionCookieConfig();

        cookieConfig.setHttpOnly(true);

        return context;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object customizeHttpConnector(final Object connector, final Dictionary<String, ?> settings) {
        if (!(connector instanceof ServerConnector)) {
            return connector;
        }

        final ServerConnector serverConnector = (ServerConnector) connector;

        final Set<Integer> ports = (Set<Integer>) settings.get("org.eclipse.kura.http.ports");

        if (ports == null) {
            return null;
        }

        for (final int port : ports) {
            final ServerConnector newConnector = new ServerConnector(serverConnector.getServer(),
                    new HttpConnectionFactory(new HttpConfiguration()));

            customizeConnector(newConnector, port);
            serverConnector.getServer().addConnector(newConnector);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object customizeHttpsConnector(final Object connector, final Dictionary<String, ?> settings) {
        if (!(connector instanceof ServerConnector)) {
            return connector;
        }

        final ServerConnector serverConnector = (ServerConnector) connector;

        final Set<Integer> httpsPorts = (Set<Integer>) settings.get("org.eclipse.kura.https.ports");
        final Set<Integer> httpsClientAuthPorts = (Set<Integer>) settings
                .get("org.eclipse.kura.https.client.auth.ports");

        if (httpsPorts != null) {
            for (final int httpsPort : httpsPorts) {
                final Optional<ServerConnector> newConnector = createSslConnector(serverConnector.getServer(), settings,
                        httpsPort, false);
                newConnector.ifPresent(c -> serverConnector.getServer().addConnector(c));
            }
        }

        if (httpsClientAuthPorts != null) {
            for (final int clientAuthPort : httpsClientAuthPorts) {
                final Optional<ServerConnector> newConnector = createSslConnector(serverConnector.getServer(), settings,
                        clientAuthPort, true);
                newConnector.ifPresent(c -> serverConnector.getServer().addConnector(c));
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private Optional<ServerConnector> createSslConnector(final Server server, final Dictionary<String, ?> settings,
            final int port, final boolean enableClientAuth) {

        final BaseSslContextFactory sslContextFactory;

        if (enableClientAuth) {
            sslContextFactory = new ClientAuthSslContextFactoryImpl(settings);
        } else {
            sslContextFactory = new BaseSslContextFactory();
        }

        final Object keystoreProvider = settings.get("org.eclipse.kura.keystore.provider");
        final Object keyManagerProvider = settings.get("org.eclipse.kura.keymanager.provider");
        final Object crlStore = settings.get("org.eclipse.kura.crl.store");

        final Optional<String> keyStorePath = getOptional(settings, JettyConstants.SSL_KEYSTORE, String.class);
        final Optional<String> keyStorePassword = getOptional(settings, JettyConstants.SSL_PASSWORD, String.class);

        if (keystoreProvider instanceof Callable && keyManagerProvider instanceof Function) {

            try {
                final KeyStore keystore = ((Callable<KeyStore>) keystoreProvider).call();

                sslContextFactory.setKeyStore(keystore);
                sslContextFactory.setTrustStore(keystore);
            } catch (final Exception e) {
                return Optional.empty();
            }

            sslContextFactory.setKeyManagersProvider((Function<String, List<KeyManager>>) keyManagerProvider);

            if (crlStore instanceof CertStore) {
                sslContextFactory.setCRLStore((CertStore) crlStore);
            }

        } else if (keyStorePath.isPresent() && keyStorePassword.isPresent()) {
            sslContextFactory.setKeyStorePath(keyStorePath.get());
            sslContextFactory.setKeyStorePassword(keyStorePassword.get());
            sslContextFactory.setKeyStoreType("JKS");

        } else {
            return Optional.empty();
        }

        sslContextFactory.setProtocol("TLS");
        sslContextFactory.setTrustManagerFactoryAlgorithm("PKIX");

        sslContextFactory.setWantClientAuth(enableClientAuth);
        sslContextFactory.setNeedClientAuth(enableClientAuth);

        final HttpConfiguration httpsConfig = new HttpConfiguration();
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        final ServerConnector connector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(httpsConfig));
        customizeConnector(connector, port);

        return Optional.of(connector);
    }

    private void customizeConnector(final ServerConnector serverConnector, final int port) {
        serverConnector.setPort(port);
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

    private static class BaseSslContextFactory extends SslContextFactory.Server {

        private Optional<Function<String, List<KeyManager>>> keyManagersProvider = Optional.empty();
        private Optional<CertStore> crlStore = Optional.empty();

        public void setCRLStore(final CertStore crlStore) {
            this.crlStore = Optional.of(crlStore);
        }

        public Optional<CertStore> getCRLStore() {
            return crlStore;
        }

        public void setKeyManagersProvider(Function<String, List<KeyManager>> keyManagersProvider) {
            this.keyManagersProvider = Optional.of(keyManagersProvider);
        }

        @Override
        protected KeyManager[] getKeyManagers(KeyStore keyStore) throws Exception {
            if (keyManagersProvider.isPresent()) {
                return keyManagersProvider.get().apply(getKeyManagerFactoryAlgorithm()).toArray(new KeyManager[0]);
            }

            return super.getKeyManagers(keyStore);
        }

    }

    private static final class ClientAuthSslContextFactoryImpl extends BaseSslContextFactory {

        private final Dictionary<String, ?> settings;

        private ClientAuthSslContextFactoryImpl(Dictionary<String, ?> settings) {
            this.settings = settings;

            final boolean isRevocationEnabled = getOrDefault(settings, "org.eclipse.kura.revocation.check.enabled",
                    true);

            setValidatePeerCerts(isRevocationEnabled);
        }

        @Override
        protected PKIXBuilderParameters newPKIXBuilderParameters(KeyStore trustStore,
                Collection<? extends java.security.cert.CRL> crls) throws Exception {
            PKIXBuilderParameters pbParams = new PKIXBuilderParameters(trustStore, new X509CertSelector());

            final boolean isRevocationEnabled = getOrDefault(settings, "org.eclipse.kura.revocation.check.enabled",
                    true);

            pbParams.setMaxPathLength(getMaxCertPathLength());
            pbParams.setRevocationEnabled(isRevocationEnabled);

            final PKIXRevocationChecker revocationChecker = (PKIXRevocationChecker) CertPathValidator
                    .getInstance("PKIX").getRevocationChecker();

            final EnumSet<PKIXRevocationChecker.Option> revocationOptions = getOrDefault(settings,
                    "org.eclipse.kura.revocation.checker.options", EnumSet.noneOf(PKIXRevocationChecker.Option.class));

            revocationChecker.setOptions(revocationOptions);

            pbParams.addCertPathChecker(revocationChecker);

            if (getPkixCertPathChecker() != null) {
                pbParams.addCertPathChecker(getPkixCertPathChecker());
            }

            final Optional<CertStore> crlStore = getCRLStore();

            if (crlStore.isPresent()) {
                pbParams.addCertStore(crlStore.get());
            } else if (crls != null && !crls.isEmpty()) {
                pbParams.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(crls)));
            }

            return pbParams;
        }
    }

}
