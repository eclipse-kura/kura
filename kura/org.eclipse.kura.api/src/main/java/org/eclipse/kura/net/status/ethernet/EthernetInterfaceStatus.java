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

import java.util.Objects;

import org.eclipse.kura.net.status.NetworkInterfaceStatus;
import org.eclipse.kura.net.status.NetworkInterfaceType;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Class that contains specific properties to describe the status of an
 * Ethernet interface.
 *
 */
@ProviderType
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
            withType(NetworkInterfaceType.ETHERNET);
            return new EthernetInterfaceStatus(this);
        }

        @Override
        public EthernetInterfaceStatusBuilder getThis() {
            return this;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(this.linkUp);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj) || getClass() != obj.getClass()) {
            return false;
        }
        EthernetInterfaceStatus other = (EthernetInterfaceStatus) obj;
        return this.linkUp == other.linkUp;
    }

}
