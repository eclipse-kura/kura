/*******************************************************************************
 * Copyright (c) 2016, 2020 Red Hat Inc and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.camel.internal.utils;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;

import org.apache.camel.spi.Registry;
import org.slf4j.Logger;

public final class KuraServiceFactory {

    // Logger

    private static final Logger logger = getLogger(KuraServiceFactory.class);

    // Constructors

    private KuraServiceFactory() {
    }

    // Operations

    public static <T> T retrieveService(final Class<T> clazz, final Registry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("Registry cannot be null.");
        }

        Set<T> servicesFromRegistry = registry.findByType(clazz);
        if (servicesFromRegistry.size() == 1) {
            T service = servicesFromRegistry.iterator().next();
            logger.info("Found Kura " + clazz.getCanonicalName()
                    + " in the registry. Kura component will use that instance.");
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