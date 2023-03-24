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
package org.eclipse.kura.network.status.provider.api;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.kura.net.status.NetworkInterfaceStatus;

@SuppressWarnings("unused")
public class InterfaceStatusListDTO {

    private final List<NetworkInterfaceStatusDTO> interfaces;
    private final List<FailureDTO> failures;

    public InterfaceStatusListDTO(final List<NetworkInterfaceStatus> interfaces, final List<FailureDTO> failures) {
        this.interfaces = interfaces.stream().map(NetworkInterfaceStatusDTO::fromNetworkInterfaceStatus)
                .collect(Collectors.toList());
        this.failures = failures;
    }

}
