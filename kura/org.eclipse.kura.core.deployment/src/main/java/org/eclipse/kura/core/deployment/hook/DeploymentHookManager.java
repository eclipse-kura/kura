/*******************************************************************************
 * Copyright (c) 2017 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.core.deployment.hook;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.deployment.hook.DeploymentHook;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeploymentHookManager {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentHookManager.class);

    private final Map<String, DeploymentHook> registeredHooks = new HashMap<>();
    private final Map<String, HookAssociation> associations = new HashMap<>();
    private BundleContext bundleContext;

    public synchronized void bindHook(ServiceReference<DeploymentHook> hook) {

        final Object rawHookId = hook.getProperty(ConfigurationService.KURA_SERVICE_PID);

        if (!(rawHookId instanceof String)) {
            logger.warn("Found hook with invalid {}, not registering", ConfigurationService.KURA_SERVICE_PID);
            return;
        }

        final String hookId = (String) rawHookId;

        if (registeredHooks.containsKey(hookId)) {
            logger.warn("Found duplicated hook with id {}, not registering", ConfigurationService.KURA_SERVICE_PID);
            return;
        }

        this.registeredHooks.put(hookId, getBundleContext().getService(hook));

        logger.info("Hook registered: {}", hookId);
        updateAssociations();
    }

    public synchronized void unbindHook(ServiceReference<DeploymentHook> hook) {

        final Object rawHookId = hook.getProperty(ConfigurationService.KURA_SERVICE_PID);

        if (!(rawHookId instanceof String)) {
            return;
        }

        final String hookId = (String) rawHookId;

        final DeploymentHook removedHook = this.registeredHooks.remove(hookId);

        updateAssociations();
        if (removedHook != null) {
            getBundleContext().ungetService(hook);
            logger.info("Hook unregistered: {}", hookId);
        }

    }

    private BundleContext getBundleContext() {
        if (this.bundleContext == null) {
            this.bundleContext = FrameworkUtil.getBundle(DeploymentHookManager.class).getBundleContext();
        }
        return this.bundleContext;
    }

    public synchronized void updateAssociations(Properties properties) {
        this.associations.clear();
        if (properties == null) {
            return;
        }
        for (Entry<Object, Object> entry : properties.entrySet()) {
            final Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            final String type = entry.getKey().toString();
            final String hookId = value.toString();
            final HookAssociation association = new HookAssociation(type, hookId);
            this.associations.put(type, association);
            logger.info("Association added: {}", association);
        }
        updateAssociations();
    }

    private void updateAssociations() {
        for (HookAssociation association : this.associations.values()) {
            final DeploymentHook hook = registeredHooks.get(association.hookId);
            association.hook = hook;
            logger.info("Association updated: {}", association);
        }
    }

    public synchronized DeploymentHook getHook(String type) {
        final HookAssociation association = this.associations.get(type);

        if (association == null) {
            return null;
        }

        return association.hook;
    }

    public Map<String, HookAssociation> getHookAssociations() {
        return Collections.unmodifiableMap(this.associations);
    }

    public Map<String, DeploymentHook> getRegisteredHooks() {
        return Collections.unmodifiableMap(this.registeredHooks);
    }

    public static class HookAssociation {

        private String type;
        private String hookId;
        private DeploymentHook hook;

        public HookAssociation(String type, String hookId) {
            this.type = type;
            this.hookId = hookId;
        }

        public String getRequestType() {
            return type;
        }

        public String getHookId() {
            return hookId;
        }

        public DeploymentHook getDeploymentHook() {
            return hook;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("request type: ").append(type).append(" hook id: ").append(hookId).append(" status: ")
                    .append(hook == null ? "NOT BOUND" : "BOUND");
            return builder.toString();
        }
    }

}
