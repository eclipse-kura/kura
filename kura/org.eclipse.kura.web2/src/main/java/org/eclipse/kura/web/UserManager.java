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
import org.eclipse.kura.identity.Permission;
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
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

    public void authenticateWithPassword(final String username, final String password) throws GwtKuraException {
        try {
            this.identityService.checkPassword(username, password.toCharArray());
        } catch (Exception e) {
            throw toGwt(e);
        }
    }

    public void requirePermissions(final String username, final String... requiredPermissions) throws GwtKuraException {

        try {
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
        } catch (final Exception e) {
            throw toGwt(e);
        }
    }

    public boolean isPasswordChangeRequired(final String username) throws GwtKuraException {

        try {
            return this.identityService
                    .getIdentityConfiguration(username, Collections.singleton(PasswordConfiguration.class))
                    .flatMap(p -> p.getComponent(PasswordConfiguration.class))
                    .map(p -> p.isPasswordChangeNeeded())
                    .orElse(false);
        } catch (final Exception e) {
            throw toGwt(e);
        }

    }

    public void createUser(final String userName) throws GwtKuraException {
        try {
            this.identityService.createIdentity(userName);
        } catch (final Exception e) {
            logger.warn("failed to create user", e);
            throw toGwt(e);
        }
    }

    public void deleteUser(final String userName) throws GwtKuraException {
        try {
            this.identityService.deleteIdentity(userName);
        } catch (final Exception e) {
            logger.warn("failed to delete user", e);
            throw toGwt(e);
        }
    }

    public void setUserPassword(final String userName, final String userPassword) throws GwtKuraException {

        try {
            this.identityService.updateIdentityConfiguration(new IdentityConfiguration(userName,
                    Collections.singletonList(
                            new PasswordConfiguration(false, true,
                                    Optional.of(userPassword.toCharArray()), Optional.empty()))));
        } catch (final Exception e) {
            throw toGwt(e);
        }

    }

    public Set<String> getDefinedPermissions() throws GwtKuraException {
        try {
            return this.identityService.getPermissions().stream().map(Permission::getName).collect(Collectors.toSet());
        } catch (final Exception e) {
            logger.warn("failed to get defined permissions", e);
            throw toGwt(e);
        }
    }

    public Set<GwtUserConfig> getUserConfig() throws GwtKuraException {

        return getUserConfig(ALL_COMPONENTS);

    }

    public Set<GwtUserConfig> getUserConfig(
            final Set<Class<? extends IdentityConfigurationComponent>> componentsToReturn)
            throws GwtKuraException {

        try {
            return this.identityService
                    .getIdentitiesConfiguration(componentsToReturn)
                    .stream().map(this::getUserConfig).collect(Collectors.toSet());
        } catch (final Exception e) {
            logger.warn("failed to get user configuration", e);
            throw toGwt(e);
        }
    }

    public GwtUserConfig getUserDefaultConfig(final String name) throws GwtKuraException {
        return getUserDefaultConfig(name, ALL_COMPONENTS);
    }

    public GwtUserConfig getUserDefaultConfig(final String name,
            final Set<Class<? extends IdentityConfigurationComponent>> componentsToReturn) throws GwtKuraException {
        try {
            return getUserConfig(this.identityService.getIdentityDefaultConfiguration(name, componentsToReturn));
        } catch (final Exception e) {
            logger.warn("failed to get user default configuration", e);
            throw toGwt(e);
        }
    }

    public Optional<GwtUserConfig> getUserConfig(final String name) throws GwtKuraException {
        return getUserConfig(name, ALL_COMPONENTS);
    }

    public Optional<GwtUserConfig> getUserConfig(final String name,
            final Set<Class<? extends IdentityConfigurationComponent>> componentsToReturn) throws GwtKuraException {
        try {
            return this.identityService
                    .getIdentityConfiguration(name, componentsToReturn)
                    .map(this::getUserConfig);
        } catch (final Exception e) {
            throw toGwt(e);
        }
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

    public Optional<Integer> getCredentialsHash(final String userName) throws GwtKuraException {

        try {
            return this.identityService
                    .getIdentityConfiguration(userName, Collections.singleton(PasswordConfiguration.class))
                    .flatMap(i -> i.getComponent(PasswordConfiguration.class)).map(i -> i.getPasswordHash().hashCode());
        } catch (final Exception e) {
            throw toGwt(e);
        }

    }

    public void setUserConfig(final Set<GwtUserConfig> userConfigs) throws GwtKuraException {

        try {
            final List<IdentityConfiguration> configurations = new ArrayList<>();

            for (final GwtUserConfig config : userConfigs) {
                configurations.add(buildIdentityConfiguration(config));
            }

            runFallibleTasks(configurations, this.identityService::validateIdentityConfiguration);

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

            runFallibleTasks(configurations, this.identityService::updateIdentityConfiguration);
        } catch (final Exception e) {
            logger.warn("failed to update user configuration", e);
            throw toGwt(e);
        }

    }

    private IdentityConfiguration buildIdentityConfiguration(final GwtUserConfig config) throws GwtKuraException {
        final Set<Permission> permissions = config.getPermissions().stream().map(Permission::new)
                .collect(Collectors.toSet());
        final AssignedPermissions assignedPermissions = new AssignedPermissions(permissions);

        final Optional<char[]> newPassword = config.getNewPassword().map(String::toCharArray);

        final PasswordConfiguration passwordData = new PasswordConfiguration(config.isPasswordChangeNeeded(),
                config.isPasswordAuthEnabled(), newPassword, Optional.empty());

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

    private interface FallibleConsumer<T> {
        public void apply(final T object) throws Exception;
    }

    /**
     * @param <T>
     * @param configs
     * @param consumer
     * @throws KuraException
     */
    private <T> void runFallibleTasks(final List<IdentityConfiguration> configs,
            final FallibleConsumer<IdentityConfiguration> consumer) throws GwtKuraException {
        final StringBuilder builder = new StringBuilder();
        boolean hasFailures = false;

        for (final IdentityConfiguration config : configs) {
            try {
                consumer.apply(config);
            } catch (final Exception e) {
                if (hasFailures) {
                    builder.append("; ");
                }
                hasFailures = true;
                builder.append(config.getName() + ": " + e.getMessage());
            }
        }

        if (hasFailures) {
            throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, null, builder.toString());
        }
    }

    private GwtKuraException toGwt(final Exception e) {
        if (e instanceof GwtKuraException) {
            return (GwtKuraException) e;
        }
        return new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, null, e.getMessage());
    }
}
