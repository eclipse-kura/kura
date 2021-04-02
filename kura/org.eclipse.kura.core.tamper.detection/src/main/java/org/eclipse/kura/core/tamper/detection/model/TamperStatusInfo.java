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
 ******************************************************************************/
package org.eclipse.kura.core.tamper.detection.model;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.kura.security.tamper.detection.TamperStatus;

public class TamperStatusInfo {

    @SuppressWarnings("unused")
    private final boolean isDeviceTampered;
    @SuppressWarnings("unused")
    private final Map<String, Object> properties;

    public TamperStatusInfo(final TamperStatus tamperStatus) {
        this.isDeviceTampered = tamperStatus.isDeviceTampered();
        this.properties = tamperStatus.getProperties().entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().getValue()));
    }
}
