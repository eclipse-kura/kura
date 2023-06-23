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
 ******************************************************************************/
package org.eclipse.kura.net.status.loopback;

import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.status.NetworkInterfaceType;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Class that contains specific properties to describe the status of a
 * Loopback interface.
 *
 */
@ProviderType
public class LoopbackInterfaceStatus extends NetworkInterfaceStatus {

    private LoopbackInterfaceStatus(LoopbackInterfaceStatusBuilder builder) {
        super(builder);
    }

    public static LoopbackInterfaceStatusBuilder builder() {
        return new LoopbackInterfaceStatusBuilder();
    }

    public static class LoopbackInterfaceStatusBuilder
            extends NetworkInterfaceStatusBuilder<LoopbackInterfaceStatusBuilder> {

        @Override
        public LoopbackInterfaceStatus build() {
            withType(NetworkInterfaceType.LOOPBACK);
            return new LoopbackInterfaceStatus(this);
        }

        @Override
        public LoopbackInterfaceStatusBuilder getThis() {
            return this;
        }
    }

}
