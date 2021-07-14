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
package org.eclipse.kura.system;

/**
 * An enum representing the supported System Resource Types
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.2
 */
public enum SystemResourceType {

    BUNDLE,
    DP,
    RPM,
    DEB,
    DOCKER,
    APK,
    UNKNOWN;
}
