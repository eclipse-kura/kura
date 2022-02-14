/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.rest.wire.api;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.kura.rest.configuration.api.AdDTO;
import org.eclipse.kura.rest.configuration.api.ComponentConfigurationDTO;

public class WireGraphMetadata {

    private final List<WireComponentDefinitionDTO> wireComponentDefinitions;
    private final List<ComponentConfigurationDTO> driverOCDs;
    private final List<DriverDescriptorDTO> driverChannelDescriptors;
    private final List<AdDTO> assetChannelDescriptor;

    public WireGraphMetadata(List<WireComponentDefinitionDTO> wireComponentDefinitions,
            List<ComponentConfigurationDTO> driverDefinitions, List<DriverDescriptorDTO> driverChannelDescriptors,
            List<AdDTO> assetChannelDescriptor) {
        this.wireComponentDefinitions = nonEmptyOrNull(wireComponentDefinitions);
        this.driverOCDs = nonEmptyOrNull(driverDefinitions);
        this.driverChannelDescriptors = nonEmptyOrNull(driverChannelDescriptors);
        this.assetChannelDescriptor = nonEmptyOrNull(assetChannelDescriptor);
    }

    public List<WireComponentDefinitionDTO> getWireComponentDefinitions() {
        return Optional.ofNullable(this.wireComponentDefinitions).orElseGet(Collections::emptyList);
    }

    public List<ComponentConfigurationDTO> factoryDriverOCDs() {
        return Optional.ofNullable(this.driverOCDs).orElseGet(Collections::emptyList);
    }

    public List<DriverDescriptorDTO> getDriverDescriptors() {
        return Optional.ofNullable(this.driverChannelDescriptors).orElseGet(Collections::emptyList);
    }

    public List<AdDTO> getBaseChannelDescriptor() {
        return Optional.ofNullable(this.assetChannelDescriptor).orElseGet(Collections::emptyList);
    }

    private static <T> List<T> nonEmptyOrNull(List<T> original) {
        if (original == null || original.isEmpty()) {
            original = null;
        }

        return original;
    }
}
