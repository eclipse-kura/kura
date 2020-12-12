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
package org.eclipse.kura.internal.useradmin.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.felix.useradmin.RoleFactory;
import org.apache.felix.useradmin.RoleRepositoryStore;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.internal.useradmin.store.RoleSerializer.RoleBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.service.useradmin.UserAdminEvent;
import org.osgi.service.useradmin.UserAdminListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;

public class RoleRepositoryStoreImpl implements RoleRepositoryStore, UserAdminListener, ConfigurableComponent {

    private static final String INTERNAL_UPDATE_ID_PROP_NAME = "internal.update.id";

    private static final Logger logger = LoggerFactory.getLogger(RoleRepositoryStoreImpl.class);

    private Map<String, Role> roles = new HashMap<>();
    private RoleRepositoryStoreOptions options;

    long nextUpdateId = 0;
    private final Set<Long> updateIds = new HashSet<>();

    private ConfigurationService configurationService;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private Optional<ScheduledFuture<?>> storeTask = Optional.empty();

    public void setConfigurationService(final ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void activate(final Map<String, Object> properties) {
        logger.info("activating...");

        doUpdate(properties, RoleFactory::createRole);

        logger.info("activating...done");
    }

    public synchronized void update(final Map<String, Object> properties) {

        if (isSelfUpdate(properties)) {
            logger.info("ignoring self update");
            return;
        }

        logger.info("updating...");

        final RoleBuilder roleBuilder;

        final BundleContext bundleContext = FrameworkUtil.getBundle(RoleRepositoryStoreImpl.class).getBundleContext();

        final ServiceReference<UserAdmin> userAdminRef = bundleContext.getServiceReference(UserAdmin.class);

        final UserAdmin userAdmin;

        if (userAdminRef != null) {
            userAdmin = bundleContext.getService(userAdminRef);
        } else {
            userAdmin = null;
        }

        if (userAdmin != null) {
            roleBuilder = (type, name) -> userAdmin.createRole(name, type);

            final Set<String> roleNames = new HashSet<>(this.roles.keySet());

            for (final String name : roleNames) {
                userAdmin.removeRole(name);
            }

        } else {
            roleBuilder = RoleFactory::createRole;
        }

        try {
            doUpdate(properties, roleBuilder);
        } finally {
            if (userAdmin != null) {
                bundleContext.ungetService(userAdminRef);
            }
        }

        logger.info("updating...done");
    }

    public void deactivate() {
        logger.info("deactivating...");

        this.executorService.shutdown();

        logger.info("deactivating...done");
    }

    @Override
    public synchronized Role addRole(final String name, final int type) throws Exception {

        if (this.roles.containsKey(name)) {
            return null;
        }

        final Role role = RoleFactory.createRole(type, name);

        this.roles.put(name, role);

        return role;
    }

    @Override
    public synchronized Role getRoleByName(final String name) throws Exception {
        return this.roles.get(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized Role[] getRoles(final String filterString) throws Exception {

        final Optional<Filter> filter;

        if (filterString != null) {
            filter = Optional.of(FrameworkUtil.createFilter(filterString));
        } else {
            filter = Optional.empty();
        }

        final List<Role> result = new ArrayList<>();

        for (final Role role : this.roles.values()) {
            if (!filter.isPresent() || filter.get().match(role.getProperties())) {
                result.add(role);
            }
        }

        return result.toArray(new Role[result.size()]);
    }

    @Override
    public synchronized Role removeRole(final String role) throws Exception {
        return Optional.ofNullable(this.roles.remove(role)).orElse(null);
    }

    private boolean isSelfUpdate(final Map<String, Object> properties) {
        final Object id = properties.get(INTERNAL_UPDATE_ID_PROP_NAME);

        if (id instanceof Long) {
            final long updateId = (Long) id;

            if (this.updateIds.contains(updateId)) {
                this.updateIds.remove(updateId);
                return true;
            }
        }

        return false;
    }

    public long getNextUpdateId() {
        final long updateId = this.nextUpdateId++;

        this.updateIds.add(updateId);

        return updateId;
    }

    private void doUpdate(final Map<String, Object> properties, final RoleBuilder roleBuilder) {
        this.options = new RoleRepositoryStoreOptions(properties);

        try {
            this.roles = decode(this.options, roleBuilder);
        } catch (final Exception e) {
            logger.warn("failed to deserialize roles", e);
        }
    }

    private synchronized void scheduleStore() {

        if (this.storeTask.isPresent()) {
            this.storeTask.get().cancel(false);
            this.storeTask = Optional.empty();
        }

        this.storeTask = Optional.of(
                this.executorService.schedule(this::storeNow, this.options.getWriteDelayMs(), TimeUnit.MILLISECONDS));
    }

    private synchronized void storeNow() {
        try {
            final JsonArray rolesArray = new JsonArray();
            final JsonArray usersArray = new JsonArray();
            final JsonArray groupsArray = new JsonArray();

            for (final Role role : this.roles.values()) {
                final int type = role.getType();

                if (type == Role.ROLE) {
                    rolesArray.add(RoleSerializer.serializeRole(role));
                } else if (type == Role.USER) {
                    usersArray.add(RoleSerializer.serializeRole(role));
                } else if (type == Role.GROUP) {
                    groupsArray.add(RoleSerializer.serializeRole(role));
                }
            }

            final Map<String, Object> properties = new RoleRepositoryStoreOptions(rolesArray.toString(), //
                    usersArray.toString(), //
                    groupsArray.toString(), //
                    this.options.getWriteDelayMs() //
            ).toProperties();

            properties.put(INTERNAL_UPDATE_ID_PROP_NAME, getNextUpdateId());

            this.configurationService.updateConfiguration(RoleRepositoryStoreImpl.class.getName(), properties);
        } catch (final Exception e) {
            logger.warn("Failed to store configuration", e);
        } finally {
            this.storeTask = Optional.empty();
        }

    }

    private final Map<String, Role> decode(final RoleRepositoryStoreOptions options, final RoleBuilder roleBuilder)
            throws DeserializationException {
        try {
            final Map<String, Role> result = new HashMap<>();

            decode(Json.parse(options.getRolesConfig()).asArray(), Role.class, result, roleBuilder);
            decode(Json.parse(options.getUsersConfig()).asArray(), User.class, result, roleBuilder);

            final JsonArray groups = Json.parse(options.getGroupsConfig()).asArray();

            decode(groups, Group.class, result, roleBuilder);

            for (final JsonValue member : groups) {
                RoleSerializer.assignMembers(member.asObject(), result);
            }

            return result;

        } catch (final DeserializationException e) {
            throw e;
        } catch (final Exception e) {
            throw new DeserializationException("failed to deserialize role repository", e);
        }

    }

    private void decode(final JsonArray array, final Class<? extends Role> classz, final Map<String, Role> target,
            final RoleBuilder roleBuilder) throws DeserializationException {
        for (final JsonValue member : array) {
            final Role role = RoleSerializer.deserializeRole(classz, member.asObject(), roleBuilder);
            target.put(role.getName(), role);
        }
    }

    @Override
    public void roleChanged(UserAdminEvent arg0) {
        logger.debug("received event");
        scheduleStore();
    }
}
