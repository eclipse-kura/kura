/*******************************************************************************
 * Copyright (c) 2016, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.shared;

public enum ForwardedEventTopic {

    // This enum defines the subset of events that will be forwarded to the web ui
    // If you want to forward other events it's enough to define an enum item here

    CLOUD_CONNECTION_STATUS_ESTABLISHED("org/eclipse/kura/cloud/CloudConnectionStatus/ESTABLISHED"),
    CLOUD_CONNECTION_STATUS_LOST("org/eclipse/kura/cloud/CloudConnectionStatus/LOST"),

    DEPLOYMENT_PACKAGE_INSTALLED("org/eclipse/kura/deployment/agent/INSTALLED"),
    DEPLOYMENT_PACKAGE_UNINSTALLED("org/eclipse/kura/deployment/agent/UNINSTALLED"),

    BUNDLE_INSTALLED("org/osgi/framework/BundleEvent/INSTALLED"),
    BUNDLE_STARTED("org/osgi/framework/BundleEvent/STARTED"),
    BUNDLE_STOPPED("org/osgi/framework/BundleEvent/STOPPED"),
    BUNDLE_UPDATED("org/osgi/framework/BundleEvent/UPDATED"),
    BUNDLE_UNINSTALLED("org/osgi/framework/BundleEvent/UNINSTALLED"),
    BUNDLE_RESOLVED("org/osgi/framework/BundleEvent/RESOLVED"),
    BUNDLE_UNRESOLVED("org/osgi/framework/BundleEvent/UNRESOLVED"),

    ROLE_CREATED("org/osgi/service/useradmin/UserAdmin/ROLE_CREATED"),
    ROLE_CHANGED("org/osgi/service/useradmin/UserAdmin/ROLE_CHANGED"),
    ROLE_REMOVED("org/osgi/service/useradmin/UserAdmin/ROLE_REMOVED"),

    ROLE_CREATED_SHORT("ROLE_CREATED"),
    ROLE_CHANGED_SHORT("ROLE_CHANGED"),
    ROLE_REMOVED_SHORT("ROLE_REMOVED"),

    TAMPER_EVENT("org/eclipse/kura/security/tamper/detection/TamperEvent/TAMPER_STATUS_CHANGED"),

    CONCURRENT_WRITE_EVENT("org/eclipse/kura/web/client/ui/CONCURRENT_WRITE_EVENT");

    private final String topic;

    private ForwardedEventTopic(String topic) {
        this.topic = topic;
    }

    @Override
    public String toString() {
        return this.topic;
    }
}
