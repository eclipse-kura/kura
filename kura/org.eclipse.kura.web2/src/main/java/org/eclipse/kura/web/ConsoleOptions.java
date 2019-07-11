/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *
 *******************************************************************************/
package org.eclipse.kura.web;

import java.util.Map;

import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.util.configuration.Property;

public class ConsoleOptions {

    static final String PROP_CONSOLE_USERNAME = "console.username.value";
    static final String PROP_CONSOLE_PASSWORD = "console.password.value";
    static final String PROP_APP_ROOT = "app.root";
    static final String PROP_SESSION_MAX_INACTIVITY_INTERVAL = "session.max.inactivity.interval";
    static final String PROP_ACCESS_BANNER_ENABLED = "access.banner.enabled";
    static final String PROP_ACCESS_BANNER_CONTENT = "access.banner.content";

    private static final Property<String> CONSOLE_USERNAME = new Property<>(PROP_CONSOLE_USERNAME, "admin");
    private static final Property<String> CONSOLE_PASSWORD = new Property<>(PROP_CONSOLE_PASSWORD, "admin");
    private static final Property<String> CONSOLE_APP_ROOT = new Property<>(PROP_APP_ROOT, "/admin/console");
    private static final Property<Integer> SESSION_MAX_INACTIVITY_INTERVAL = new Property<>(
            PROP_SESSION_MAX_INACTIVITY_INTERVAL, 15);
    private static final Property<Boolean> ACCESS_BANNER_ENABLED = new Property<>(PROP_ACCESS_BANNER_ENABLED, false);
    private static final Property<String> ACCESS_BANNER_CONTENT = new Property<>(PROP_ACCESS_BANNER_CONTENT, "");

    private final String username;
    private final String userPassword;
    private final String appRoot;
    private final int sessionMaxInactivityInterval;
    private final boolean bannerEnabled;
    private final String bannerContent;

    public ConsoleOptions(Map<String, Object> properties) {
        this.username = CONSOLE_USERNAME.get(properties);
        this.userPassword = CONSOLE_PASSWORD.get(properties); //TODO: to Password object?
        this.appRoot = CONSOLE_APP_ROOT.get(properties);
        this.sessionMaxInactivityInterval = SESSION_MAX_INACTIVITY_INTERVAL.get(properties);
        this.bannerEnabled = ACCESS_BANNER_ENABLED.get(properties);
        this.bannerContent = ACCESS_BANNER_CONTENT.get(properties);
    }

    public String getUsername() {
        return this.username;
    }

    public String getUserPassword() {
        return this.userPassword;
    }

    public String getAppRoot() {
        return this.appRoot;
    }

    public int getSessionMaxInactivityInterval() {
        return this.sessionMaxInactivityInterval;
    }

    public boolean isBannerEnabled() {
        return this.bannerEnabled;
    }

    public String getBannerContent() {
        return this.bannerContent;
    }

}
