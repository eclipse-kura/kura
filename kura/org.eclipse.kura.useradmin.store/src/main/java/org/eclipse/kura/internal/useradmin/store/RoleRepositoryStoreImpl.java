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
import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.ConfigurationService;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.useradmin.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;

public class RoleRepositoryStoreImpl implements RoleRepositoryStore, EventHandler, ConfigurableComponent {

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

        doUpdate(properties);

        logger.info("activating...done");
    }

    public synchronized void update(final Map<String, Object> properties) {

        if (isSelfUpdate(properties)) {
            return;
        }

        logger.info("updating...");

        doUpdate(properties);

        logger.info("updating...done");
    }

    public void deactivate() {
        logger.info("deactivating...");

        executorService.shutdown();

        logger.info("deactivating...done");
    }

    @Override
    public synchronized Role addRole(final String name, final int type) throws Exception {

        if (roles.containsKey(name)) {
            return null;
        }

        final Role role = RoleFactory.createRole(type, name);

        roles.put(name, role);

        return role;
    }

    @Override
    public synchronized Role getRoleByName(final String name) throws Exception {
        return roles.get(name);
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

        for (final Role role : roles.values()) {
            if (!filter.isPresent() || filter.get().match(role.getProperties())) {
                result.add(role);
            }
        }

        return result.toArray(new Role[result.size()]);
    }

    @Override
    public synchronized Role removeRole(final String role) throws Exception {
        return Optional.ofNullable(roles.remove(role)).orElse(null);
    }

    private boolean isSelfUpdate(final Map<String, Object> properties) {
        final Object id = properties.get(INTERNAL_UPDATE_ID_PROP_NAME);

        if (id instanceof Long) {
            final long updateId = (Long) id;

            if (updateIds.contains(updateId)) {
                updateIds.remove(updateId);
                return true;
            }
        }

        return false;
    }

    public long getNextUpdateId() {
        final long updateId = nextUpdateId++;

        updateIds.add(updateId);

        return updateId;
    }

    private void doUpdate(final Map<String, Object> properties) {
        options = new RoleRepositoryStoreOptions(properties);

        try {
            roles = decode(options.getRolesConfig());
        } catch (final Exception e) {
            logger.warn("failed to deserialize roles", e);
        }
    }

    private synchronized void scheduleStore() {

        if (storeTask.isPresent()) {
            storeTask.get().cancel(false);
            storeTask = Optional.empty();
        }

        storeTask = Optional
                .of(executorService.schedule(this::storeNow, options.getWriteDelayMs(), TimeUnit.MILLISECONDS));
    }

    private synchronized void storeNow() {
        final JsonObject stored = new JsonObject();

        for (final Role role : roles.values()) {
            stored.add(role.getName(), RoleSerializer.serializeRole(role));
        }

        final Map<String, Object> properties = new RoleRepositoryStoreOptions(stored.toString(),
                options.getWriteDelayMs()).toProperties();

        properties.put(INTERNAL_UPDATE_ID_PROP_NAME, getNextUpdateId());

        try {
            configurationService.updateConfiguration(RoleRepositoryStoreImpl.class.getName(), properties);
        } catch (final KuraException e) {
            logger.warn("Failed to store configuration", e);
        }

        storeTask = Optional.empty();
    }

    private final Map<String, Role> decode(final String encodedRoles) throws DeserializationException {
        try {
            final JsonObject object = Json.parse(encodedRoles).asObject();
            final Map<String, Role> result = new HashMap<>();

            for (final Member member : object) {
                final Role role = RoleSerializer.deserializeRole(member.getValue().asObject());
                result.put(member.getName(), role);
            }

            for (final Member member : object) {
                RoleSerializer.assignMembers(member.getValue().asObject(), result.get(member.getName()), result);
            }

            return result;

        } catch (final DeserializationException e) {
            throw e;
        } catch (final Exception e) {
            throw new DeserializationException("failed to deserialize role repository", e);
        }

    }

    @Override
    public void handleEvent(Event event) {
        scheduleStore();
    }

}
