/*******************************************************************************
 * Copyright (c) 2021 WinWinIt and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  WinWinIt
 *******************************************************************************/
package org.eclipse.kura.system;

import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

/**
 * This service provides all extended properties which will be added to the MQTT birth certificate.
 */
@ProviderType
public interface ExtendedPropertiesService {

    /**
     * Returns the available MQTT birth certificate extended properties.
     * 
     * @return An {@link Optional} containing the extended properties, if available. Never {@literal null}.
     */
    public Optional<ExtendedProperties> getExtendedProperties();

}
