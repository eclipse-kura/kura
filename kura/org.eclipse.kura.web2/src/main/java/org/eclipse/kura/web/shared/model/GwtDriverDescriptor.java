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

package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

public class GwtDriverDescriptor extends GwtBaseModel implements Serializable {

    private static final long serialVersionUID = -5716936754311577904L;

    private final String pid;
    private final String factoryPid;
    private final GwtConfigComponent channelDescriptor;

    public GwtDriverDescriptor(String pid, String factoryPid, GwtConfigComponent channelDescriptor) {
        super();
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

    public GwtConfigComponent getChannelDescriptor() {
        return this.channelDescriptor;
    }

}