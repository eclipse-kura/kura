/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
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

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.google.gwt.user.client.rpc.IsSerializable;

public class GwtUserConfig extends GwtUserData implements IsSerializable {

    private static final String PASSWORD_KEY = "password";
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
        set("password.enabled", enabled);
    }

    public void setNewPassword(final Optional<String> password) {
        if (password.isPresent()) {
            set(PASSWORD_KEY, password.get());
        } else {
            set(PASSWORD_KEY, null);
        }
    }

    public Optional<String> getNewPassword() {
        return Optional.ofNullable(get(PASSWORD_KEY));
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
