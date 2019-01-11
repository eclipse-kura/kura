/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.server;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.ssl.SslManagerServiceOptions;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.ssl.SslManagerService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtSslConfig;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtSslService;

public class GwtSslServiceImpl extends OsgiRemoteServiceServlet implements GwtSslService {

    /**
     *
     */
    private static final long serialVersionUID = -6676966456051889821L;

    private static final String SSL_PID = "org.eclipse.kura.ssl.SslManagerService";
    private static final String PROP_PROTOCOL = "ssl.default.protocol";
    private static final String PROP_TRUST_STORE = "ssl.default.trustStore";
    private static final String PROP_CIPHERS = "ssl.default.cipherSuites";
    private static final String PROP_HN_VERIFY = "ssl.hostname.verification";
    private static final String PROP_TRUST_PASSWORD = "ssl.keystore.password";

    private static final String PLACEHOLDER = "Placeholder";

    @Override
    public void updateSslConfiguration(GwtXSRFToken xsrfToken, GwtSslConfig sslConfig) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        try {
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put(PROP_PROTOCOL, sslConfig.getProtocol());
            properties.put(PROP_HN_VERIFY, sslConfig.isHostnameVerification());
            properties.put(PROP_TRUST_STORE, sslConfig.getKeyStore());
            if (PLACEHOLDER.equals(sslConfig.getKeystorePassword())) {
                CryptoService cryptoService = ServiceLocator.getInstance().getService(CryptoService.class);
                SslManagerServiceOptions options = getSslConfiguration();
                properties.put(PROP_TRUST_PASSWORD,
                        new Password(cryptoService.decryptAes(options.getSslKeystorePassword().toCharArray())));
            } else {
                properties.put(PROP_TRUST_PASSWORD, new Password(sslConfig.getKeystorePassword()));
            }
            properties.put(PROP_CIPHERS, sslConfig.getCiphers());

            ConfigurationService configService = ServiceLocator.getInstance().getService(ConfigurationService.class);

            configService.updateConfiguration(SSL_PID, properties);
        } catch (KuraException e) {
            throw new GwtKuraException(e.getMessage());
        }
    }

    @Override
    public GwtSslConfig getSslConfiguration(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);

        try {
            SslManagerServiceOptions options = getSslConfiguration();
            GwtSslConfig gwtSslConfig = new GwtSslConfig();
            gwtSslConfig.setProtocol(options.getSslProtocol());
            gwtSslConfig.setKeyStore(options.getSslKeyStore());
            gwtSslConfig.setCiphers(options.getSslCiphers());
            gwtSslConfig.setKeystorePassword(PLACEHOLDER);
            gwtSslConfig.setHostnameVerification(options.isSslHostnameVerification());
            return gwtSslConfig;
        } catch (KuraException e) {
            throw new GwtKuraException(GwtKuraErrorCode.ILLEGAL_ACCESS);
        }
    }

    private SslManagerServiceOptions getSslConfiguration() throws KuraException {
        SslManagerServiceOptions options=null;
        try {
            SslManagerService sslService = ServiceLocator.getInstance().getService(SslManagerService.class);
            //TODO: options = sslService.getConfigurationOptions();
            return options;
        } catch (GwtKuraException e) {
            throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION);
        }
    }

}
