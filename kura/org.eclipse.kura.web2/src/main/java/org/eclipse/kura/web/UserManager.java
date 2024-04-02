/*******************************************************************************
 * Copyright (c) 2020, 2024 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.identity.AdditionalConfigurations;
import org.eclipse.kura.identity.AssignedPermissions;
import org.eclipse.kura.identity.IdentityConfiguration;
import org.eclipse.kura.identity.IdentityConfigurationComponent;
import org.eclipse.kura.identity.IdentityService;
import org.eclipse.kura.identity.PasswordConfiguration;
import org.eclipse.kura.identity.PasswordHash;
import org.eclipse.kura.identity.Permission;
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.shared.KuraPermission;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtUserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserManager {

    private static final Set<Class<? extends IdentityConfigurationComponent>> ALL_COMPONENTS = new HashSet<>(
            Arrays.asList(PasswordConfiguration.class, AssignedPermissions.class, AdditionalConfigurations.class));

    private static final Logger logger = LoggerFactory.getLogger(UserManager.class);

    private final IdentityService identityService;

    public UserManager(final IdentityService identityService) {
        this.identityService = identityService;
    }

    public void update() {
        initializeUserAdmin();
    }

    public void authenticateWithPassword(final String username, final String password) throws KuraException {
        final PasswordConfiguration passwordConfiguration = this.identityService
                .getIdentityConfiguration(username, Collections.singleton(PasswordConfiguration.class))
                .flatMap(i -> i.getComponent(PasswordConfiguration.class))
                .orElseThrow(() -> new KuraException(KuraErrorCode.SECURITY_EXCEPTION));

        if (!passwordConfiguration.isPasswordAuthEnabled() || !Objects.equals(passwordConfiguration.getPasswordHash(),
                Optional.of(this.identityService.computePasswordHash(password.toCharArray())))) {
            throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION);
        }
    }

    public void requirePermissions(final String username, final String... requiredPermissions) throws KuraException {

        final AssignedPermissions assignedPermissions = this.identityService
                .getIdentityConfiguration(username, Collections.singleton(AssignedPermissions.class))
                .flatMap(i -> i.getComponent(AssignedPermissions.class))
                .orElseThrow(() -> new KuraException(KuraErrorCode.SECURITY_EXCEPTION, "Identity not found"));

        if (assignedPermissions.getPermissions().contains(new Permission("admin"))) {
            return;
        }

        for (final String requiredPermission : requiredPermissions) {
            if (!assignedPermissions.getPermissions().contains(new Permission(requiredPermission))) {
                throw new KuraException(KuraErrorCode.SECURITY_EXCEPTION,
                        "identity does not have the " + requiredPermission + " perimission");
            }
        }
    }

    public boolean isPasswordChangeRequired(final String username) throws KuraException {

        return this.identityService
                .getIdentityConfiguration(username, Collections.singleton(PasswordConfiguration.class))
                .flatMap(p -> p.getComponent(PasswordConfiguration.class))
                .map(p -> p.isPasswordChangeNeeded())
                .orElse(false);

    }

    public void createUser(final String userName) throws KuraException {
        this.identityService.createIdentity(userName);
    }

    public void deleteUser(final String userName) throws KuraException {
        this.identityService.deleteIdentity(userName);
    }

    public void setUserPassword(final String userName, final String userPassword) throws KuraException {

        this.identityService.updateIdentityConfigurations(Collections.singletonList(new IdentityConfiguration(userName,
                Collections.singletonList(
                        new PasswordConfiguration(false, true,
                                Optional.of(this.identityService.computePasswordHash(userPassword.toCharArray())))))));

    }

    public Set<String> getDefinedPermissions() throws KuraException {
        return this.identityService.getPermissions().stream().map(Permission::getName).collect(Collectors.toSet());
    }

    public Set<GwtUserConfig> getUserConfig() throws KuraException {

        return getUserConfig(ALL_COMPONENTS);

    }

    public Set<GwtUserConfig> getUserConfig(
            final Set<Class<? extends IdentityConfigurationComponent>> componentsToReturn)
            throws KuraException {

        return this.identityService
                .getIdentitiesConfiguration(componentsToReturn)
                .stream().map(this::getUserConfig).collect(Collectors.toSet());
    }

    public GwtUserConfig getUserDefaultConfig(final String name) throws KuraException {
        return getUserDefaultConfig(name, ALL_COMPONENTS);
    }

    public GwtUserConfig getUserDefaultConfig(final String name,
            final Set<Class<? extends IdentityConfigurationComponent>> componentsToReturn) throws KuraException {
        return getUserConfig(this.identityService.getIdentityDefaultConfiguration(name, componentsToReturn));
    }

    public Optional<GwtUserConfig> getUserConfig(final String name) throws KuraException {
        return getUserConfig(name, ALL_COMPONENTS);
    }

    public Optional<GwtUserConfig> getUserConfig(final String name,
            final Set<Class<? extends IdentityConfigurationComponent>> componentsToReturn) throws KuraException {
        return this.identityService
                .getIdentityConfiguration(name, componentsToReturn)
                .map(this::getUserConfig);
    }

    public GwtUserConfig getUserConfig(final IdentityConfiguration identity) {

        final Optional<PasswordConfiguration> passwordData = identity.getComponent(PasswordConfiguration.class);

        final Set<String> perimissions = identity.getComponent(AssignedPermissions.class)
                .map(AssignedPermissions::getPermissions).orElseGet(Collections::emptySet)
                .stream().map(Permission::getName).collect(Collectors.toSet());

        final Map<String, GwtConfigComponent> additionalConfigurations = identity
                .getComponent(AdditionalConfigurations.class)
                .map(AdditionalConfigurations::getConfigurations).orElseGet(Collections::emptyList)
                .stream().map(GwtServerUtil::toGwtConfigComponent).filter(Objects::nonNull)
                .collect(Collectors.toMap(c -> c.getComponentId(), c -> c));

        return new GwtUserConfig(identity.getName(), perimissions,
                additionalConfigurations,
                passwordData.map(PasswordConfiguration::isPasswordAuthEnabled).orElse(false),
                passwordData.map(PasswordConfiguration::isPasswordChangeNeeded).orElse(false));

    }

    public Optional<Integer> getCredentialsHash(final String userName) throws KuraException {

        return this.identityService
                .getIdentityConfiguration(userName, Collections.singleton(PasswordConfiguration.class))
                .flatMap(i -> i.getComponent(PasswordConfiguration.class)).map(i -> i.getPasswordHash().hashCode());

    }

    public void setUserConfig(final Set<GwtUserConfig> userConfigs) throws KuraException {

        final List<IdentityConfiguration> configurations = new ArrayList<>();

        for (final GwtUserConfig config : userConfigs) {
            configurations.add(buildIdentityConfiguration(config));
        }

        this.identityService.validateIdentityConfigurations(configurations);

        final Set<String> existingIdentityNames = this.identityService
                .getIdentitiesConfiguration(Collections.emptySet()).stream()
                .map(IdentityConfiguration::getName).collect(Collectors.toSet());

        for (final String existingIdentity : existingIdentityNames) {
            if (userConfigs.stream().noneMatch(data -> data.getUserName().equals(existingIdentity))) {
                try {
                    deleteUser(existingIdentity);
                } catch (Exception e) {
                    throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                            "Failed to delete identity " + existingIdentity);
                }
            }
        }

        for (final GwtUserConfig config : userConfigs) {
            if (!existingIdentityNames.contains(config.getUserName())) {
                try {
                    createUser(config.getUserName());
                } catch (Exception e) {
                    throw new KuraException(KuraErrorCode.CONFIGURATION_ERROR,
                            "Failed to create identity " + config.getUserName());
                }
            }
        }

        this.identityService.updateIdentityConfigurations(configurations);

    }

    private IdentityConfiguration buildIdentityConfiguration(final GwtUserConfig config) throws KuraException {
        final Set<Permission> permissions = config.getPermissions().stream().map(Permission::new)
                .collect(Collectors.toSet());
        final AssignedPermissions assignedPermissions = new AssignedPermissions(permissions);

        final Optional<String> newPassword = config.getNewPassword();
        final Optional<PasswordHash> passwordHash;

        if (newPassword.isPresent()) {
            passwordHash = Optional
                    .of(this.identityService.computePasswordHash(newPassword.get().toCharArray()));
        } else {
            passwordHash = Optional.empty();
        }

        final PasswordConfiguration passwordData = new PasswordConfiguration(config.isPasswordChangeNeeded(),
                config.isPasswordAuthEnabled(), passwordHash);

        final AdditionalConfigurations additionalConfigurations = new AdditionalConfigurations(
                config.getAdditionalConfigurations().values()
                        .stream().map(c -> GwtServerUtil.fromGwtConfigComponent(c, null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));

        return new IdentityConfiguration(config.getUserName(),
                Arrays.asList(passwordData, assignedPermissions, additionalConfigurations));
    }

    private void initializeUserAdmin() {

        for (final String defaultPermission : KuraPermission.DEFAULT_PERMISSIONS) {
            try {
                this.identityService.createPermission(new Permission(defaultPermission));
            } catch (final KuraException e) {
                logger.warn("Failed to create permission", e);
            }
        }
    }

}
