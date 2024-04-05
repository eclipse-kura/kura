/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.identity.provider.v2.dto;

public class IdentityConfigurationDTO {

    private final IdentityDTO identity;
    private PermissionConfigurationDTO permissionConfiguration;
    private PasswordConfigurationDTO passwordConfiguration;
    private AdditionalConfigurationsDTO additionalConfigurations;

    public IdentityConfigurationDTO(IdentityDTO identity) {
        this.identity = identity;
    }

    public IdentityDTO getIdentity() {
        return this.identity;
    }

    public PermissionConfigurationDTO getPermissionConfiguration() {
        return this.permissionConfiguration;
    }

    public void setPermissionConfiguration(PermissionConfigurationDTO permissionConfiguration) {
        this.permissionConfiguration = permissionConfiguration;
    }

    public PasswordConfigurationDTO getPasswordConfiguration() {
        return this.passwordConfiguration;
    }

    public void setPasswordConfiguration(PasswordConfigurationDTO passwordConfiguration) {
        this.passwordConfiguration = passwordConfiguration;
    }

    public AdditionalConfigurationsDTO getAdditionalConfigurations() {
        return this.additionalConfigurations;
    }

    public void setAdditionalConfigurations(AdditionalConfigurationsDTO additionalConfiguration) {
        this.additionalConfigurations = additionalConfiguration;
    }

    @Override
    public String toString() {
        return "IdentityConfigurationDTO [identity=" + this.identity + ", permissionConfiguration="
                + this.permissionConfiguration + ", passwordConfiguration=" + this.passwordConfiguration
                + ", additionalConfigurations=" + this.additionalConfigurations + "]";
    }

}
