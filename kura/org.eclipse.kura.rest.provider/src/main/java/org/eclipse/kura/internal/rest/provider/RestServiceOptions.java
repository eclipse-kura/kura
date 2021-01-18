/*******************************************************************************
 * Copyright (c) 2017, 2021 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.internal.rest.provider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.util.configuration.Property;

public class RestServiceOptions {

    private static final Property<Integer[]> ALLOWED_PORTS = new Property<>("allowed.ports", new Integer[] {});

    private final Set<Integer> allowedPorts;

    public RestServiceOptions(final Map<String, Object> properties) {
        this.allowedPorts = loadIntArrayProperty(ALLOWED_PORTS.get(properties));
    }

    public Set<Integer> getAllowedPorts() {
        return allowedPorts;
    }

    private static Set<Integer> loadIntArrayProperty(final Integer[] list) {
        if (list == null) {
            return Collections.emptySet();
        }

        final Set<Integer> result = new HashSet<>();

        for (int i = 0; i < list.length; i++) {
            final Integer value = list[i];

            if (value != null) {
                result.add(value);
            }
        }

        return result;
    }
}