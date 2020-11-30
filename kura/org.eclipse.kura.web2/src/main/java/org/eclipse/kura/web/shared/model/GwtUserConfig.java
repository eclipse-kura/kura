/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.web.shared.model;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GwtUserConfig extends GwtUserData implements IsSerializable {

    /**
     *
     */
    private static final long serialVersionUID = 8795619406606205153L;

    public GwtUserConfig() {
    }

    public GwtUserConfig(final String userName, final Set<String> permissions, final boolean isPasswordAuthEnabled) {
        super(userName, permissions);
        setPasswordAuthEnabled(isPasswordAuthEnabled);
    }

    public boolean isPasswordAuthEnabled() {
        return this.get("password.enabled");
    }

    public void setPasswordAuthEnabled(final boolean enabled) {
        this.set("password.enabled", enabled);
    }

    public void setNewPassword(final Optional<String> password) {
        if (password.isPresent()) {
            this.set("password", password.get());
        } else {
            this.set("password", null);
        }
    }

    public Optional<String> getNewPassword() {
        return Optional.ofNullable(get("password"));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(isPasswordAuthEnabled(), getNewPassword());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        GwtUserConfig other = (GwtUserConfig) obj;
        return isPasswordAuthEnabled() == other.isPasswordAuthEnabled()
                && Objects.equals(getNewPassword(), other.getNewPassword());
    }

}
