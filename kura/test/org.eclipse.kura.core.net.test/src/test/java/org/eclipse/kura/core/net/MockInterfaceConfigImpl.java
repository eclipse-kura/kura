/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
