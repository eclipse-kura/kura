/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.Set;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GwtLoginInfo implements IsSerializable, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 4501114031863790538L;

    private String bannerContent;
    private Set<String> enabledAuthMethods;
    private Integer certAuthPort;

    public GwtLoginInfo() {
    }

    public GwtLoginInfo(final String bannerContent, final Set<String> enabledAuthMethods,
            final Integer clientAuthPort) {
        this.bannerContent = bannerContent;
        this.enabledAuthMethods = enabledAuthMethods;
        this.certAuthPort = clientAuthPort;
    }

    public String getBannerContent() {
        return bannerContent;
    }

    public Set<String> getEnabledAuthMethods() {
        return enabledAuthMethods;
    }

    public Integer getCertAuthPort() {
        return certAuthPort;
    }
}
