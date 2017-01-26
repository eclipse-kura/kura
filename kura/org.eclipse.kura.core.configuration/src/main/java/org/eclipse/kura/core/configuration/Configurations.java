/*******************************************************************************
 * Copyright (c) 2016, 2017 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.core.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.crypto.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Configurations {

    private static final Logger logger = LoggerFactory.getLogger(Configurations.class);

    private Configurations() {
    }

    /**
     * Decrypt a list of configs
     * <p>
     * Actually performs a call to {@link #decryptPasswords(ComponentConfiguration, CryptoService)} for each entry in
     * the list.
     * </p>
     * 
     * @param configs
     *            the configs to decrypt
     * @param cryptoService
     *            the crypto service to use, may be {@code null}
     * @return a list of decrypted configs
     */
    static List<ComponentConfiguration> decryptPasswords(final List<ComponentConfiguration> configs,
            final CryptoService cryptoService) {
        if (configs == null || cryptoService == null) {
            return configs;
        }

        final List<ComponentConfiguration> result = new ArrayList<ComponentConfiguration>(configs.size());
        for (ComponentConfiguration config : configs) {
            result.add(decryptPasswords(config, cryptoService));
        }

        return result;
    }

    
    /**
     * Decrypt password on a configuration
     *
     * @param config
     *            the config to decrypt
     * @param cryptoService
     *            the crypto service to use, may be {@code null}
     * @return the decrypted configuration, may be the original input of no decryption was performed
     */
    static ComponentConfiguration decryptPasswords(final ComponentConfiguration config,
            final CryptoService cryptoService) {

        if (config == null) {
            return null;
        }
        if (config.getConfigurationProperties() == null) {
            return config;
        }

        final Map<String, Object> decryptedProperties = new HashMap<String, Object>(
                config.getConfigurationProperties().size());

        boolean touched = false;
        for (Entry<String, Object> property : config.getConfigurationProperties().entrySet()) {
            final String key = property.getKey();
            final Object value = property.getValue();

            if (value instanceof Password) {
                try {
                    decryptedProperties.put(key, decrypt((Password) value, cryptoService));
                    touched = true;
                } catch (KuraException e) {
                    decryptedProperties.put(key, value);
                    logger.warn("Failed to decode password", e);
                }

            } else {
                decryptedProperties.put(key, value);
            }
        }

        if (!touched) {
            return config;
        } else {
            final ComponentConfigurationImpl cci = new ComponentConfigurationImpl();
            cci.setPid(config.getPid());
            cci.setDefinition(config.getDefinition());
            cci.setProperties(decryptedProperties);
            return cci;
        }
    }

    private static Password decrypt(Password value, CryptoService cryptoService) throws KuraException {
        if (value == null || cryptoService == null) {
            return value;
        }

        char[] result = cryptoService.decryptAes(value.getPassword());

        if (result != null) {
            return new Password(result);
        } else {
            return null;
        }
    }
}
