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
package org.eclipse.kura.net.status.ethernet;

import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.status.NetworkInterfaceStatus.NetworkInterfaceStatusBuilder;

public class EthernetInterfaceStatus extends NetworkInterfaceStatus {

    private final boolean linkUp;

    private EthernetInterfaceStatus(EthernetInterfaceStatusBuilder builder) {
        super(builder);
        this.linkUp = builder.linkUp;
    }

    public boolean isLinkUp() {
        return this.linkUp;
    }

    public static EthernetInterfaceStatusBuilder builder() {
        return new EthernetInterfaceStatusBuilder();
    }

    public static class EthernetInterfaceStatusBuilder
            extends NetworkInterfaceStatusBuilder<EthernetInterfaceStatusBuilder> {

        private boolean linkUp = false;

        public EthernetInterfaceStatusBuilder withIsLinkUp(boolean linkUp) {
            this.linkUp = linkUp;
            return getThis();
        }

        @Override
        public EthernetInterfaceStatus build() {
            return new EthernetInterfaceStatus(this);
        }

        @Override
        public EthernetInterfaceStatusBuilder getThis() {
            return this;
        }
    }

}
