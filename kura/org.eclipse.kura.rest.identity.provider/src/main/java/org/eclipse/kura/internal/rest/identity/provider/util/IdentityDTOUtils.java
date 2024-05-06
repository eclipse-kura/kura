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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.core.configuration.ComponentConfigurationImpl;
import org.eclipse.kura.identity.AdditionalConfigurations;
import org.eclipse.kura.identity.AssignedPermissions;
import org.eclipse.kura.identity.IdentityConfiguration;
import org.eclipse.kura.identity.IdentityConfigurationComponent;
import org.eclipse.kura.identity.PasswordConfiguration;
import org.eclipse.kura.identity.PasswordStrengthRequirements;
import org.eclipse.kura.identity.Permission;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.AdditionalConfigurationsDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.IdentityConfigurationDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.IdentityDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.PasswordConfigurationDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.PasswordStrenghtRequirementsDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.PermissionConfigurationDTO;
import org.eclipse.kura.internal.rest.identity.provider.v2.dto.PermissionDTO;
import org.eclipse.kura.rest.configuration.api.ComponentConfigurationDTO;
import org.eclipse.kura.rest.configuration.api.DTOUtil;

public class IdentityDTOUtils {

    private IdentityDTOUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static Set<Class<? extends IdentityConfigurationComponent>> toIdentityConfigurationComponents(
            Set<String> componentNames) {

        return componentNames.stream().map(name -> {

            switch (name) {
            case "AdditionalConfigurations":
                return AdditionalConfigurations.class;
            case "AssignedPermissions":
                return AssignedPermissions.class;
            case "PasswordConfiguration":
                return PasswordConfiguration.class;
            default:
                throw new IllegalArgumentException("Unknown component name: " + name);
            }

        }).collect(Collectors.toSet());

    }

    public static IdentityConfigurationDTO fromIdentityConfiguration(IdentityConfiguration identityConfiguration) {

        IdentityConfigurationDTO identityConfigurationDTO = new IdentityConfigurationDTO(
                new IdentityDTO(identityConfiguration.getName()));

        identityConfiguration.getComponent(AdditionalConfigurations.class)
                .ifPresent(additionalConfigurations -> identityConfigurationDTO
                        .setAdditionalConfigurations(fromAdditionalConfigurations(additionalConfigurations)));

        identityConfiguration.getComponent(AssignedPermissions.class)
                .ifPresent(assignedPermissions -> identityConfigurationDTO
                        .setPermissionConfiguration(fromPermissionConfiguration(assignedPermissions)));

        identityConfiguration.getComponent(PasswordConfiguration.class)
                .ifPresent(passwordConfiguration -> identityConfigurationDTO
                        .setPasswordConfiguration(fromPasswordConfiguration(passwordConfiguration)));

        return identityConfigurationDTO;
    }

    public static ComponentConfigurationDTO fromComponentConfiguration(ComponentConfiguration componentConfiguration) {
        return new ComponentConfigurationDTO(componentConfiguration.getPid(), null, DTOUtil
                .configurationPropertiesToDtos(componentConfiguration.getConfigurationProperties(), null, false));
    }

    public static Permission toPermission(PermissionDTO permissionDTO) {
        return new Permission(permissionDTO.getName());
    }

    public static PermissionDTO fromPermission(Permission permission) {
        return new PermissionDTO(permission.getName());
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

    public static PermissionConfigurationDTO fromPermissionConfiguration(AssignedPermissions assignedPermissions) {
        PermissionConfigurationDTO permissionsConfigurationDTO = new PermissionConfigurationDTO();
        permissionsConfigurationDTO.setPermissions(assignedPermissions.getPermissions().stream()
                .map(IdentityDTOUtils::fromPermission).collect(Collectors.toSet()));

        return permissionsConfigurationDTO;
    }

    public static PasswordConfigurationDTO fromPasswordConfiguration(PasswordConfiguration passwordConfiguration) {
        PasswordConfigurationDTO passwordConfigurationDTO = new PasswordConfigurationDTO();

        passwordConfigurationDTO.setPasswordChangeNeeded(passwordConfiguration.isPasswordChangeNeeded());
        passwordConfigurationDTO.setPasswordAuthEnabled(passwordConfiguration.isPasswordAuthEnabled());

        return passwordConfigurationDTO;
    }

    public static PasswordConfiguration toPasswordConfiguration(PasswordConfigurationDTO passwordConfigurationDTO) {

        Optional<char[]> password = Optional.empty();

        if (passwordConfigurationDTO.getPassword() != null) {

            password = Optional.of(passwordConfigurationDTO.getPassword().toCharArray());
        }

        return new PasswordConfiguration(passwordConfigurationDTO.isPasswordChangeNeeded(),
                passwordConfigurationDTO.isPasswordAuthEnabled(), password, Optional.empty());
    }

    public static IdentityConfigurationComponent toAdditionalConfigurations(
            AdditionalConfigurationsDTO additionalConfigurationsDTO) {

        List<ComponentConfiguration> configurations = additionalConfigurationsDTO.getConfigurations()//
                .stream()//
                .map(IdentityDTOUtils::toComponentConfiguration)//
                .collect(Collectors.toList());

        return new AdditionalConfigurations(configurations);
    }

    public static AdditionalConfigurationsDTO fromAdditionalConfigurations(
            AdditionalConfigurations additionalConfigurations) {

        AdditionalConfigurationsDTO additionalConfigurationsDTO = new AdditionalConfigurationsDTO();

        Set<ComponentConfigurationDTO> configurations = additionalConfigurations.getConfigurations()//
                .stream()//
                .map(IdentityDTOUtils::fromComponentConfiguration)//
                .collect(Collectors.toSet());

        additionalConfigurationsDTO.setConfigurations(configurations);

        return additionalConfigurationsDTO;
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

    public static PasswordStrenghtRequirementsDTO fromPasswordStrengthRequirements(
            PasswordStrengthRequirements passwordStrengthRequirements) {

        return new PasswordStrenghtRequirementsDTO(passwordStrengthRequirements.getPasswordMinimumLength(),
                passwordStrengthRequirements.digitsRequired(), //
                passwordStrengthRequirements.specialCharactersRequired(),
                passwordStrengthRequirements.bothCasesRequired());
    }

}
