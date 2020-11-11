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

import com.google.gwt.user.client.rpc.IsSerializable;

public class GwtLoginInfo implements IsSerializable, Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 4501114031863790538L;

    private String bannerContent;
    private String[] enabledAuthMethods;

    public GwtLoginInfo() {
    }

    public GwtLoginInfo(final String bannerContent, final String[] enabledAuthMethods) {
        this.bannerContent = bannerContent;
        this.enabledAuthMethods = enabledAuthMethods;
    }

    public String getBannerContent() {
        return bannerContent;
    }

    public String[] getEnabledAuthMethods() {
        return enabledAuthMethods;
    }
}
