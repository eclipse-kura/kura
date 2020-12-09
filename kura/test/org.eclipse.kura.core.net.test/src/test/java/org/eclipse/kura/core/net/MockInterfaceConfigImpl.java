/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.net;

import java.util.List;

import org.eclipse.kura.net.NetConfig;
import org.eclipse.kura.net.NetInterfaceAddressConfig;
import org.eclipse.kura.net.NetInterfaceConfig;

public class MockInterfaceConfigImpl extends MockInterfaceImpl<NetInterfaceAddressConfig>
        implements NetInterfaceConfig<NetInterfaceAddressConfig> {

    private List<NetConfig> configs;

    public MockInterfaceConfigImpl(String name) {
        super(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof MockInterfaceConfigImpl)) {
            return false;
        }
        MockInterfaceConfigImpl other = (MockInterfaceConfigImpl) obj;
        if (this.configs == null) {
            if (other.configs != null) {
                return false;
            }
        } else if (!this.configs.equals(other.configs)) {
            return false;
        }
        return true;
    }

    @Override
    public List<NetConfig> getConfigs() {
        return this.configs;
    }

    public void setNetConfigs(List<NetConfig> configs) {
        this.configs = configs;
    }
}
