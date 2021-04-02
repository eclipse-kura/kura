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

import com.google.gwt.user.client.rpc.IsSerializable;

public class GwtSecurityCapabilities implements IsSerializable {

    private boolean isDebugMode;
    private boolean isSecurityServiceAvailable;
    private boolean isThreatManagerAvailable;
    private boolean isTamperDetectionAvailable;

    public GwtSecurityCapabilities() {
    }

    public GwtSecurityCapabilities(final boolean isDebugMode, final boolean isSecurityServiceAvailable,
            final boolean isThreatManagerAvailable, final boolean isTamperDetectionAvailable) {
        this.isDebugMode = isDebugMode;
        this.isSecurityServiceAvailable = isSecurityServiceAvailable;
        this.isThreatManagerAvailable = isThreatManagerAvailable;
        this.isTamperDetectionAvailable = isTamperDetectionAvailable;
    }

    public boolean isDebugMode() {
        return isDebugMode;
    }

    public boolean isSecurityServiceAvailable() {
        return isSecurityServiceAvailable;
    }

    public boolean isThreatManagerAvailable() {
        return isThreatManagerAvailable;
    }

    public boolean isTamperDetectionAvailable() {
        return isTamperDetectionAvailable;
    }

}
