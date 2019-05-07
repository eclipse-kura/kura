/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     
 *******************************************************************************/
package org.eclipse.kura.http.server.manager;

import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.crypto.CryptoService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpService {

    private static final Logger logger = LoggerFactory.getLogger(HttpService.class);

    private CryptoService cryptoService;
    
    private BundleContext bundleContext;
    private HttpServiceOptions options;
    
    public void setCryptoService(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    public void unsetCryptoService(CryptoService cryptoService) {
        this.cryptoService = null;
    }
    

    public void activate(ComponentContext context, Map<String, Object> properties) {
        logger.info("Activating {}", this.getClass().getSimpleName());

        this.options = new HttpServiceOptions(properties);

        setSystemProperties();

        this.bundleContext = context.getBundleContext();
        activateHttpService();

    }

    public void updated(Map<String, Object> properties) {
        logger.info("Updating {}", this.getClass().getSimpleName());

        this.options = new HttpServiceOptions(properties);

        deactivateHttpService();
        activateHttpService();

        logger.info("Updating... Done.");
    }

    public void deactivate() {
        logger.info("Deactivating {}", this.getClass().getSimpleName());

        deactivateHttpService();
    }

    private void setSystemProperties() {
        System.setProperty("org.eclipse.equinox.http.jetty.https.port", Integer.toString(this.options.getHttpsPort()));
        System.setProperty("org.eclipse.equinox.http.jetty.https.enabled",
                Boolean.toString(this.options.isHttpsEnabled()));
        System.setProperty("org.eclipse.equinox.http.jetty.https.host", "0.0.0.0");
        System.setProperty("org.eclipse.equinox.http.jetty.ssl.keystore", this.options.getHttpsKeystorePath());
        
        char[] decryptedPassword;
        try {
            decryptedPassword = this.cryptoService.decryptAes(this.options.getHttpsKeystorePassword());
        } catch (KuraException e) {
            logger.warn("Unable to decrypt property password");
            decryptedPassword = this.options.getHttpsKeystorePassword();
        }
        System.setProperty("org.eclipse.equinox.http.jetty.ssl.password",
                new String(decryptedPassword));
        
        System.setProperty("org.osgi.service.http.port", Integer.toString(this.options.getHttpPort()));
        System.setProperty("org.eclipse.equinox.http.jetty.http.enabled",
                Boolean.toString(this.options.isHttpEnabled()));
    }

    private void activateHttpService() {
        for (Bundle bundle : this.bundleContext.getBundles()) {
            if (bundle.getSymbolicName().contains("org.eclipse.equinox.http.jetty")) {
                try {
                    bundle.start();
                } catch (BundleException e) {
                    logger.error("Could not start Jetty Web server", e);
                }
            }
        }
    }

    private void deactivateHttpService() {
        for (Bundle bundle : this.bundleContext.getBundles()) {
            if (bundle.getSymbolicName().contains("org.eclipse.equinox.http.jetty")) {
                try {
                    bundle.stop();
                } catch (BundleException e) {
                    logger.error("Could not start Jetty Web server", e);
                }
            }
        }
    }

}
