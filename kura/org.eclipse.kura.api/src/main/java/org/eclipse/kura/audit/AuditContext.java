/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.audit;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents a context that can be created by a framework entry point for audit purposes. Examples of entry points
 * are the Web UI, a cloud connection or the Rest service.
 * An AuditContext contains a set of properties. Some property are well known and described by the
 * {@link AuditConstants} enumeration, other custom properties can be added by the entry point implementation.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.2
 */
@ProviderType
public class AuditContext {

    private static ThreadLocal<AuditContext> localContext = new ThreadLocal<>();

    private final Map<String, String> properties;

    /**
     * Creates a new {@link AuditContext}.
     * 
     * @param properties
     *            The properties to be associated with this context. It must be a non null mutable map.
     */
    public AuditContext(final Map<String, String> properties) {
        this.properties = requireNonNull(properties);
    }

    /**
     * Returns the properties associated with this context
     * 
     * @return the properties.
     */
    public Map<String, String> getProperties() {
        return this.properties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return properties.toString();
    }

    /**
     * Creates a copy of this AuditContext with a new and independent set of properties.
     * 
     * @return a copy of this AuditContext.
     */
    public AuditContext copy() {
        return new AuditContext(new HashMap<>(this.properties));
    }

    /**
     * Returns the {@link AuditContext} associated with the current thread, if set, or a context with
     * {@link AuditConstants#KEY_ENTRY_POINT} set to {@link AuditConstants#ENTRY_POINT_INTERNAL} otherwise.
     * 
     * @return
     */
    public static AuditContext current() {
        final AuditContext current = localContext.get();

        if (current != null) {
            return current;
        } else {
            final Map<String, String> properties = new HashMap<>(1);
            properties.put(AuditConstants.KEY_ENTRY_POINT.getValue(), "Internal");

            return new AuditContext(properties);
        }
    }

    /**
     * Sets the provided context as the thread local instance and returns a {@link Scope} that can be used for removing
     * it.
     * Subsequent calls to {@link AuditContext#current()} performed on the same thread will return the provided
     * {@link AuditContext} instance until the returned {@link Scope} is closed.
     * 
     * @param context
     *            the context
     * @return a {@link Scope} that removes the thread local context when closed.
     */
    public static Scope openScope(final AuditContext context) {
        return new Scope(context);
    }

    /**
     * Resents an {@link AutoCloseable} scope that removes the associated {@link AuditContext} when closed.
     * See {@link AuditContext#openScope(AuditContext)} for more details.
     * 
     * @noextend This class is not intended to be subclassed by clients.
     * @since 2.2
     */
    @ProviderType
    public static class Scope implements AutoCloseable {

        private final AuditContext context;

        private Scope(final AuditContext context) {
            this.context = context;
            localContext.set(context);
        }

        /**
         * Removes the associated {@link AuditContext} from thread local storage.
         * 
         * {@inheritDoc}
         */
        @Override
        public void close() {
            if (localContext.get() == context) {
                localContext.remove();
            }
        }

    }

}
