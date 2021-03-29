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
