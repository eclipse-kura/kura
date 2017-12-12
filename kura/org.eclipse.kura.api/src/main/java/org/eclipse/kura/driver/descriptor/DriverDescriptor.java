/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.driver.descriptor;

import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.osgi.annotation.versioning.ProviderType;

/**
 * The Class DriverDescriptor is responsible for storing the driver instance
 * configuration. This class can then be passed for serialization and is used to
 * map all the information related to a driver instance.<br>
 * <br>
 *
 * @see Driver
 * @see ChannelDescriptor
 *
 * @noextend This class is not intended to be extended by clients.
 * @since 1.4
 */
@ProviderType
public class DriverDescriptor {

    private final String pid;
    private final String factoryPid;
    private final Object channelDescriptor;

    public DriverDescriptor(String pid, String factoryPid, Object channelDescriptor) {
        this.pid = pid;
        this.factoryPid = factoryPid;
        this.channelDescriptor = channelDescriptor;
    }

    public String getPid() {
        return this.pid;
    }

    public String getFactoryPid() {
        return this.factoryPid;
    }

    public Object getChannelDescriptor() {
        return this.channelDescriptor;
    }
}
