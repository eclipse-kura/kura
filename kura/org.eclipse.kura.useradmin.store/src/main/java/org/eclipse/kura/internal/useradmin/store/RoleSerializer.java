package org.eclipse.kura.internal.useradmin.store;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.felix.useradmin.RoleFactory;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.eclipsesource.json.JsonValue;

class RoleSerializer {

    private static final Logger logger = LoggerFactory.getLogger(RoleSerializer.class);

    private static final String NAME = "name";

    private static final String ROLE_PROPERTIES = "roleProperties";
    private static final String ROLE_TYPE = "roleType";

    private static final String USER_CREDENTIALS = "userCredentials";

    private static final String GROUP_BASIC_MEMBERS = "groupBasicMembers";
    private static final String GROUP_REQUIRED_MEMBERS = "groupRequiredMembers";

    private static JsonArray serializeByteArray(final byte[] value) {
        final JsonArray result = new JsonArray();

        for (final byte b : value) {
            result.add(b);
        }

        return result;
    }

    private static JsonArray serializeRoleNames(final Role[] roles) {
        final JsonArray result = new JsonArray();

        for (final Role r : roles) {
            result.add(r.getName());
        }

        return result;
    }

    @SuppressWarnings("rawtypes")
    static JsonObject serializeProperties(final Dictionary properties) {
        final JsonObject result = new JsonObject();

        final Enumeration<?> keys = properties.keys();

        while (keys.hasMoreElements()) {
            final Object next = keys.nextElement();

            if (!(next instanceof String)) {
                logger.warn("unsupported property key: {}", next);
                continue;
            }

            final String key = (String) next;

            final Object value = properties.get(key);

            if (value instanceof String) {
                result.add(key, (String) value);
            } else if (value instanceof byte[]) {
                result.add(key, serializeByteArray((byte[]) value));
            } else {
                logger.warn("unsupported property value: {}", value);
            }
        }

        return result;
    }

    static JsonObject serializeRole(final Role role) {
        final JsonObject result = new JsonObject();

        result.add(NAME, role.getName());
        result.add(ROLE_PROPERTIES, serializeProperties(role.getProperties()));
        result.add(ROLE_TYPE, role.getType());

        if (role instanceof User) {
            final User asUser = (User) role;

            result.add(USER_CREDENTIALS, serializeProperties(asUser.getCredentials()));
        }

        if (role instanceof Group) {
            final Group asGroup = (Group) role;

            result.add(GROUP_BASIC_MEMBERS, serializeRoleNames(asGroup.getMembers()));
            result.add(GROUP_REQUIRED_MEMBERS, serializeRoleNames(asGroup.getRequiredMembers()));

        }

        return result;
    }

    private static byte[] deserializeByteArray(final JsonArray array) {
        final byte[] result = new byte[array.size()];

        for (int i = 0; i < array.size(); i++) {
            result[i] = (byte) array.get(i).asInt();
        }

        return result;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void deserializeProperties(final JsonObject properties, final Dictionary target) {
        for (final Member member : properties) {
            final JsonValue value = member.getValue();

            if (value.isString()) {
                target.put(member.getName(), value.asString());
            } else if (value.isArray()) {
                target.put(member.getName(), deserializeByteArray(value.asArray()));
            }
        }
    }

    static Role deserializeRole(final JsonObject object) throws DeserializationException {
        try {
            final int type = object.get(ROLE_TYPE).asInt();
            final String name = object.get(NAME).asString();

            final Role result = RoleFactory.createRole(type, name);

            final JsonValue roleProperties = object.get(ROLE_PROPERTIES);

            if (roleProperties != null) {
                deserializeProperties(roleProperties.asObject(), result.getProperties());
            }

            if (result instanceof User) {
                final User asUser = (User) result;
                final JsonValue userCredentials = object.get(USER_CREDENTIALS);

                deserializeProperties(userCredentials.asObject(), asUser.getCredentials());
            }

            return result;
        } catch (final Exception e) {
            throw new DeserializationException("failed to deserialize role", e);
        }
    }

    private static void assignMembers(final JsonArray members, final Map<String, Role> roles,
            final Consumer<Role> consumer) {
        for (final JsonValue value : members.asArray()) {
            final String roleName = value.asString();

            final Role member = roles.get(value.asString());

            if (member == null) {
                logger.warn("Role {} cannot be found", roleName);
                continue;
            }

            consumer.accept(member);
        }
    }

    static void assignMembers(final JsonObject object, final Role role, final Map<String, Role> roles)
            throws DeserializationException {
        try {

            if (!(role instanceof Group)) {
                return;
            }

            final Group asGroup = (Group) role;

            final JsonValue basicMembers = object.get(GROUP_BASIC_MEMBERS);

            if (basicMembers != null) {
                assignMembers(basicMembers.asArray(), roles, asGroup::addMember);
            }

            final JsonValue requiredMembers = object.get(GROUP_REQUIRED_MEMBERS);

            if (requiredMembers != null) {
                assignMembers(requiredMembers.asArray(), roles, asGroup::addRequiredMember);
            }

        } catch (final Exception e) {
            throw new DeserializationException("failed to deserialize role", e);
        }
    }

}
