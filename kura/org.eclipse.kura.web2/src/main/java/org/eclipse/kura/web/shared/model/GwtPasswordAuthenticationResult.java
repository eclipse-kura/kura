package org.eclipse.kura.web.shared.model;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GwtPasswordAuthenticationResult implements IsSerializable {

    private boolean isPasswordUpdateRequired;
    private String redirectPath;

    public GwtPasswordAuthenticationResult() {
    }

    public GwtPasswordAuthenticationResult(final boolean isPasswordUpdateRequired, final String redirectPath) {
        this.isPasswordUpdateRequired = isPasswordUpdateRequired;
        this.redirectPath = redirectPath;
    }

    public boolean isPasswordUpdateRequired() {
        return isPasswordUpdateRequired;
    }

    public String redirectPath() {
        return redirectPath;
    }
}
