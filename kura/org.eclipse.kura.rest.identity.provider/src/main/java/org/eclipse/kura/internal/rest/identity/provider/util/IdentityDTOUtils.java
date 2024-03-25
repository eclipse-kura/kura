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
package org.eclipse.kura.internal.rest.identity.provider.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.identity.AdditionalConfigurations;
import org.eclipse.kura.identity.AssignedPermissions;
import org.eclipse.kura.identity.IdentityConfiguration;
import org.eclipse.kura.identity.IdentityConfigurationComponent;
import org.eclipse.kura.identity.PasswordConfiguration;
import org.eclipse.kura.identity.PasswordHash;
import org.eclipse.kura.identity.Permission;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.AdditionalConfigurationsDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.IdentityConfigurationDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.PasswordConfigurationDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.PermissionConfigurationDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.PermissionDTO;
import org.eclipse.kura.rest.configuration.api.ComponentConfigurationDTO;
import org.eclipse.kura.rest.configuration.api.DTOUtil;

public class IdentityDTOUtils {

    private IdentityDTOUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static Permission toPermission(PermissionDTO permissionDTO) {
        return new Permission(permissionDTO.getName());
    }

    public static ComponentConfiguration toComponentConfiguration(ComponentConfigurationDTO componentConfigurationDTO) {

        ComponentConfigurationImpl componentConfiguration = new ComponentConfigurationImpl();
        componentConfiguration.setPid(componentConfigurationDTO.getPid());
        componentConfiguration
                .setProperties(DTOUtil.dtosToConfigurationProperties(componentConfigurationDTO.getProperties()));

        return componentConfiguration;
    }

    public static IdentityConfigurationComponent toPermissionConfiguration(
            PermissionConfigurationDTO permissionConfigurationDTO) {
        return new AssignedPermissions(permissionConfigurationDTO.getPermissions().stream() //
                .map(IdentityDTOUtils::toPermission) //
                .collect(Collectors.toSet()) //
        );
    }

    public static PasswordConfiguration toPasswordConfiguration(PasswordConfigurationDTO passwordConfigurationDTO) {

        return new PasswordConfiguration(passwordConfigurationDTO.isPasswordChangeNeeded(),
                passwordConfigurationDTO.isPasswordAuthEnabled(),
                Optional.ofNullable(passwordConfigurationDTO.getPasswordHash() != null
                        ? new PasswordHashImpl(passwordConfigurationDTO.getPasswordHash())
                        : null));
    }

    public static IdentityConfigurationComponent toAdditionalConfigurations(
            AdditionalConfigurationsDTO additionalConfigurationsDTO) {

        List<ComponentConfiguration> configurations = additionalConfigurationsDTO.getConfigurations()//
                .stream()//
                .map(IdentityDTOUtils::toComponentConfiguration)//
                .collect(Collectors.toList());

        return new AdditionalConfigurations(configurations);
    }

    public static IdentityConfiguration toIdentityConfiguration(IdentityConfigurationDTO identityConfigurationDTO) {
        List<IdentityConfigurationComponent> components = new ArrayList<>();

        if (identityConfigurationDTO.getPermissionConfiguration() != null) {
            components.add(toPermissionConfiguration(identityConfigurationDTO.getPermissionConfiguration()));
        }

        if (identityConfigurationDTO.getPasswordConfiguration() != null) {
            components.add(toPasswordConfiguration(identityConfigurationDTO.getPasswordConfiguration()));
        }

        if (identityConfigurationDTO.getAdditionalConfigurations() != null) {
            components.add(toAdditionalConfigurations(identityConfigurationDTO.getAdditionalConfigurations()));
        }

        return new IdentityConfiguration(identityConfigurationDTO.getIdentity().getName(), components);

    }

    private static class PasswordHashImpl implements PasswordHash {

        private final String passwordHash;

        public PasswordHashImpl(String passwordHash) {
            this.passwordHash = passwordHash;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.passwordHash);
        }

        @Override
        public boolean equals(Object obj) {

            if (this == obj) {
                return true;
            }
            if ((obj == null) || (getClass() != obj.getClass())) {
                return false;
            }

            PasswordHashImpl other = (PasswordHashImpl) obj;

            return Objects.equals(this.passwordHash, other.passwordHash);
        }

    }

}
