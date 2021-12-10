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
 *******************************************************************************/
package org.eclipse.kura.rest.configuration.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.crypto.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DTOUtil {

    private static final Logger logger = LoggerFactory.getLogger(DTOUtil.class);

    private DTOUtil() {
    }

    public static ComponentConfigurationDTO toComponentConfigurationDTO(final ComponentConfiguration config,
            final CryptoService cryptoService, final boolean decryptPasswords) {

        return new ComponentConfigurationDTO(config.getPid(), ocdToDto(config.getDefinition()),
                configurationPropertiesToDtos(config.getConfigurationProperties(), cryptoService, decryptPasswords));
    }

    public static ComponentConfigurationList toComponentConfigurationList(final List<ComponentConfiguration> configs,
            final CryptoService cryptoService, final boolean decryptPasswords) {
        final List<ComponentConfigurationDTO> result = configs.stream()
                .map(c -> toComponentConfigurationDTO(c, cryptoService, decryptPasswords)).collect(Collectors.toList());

        return new ComponentConfigurationList(result);
    }

    public static Map<String, Object> dtosToConfigurationProperties(final Map<String, PropertyDTO> properties) {
        if (properties == null) {
            return null;
        }

        final Map<String, Object> result = new HashMap<>(properties.size());

        for (final Entry<String, PropertyDTO> e : properties.entrySet()) {

            if (e.getValue().getValue() == null) {
                result.put(e.getKey(), null);
            } else {
                final Object propertyValue = e.getValue().toConfigurationProperty()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Invalid property value for " + e.getKey() + " " + e.getValue()));

                result.put(e.getKey(), propertyValue);
            }
        }

        return result;
    }

    public static OcdDTO ocdToDto(final OCD ocd) {
        if (ocd == null) {
            return null;
        } else {
            return new OcdDTO(ocd);
        }
    }

    public static Map<String, PropertyDTO> configurationPropertiesToDtos(Map<String, Object> properties,
            final CryptoService cryptoService, final boolean decryptPasswords) {
        if (properties == null) {
            return null;
        }

        final Map<String, PropertyDTO> result = new HashMap<>();

        for (final Entry<String, Object> entry : properties.entrySet()) {

            final Optional<Object> value;

            if (entry.getValue() == null) {
                continue;
            }

            if (decryptPasswords) {
                value = decryptPassword(entry.getValue(), cryptoService);
            } else {
                value = Optional.ofNullable(entry.getValue());
            }

            final Optional<PropertyDTO> propertyDTO = value.flatMap(PropertyDTO::fromConfigurationProperty);

            if (!propertyDTO.isPresent()) {
                logger.warn("ignoring invalid configiration property for {}: {}", entry.getKey(), entry.getValue());
            } else {
                result.put(entry.getKey(), propertyDTO.get());
            }
        }

        return result;
    }

    public static Optional<Object> decryptPassword(final Object property, final CryptoService cryptoService) {
        try {
            final Object result;

            if (property instanceof Password) {
                result = new Password(cryptoService.decryptAes(((Password) property).getPassword()));
            } else if (property instanceof Password[]) {
                final Password[] asPasswords = (Password[]) property;
                final Password[] resultPasswords = new Password[asPasswords.length];

                for (int i = 0; i < asPasswords.length; i++) {
                    resultPasswords[i] = new Password(cryptoService.decryptAes(asPasswords[i].getPassword()));
                }

                result = resultPasswords;
            } else {
                result = property;
            }

            return Optional.ofNullable(result);
        } catch (final Exception e) {
            return Optional.empty();
        }
    }
}
