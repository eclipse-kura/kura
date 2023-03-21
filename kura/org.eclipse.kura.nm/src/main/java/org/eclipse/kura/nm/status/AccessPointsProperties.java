/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.nm.status;

import java.util.List;
import java.util.Optional;

import org.freedesktop.dbus.interfaces.Properties;

public class AccessPointsProperties {

    private final Optional<Properties> activeAccessPoint;
    private final List<Properties> availableAccessPoints;

    public AccessPointsProperties(Optional<Properties> activeAP, List<Properties> availableAPs) {
        this.activeAccessPoint = activeAP;
        this.availableAccessPoints = availableAPs;
    }

    public Optional<Properties> getActiveAccessPoint() {
        return this.activeAccessPoint;
    }

    public List<Properties> getAvailableAccessPoints() {
        return this.availableAccessPoints;
    }

}
