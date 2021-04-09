/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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

import java.util.Optional;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GwtComponentInstanceInfo implements IsSerializable {

    private String pid;
    private String factoryPid;

    public GwtComponentInstanceInfo() {
    }

    public GwtComponentInstanceInfo(final String pid, final Optional<String> factoryPid) {
        this.pid = pid;
        this.factoryPid = factoryPid.orElse(null);
    }

    public String getPid() {
        return pid;
    }

    public Optional<String> getFactoryPid() {
        return Optional.ofNullable(factoryPid);
    }

}
