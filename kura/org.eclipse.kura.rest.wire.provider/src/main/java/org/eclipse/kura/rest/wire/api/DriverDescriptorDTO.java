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
 ******************************************************************************/
package org.eclipse.kura.rest.wire.api;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.driver.descriptor.DriverDescriptor;
import org.eclipse.kura.rest.configuration.api.AdDTO;

public class DriverDescriptorDTO {

    private final String pid;
    private final String factoryPid;
    private final List<AdDTO> channelDescriptor;

    public DriverDescriptorDTO(final DriverDescriptor driverDescriptor) {
        this.pid = driverDescriptor.getPid();
        this.factoryPid = driverDescriptor.getFactoryPid();

        final Object rawDescriptor = driverDescriptor.getChannelDescriptor();

        if (rawDescriptor instanceof List) {
            @SuppressWarnings("unchecked")
            final List<AD> channelDescriptorADs = (List<AD>) rawDescriptor;

            this.channelDescriptor = channelDescriptorADs.stream().map(AdDTO::new).collect(Collectors.toList());
        } else {
            this.channelDescriptor = null;
        }
    }

    public String getPid() {
        return pid;
    }

    public String getFactoryPid() {
        return factoryPid;
    }

    public List<AdDTO> getChannelDescriptor() {
        return Optional.ofNullable(this.channelDescriptor).orElseGet(Collections::emptyList);
    }

}
