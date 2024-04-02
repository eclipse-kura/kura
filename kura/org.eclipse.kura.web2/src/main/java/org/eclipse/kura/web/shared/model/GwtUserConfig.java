/*******************************************************************************
 * Copyright (c) 2020, 2024 Eurotech and/or its affiliates and others
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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class GwtUserConfig extends GwtUserData {

    private static final String PASSWORD_ENABLED_KEY = "password.enabled";
    private static final String PASSWORD_KEY = "password";
    private static final String PASSWORD_CHANGE_NEEDED_KEY = "password.change.needed";
    private static final long serialVersionUID = 8795619406606205153L;

    private Map<String, GwtConfigComponent> additionalConfigurations;

    public GwtUserConfig() {
    }

    public GwtUserConfig(final String userName, final Set<String> permissions,
            final Map<String, GwtConfigComponent> additionalConfigurations, final boolean isPasswordAuthEnabled,
            final boolean isPasswordChangeNeeded) {
        super(userName, permissions);
        setPasswordAuthEnabled(isPasswordAuthEnabled);
        setPasswordChangeNeeded(isPasswordChangeNeeded);
        setAdditionalConfigurations(additionalConfigurations);
    }

    public boolean isPasswordAuthEnabled() {
        return this.get(PASSWORD_ENABLED_KEY);
    }

    public void setPasswordAuthEnabled(final boolean enabled) {
        set(PASSWORD_ENABLED_KEY, enabled);
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

    public boolean isPasswordChangeNeeded() {
        return Optional.ofNullable((Boolean) get(PASSWORD_CHANGE_NEEDED_KEY)).orElse(false);
    }

    public void setPasswordChangeNeeded(final boolean isPasswordChangeNeeded) {
        set(PASSWORD_CHANGE_NEEDED_KEY, isPasswordChangeNeeded);
    }

    public Map<String, GwtConfigComponent> getAdditionalConfigurations() {
        return additionalConfigurations;
    }

    public void setAdditionalConfigurations(final Map<String, GwtConfigComponent> additionalConfigurations) {
        this.additionalConfigurations = additionalConfigurations;
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
