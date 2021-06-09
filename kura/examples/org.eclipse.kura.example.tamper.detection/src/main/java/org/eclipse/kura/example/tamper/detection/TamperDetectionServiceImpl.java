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
package org.eclipse.kura.example.tamper.detection;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.security.tamper.detection.TamperDetectionProperties;
import org.eclipse.kura.security.tamper.detection.TamperDetectionService;
import org.eclipse.kura.security.tamper.detection.TamperEvent;
import org.eclipse.kura.security.tamper.detection.TamperStatus;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.util.configuration.Property;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TamperDetectionServiceImpl implements TamperDetectionService, ConfigurableComponent {

    private static final String TAMPERED_KEY = "tampered";

    private static final Property<Boolean> TAMPERED = new Property<>(TAMPERED_KEY, false);

    private static final Logger logger = LoggerFactory.getLogger(TamperDetectionServiceImpl.class);

    private EventAdmin eventAdmin;
    private ConfigurationService configurationService;

    private boolean isDeviceTampered = false;
    private Optional<Date> tamperInstant = Optional.empty();
    private String ownPid;

    public void setConfigurationService(final ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void setEventAdmin(final EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    public void activate(final Map<String, Object> properties) {
        logger.info("activating...");
        ownPid = extractPid(properties);
        setDeviceTampered(TAMPERED.get(properties));
        logger.info("activating...done");
    }

    public void update(final Map<String, Object> properties) {
        logger.info("updating...");
        ownPid = extractPid(properties);
        setDeviceTampered(TAMPERED.get(properties));
        logger.info("updating...done");
    }

    @Override
    public String getDisplayName() {
        return "Simulated tamper detection " + ownPid;
    }

    @Override
    public TamperStatus getTamperStatus() {
        final Map<String, TypedValue<?>> properties = new HashMap<>();

        if (tamperInstant.isPresent()) {
            properties.put(TamperDetectionProperties.TIMESTAMP_PROPERTY_KEY.getValue(),
                    TypedValues.newLongValue(tamperInstant.get().getTime()));
        }

        return new TamperStatus(isDeviceTampered, properties);
    }

    @Override
    public void resetTamperStatus() throws KuraException {
        configurationService.updateConfiguration(ownPid, Collections.singletonMap(TAMPERED_KEY, false));
    }

    private String extractPid(final Map<String, Object> properties) {
        try {
            return (String) properties.get(ConfigurationService.KURA_SERVICE_PID);
        } catch (final Exception e) {
            return TamperDetectionServiceImpl.class.getName();
        }
    }

    private void postTamperEvent() {
        final TamperEvent tamperEvent = new TamperEvent(ownPid, getTamperStatus());

        this.eventAdmin.postEvent(tamperEvent);
    }

    private void setDeviceTampered(final boolean isDeviceTampered) {

        final boolean stateChanged = this.isDeviceTampered ^ isDeviceTampered;

        this.isDeviceTampered = isDeviceTampered;

        if (isDeviceTampered) {
            tamperInstant = Optional.of(new Date());
        } else {
            tamperInstant = Optional.empty();
        }

        if (isDeviceTampered || stateChanged) {
            postTamperEvent();
        }
    }
}
