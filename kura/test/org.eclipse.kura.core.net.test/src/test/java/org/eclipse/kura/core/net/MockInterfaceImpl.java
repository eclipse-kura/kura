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
import org.eclipse.kura.net.NetInterfaceAddress;
import org.eclipse.kura.net.NetInterfaceType;

public class MockInterfaceImpl<T extends NetInterfaceAddress> extends AbstractNetInterface<T>
        implements MockInterface<T> {

    private NetInterfaceType type;

    protected MockInterfaceImpl(String name) {
        super(name);
    }

    @Override
    public NetInterfaceType getType() {
        return this.type;
    }

    public void setType(NetInterfaceType type) {
        this.type = type;
    }

    public List<NetConfig> getConfigs() {
        return null;
    }
}
