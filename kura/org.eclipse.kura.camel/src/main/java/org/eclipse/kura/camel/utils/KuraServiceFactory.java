/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.camel.utils;

import org.apache.camel.spi.Registry;
import org.slf4j.Logger;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

public final class KuraServiceFactory<T> {

    // Logger

    private static final Logger s_logger = getLogger(KuraServiceFactory.class);

    // Constructors

    private KuraServiceFactory() {
    }

    // Operations

    public static <T> T retrieveService(Class<T> clazz, Registry registry) {
        if(registry == null) {
            throw new IllegalArgumentException("Registry cannot be null.");
        }

        Set<T> servicesFromRegistry = registry.findByType(clazz);
        if (servicesFromRegistry.size() == 1) {
            T service = servicesFromRegistry.iterator().next();
            s_logger.info("Found Kura " + clazz.getCanonicalName() + " in the registry. Kura component will use that instance.");
            return service;
        } else if (servicesFromRegistry.size() > 1) {
            throw new IllegalStateException("Too many " + clazz.getCanonicalName() + " services found in a registry: "
                    + servicesFromRegistry.size());
        } else {
            throw new IllegalArgumentException(
                    "No " + clazz.getCanonicalName() + " service instance found in a registry.");
        }
    }

}