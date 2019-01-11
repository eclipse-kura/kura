/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
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
        return pid;
    }

    public String getFactoryPid() {
        return factoryPid;
    }

    public GwtConfigComponent getChannelDescriptor() {
        return channelDescriptor;
    }

}