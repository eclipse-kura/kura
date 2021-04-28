/*******************************************************************************
 * Copyright (c) 2019, 2021 Eurotech and/or its affiliates and others
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

import java.security.KeyStore;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;

import javax.net.ssl.KeyManager;

import org.eclipse.equinox.http.jetty.JettyConfigurator;
import org.eclipse.equinox.http.jetty.JettyConstants;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.security.keystore.KeystoreService;
import org.eclipse.kura.system.SystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpService implements ConfigurableComponent {

    private static final String KURA_JETTY_PID = "kura.default";

    private static final Logger logger = LoggerFactory.getLogger(HttpService.class);

    private HttpServiceOptions options;

    private SystemService systemService;

    private KeystoreService keystoreService;

    public void setSystemService(SystemService systemService) {
        this.systemService = systemService;
    }

    public void setKeystoreService(KeystoreService keystoreService) {
        this.keystoreService = keystoreService;
    }

    public void activate(Map<String, Object> properties) {
        logger.info("Activating {}", this.getClass().getSimpleName());

        this.options = new HttpServiceOptions(properties, this.systemService.getKuraHome());

        activateHttpService();

        logger.info("Activating... Done.");
    }

    public void updated(Map<String, Object> properties) {
        logger.info("Updating {}", this.getClass().getSimpleName());

        HttpServiceOptions updatedOptions = new HttpServiceOptions(properties, this.systemService.getKuraHome());

        if (!this.options.equals(updatedOptions)) {
            logger.debug("Updating, new props");
            this.options = updatedOptions;

            deactivateHttpService();

            activateHttpService();
        }

        logger.info("Updating... Done.");
    }

    public void deactivate() {
        logger.info("Deactivating {}", this.getClass().getSimpleName());

        deactivateHttpService();
    }

    private Dictionary<String, Object> getJettyConfig() {

        final Dictionary<String, Object> jettyConfig = new Hashtable<>();

        final Set<Integer> httpPorts = this.options.getHttpPorts();
        final Set<Integer> httpsPorts = this.options.getHttpsPorts();
        final Set<Integer> httpsWithClientAuthPorts = this.options.getHttpsClientAuthPorts();

        final boolean isHttpEnabled = !httpPorts.isEmpty();
        final boolean isHttpsEnabled = !httpsPorts.isEmpty() || !httpsWithClientAuthPorts.isEmpty();

        jettyConfig.put(JettyConstants.HTTP_ENABLED, isHttpEnabled);

        if (isHttpEnabled) {
            jettyConfig.put("org.eclipse.kura.http.ports", httpPorts);
        }

        final String customizerClass = System
                .getProperty(JettyConstants.PROPERTY_PREFIX + JettyConstants.CUSTOMIZER_CLASS);

        if (customizerClass instanceof String) {
            jettyConfig.put(JettyConstants.CUSTOMIZER_CLASS, customizerClass);
        }

        if (!isHttpsEnabled) {
            return jettyConfig;
        }

        final KeystoreService currentKeystoreService = this.keystoreService;

        if (currentKeystoreService == null) {
            logger.warn("HTTPS is enabled but keystore service is not configured properly, disabling HTTPS");
            jettyConfig.put(JettyConstants.HTTPS_ENABLED, false);
            jettyConfig.put("kura.https.client.auth.enabled", false);
            return jettyConfig;
        }

        jettyConfig.put(JettyConstants.HTTPS_ENABLED, isHttpsEnabled);

        if (!httpsPorts.isEmpty()) {
            jettyConfig.put("org.eclipse.kura.https.ports", httpsPorts);
        }
        if (!httpsWithClientAuthPorts.isEmpty()) {
            jettyConfig.put("org.eclipse.kura.https.client.auth.ports", httpsWithClientAuthPorts);
        }

        jettyConfig.put(JettyConstants.HTTPS_HOST, "0.0.0.0");

        jettyConfig.put("org.eclipse.kura.keystore.provider", (Callable<KeyStore>) currentKeystoreService::getKeyStore);
        jettyConfig.put("org.eclipse.kura.keymanager.provider", (Function<String, List<KeyManager>>) a -> {
            try {
                return currentKeystoreService.getKeyManagers(a);
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        });
        jettyConfig.put(JettyConstants.SSL_KEYSTORE, "/tmp/foo");
        jettyConfig.put(JettyConstants.SSL_PASSWORD, "foo");

        final boolean isRevocationEnabled = this.options.isRevocationEnabled();

        jettyConfig.put("org.eclipse.kura.revocation.check.enabled", isRevocationEnabled);

        final Optional<String> ocspURI = this.options.getOcspURI();
        final Optional<String> crlPath = this.options.getCrlPath();
        final boolean softFail = this.options.isRevocationSoftFailEnabled();

        if (isRevocationEnabled) {
            if (ocspURI.isPresent() && !ocspURI.get().trim().isEmpty()) {
                jettyConfig.put("org.eclipse.kura.revocation.ocsp.uri", ocspURI.get());
            }
            if (crlPath.isPresent() && !crlPath.get().trim().isEmpty()) {
                jettyConfig.put("org.eclipse.kura.revocation.crl.path", crlPath.get());
            }
            jettyConfig.put("org.eclipse.kura.revocation.soft.fail", softFail);
        }

        return jettyConfig;
    }

    private void activateHttpService() {
        try {
            logger.info("starting Jetty instance...");
            JettyConfigurator.startServer(KURA_JETTY_PID, getJettyConfig());
            logger.info("starting Jetty instance...done");
        } catch (final Exception e) {
            logger.error("Could not start Jetty Web server", e);
        }
    }

    private void deactivateHttpService() {
        try {
            logger.info("stopping Jetty instance...");
            JettyConfigurator.stopServer(KURA_JETTY_PID);
            logger.info("stopping Jetty instance...done");
        } catch (final Exception e) {
            logger.error("Could not stop Jetty Web server", e);
        }
    }

}