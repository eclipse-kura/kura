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

import org.eclipse.kura.util.configuration.Property;
import org.eclipse.kura.web.shared.model.GwtConsoleUserOptions;

public class ConsoleOptions {

    static final String PROP_CONSOLE_USERNAME = "console.username.value";
    static final String PROP_CONSOLE_PASSWORD = "console.password.value";
    static final String PROP_APP_ROOT = "app.root";
    static final String PROP_SESSION_MAX_INACTIVITY_INTERVAL = "session.max.inactivity.interval";
    static final String PROP_ACCESS_BANNER_ENABLED = "access.banner.enabled";
    static final String PROP_ACCESS_BANNER_CONTENT = "access.banner.content";

    private static final String PROP_PW_MIN_LENGTH = "new.password.min.length";
    private static final String PROP_PW_REQUIRE_DIGITS = "new.password.require.digits";
    private static final String PROP_PW_REQUIRE_SPECIAL_CHARS = "new.password.require.special.characters";
    private static final String PROP_PW_REQUIRE_BOTH_CASES = "new.password.require.both.cases";

    private static final Property<String> CONSOLE_USERNAME = new Property<>(PROP_CONSOLE_USERNAME, "admin");
    private static final Property<String> CONSOLE_PASSWORD = new Property<>(PROP_CONSOLE_PASSWORD, "admin");
    private static final Property<String> CONSOLE_APP_ROOT = new Property<>(PROP_APP_ROOT, "/admin/console");
    private static final Property<Integer> SESSION_MAX_INACTIVITY_INTERVAL = new Property<>(
            PROP_SESSION_MAX_INACTIVITY_INTERVAL, 15);
    private static final Property<Boolean> ACCESS_BANNER_ENABLED = new Property<>(PROP_ACCESS_BANNER_ENABLED, false);
    private static final Property<String> ACCESS_BANNER_CONTENT = new Property<>(PROP_ACCESS_BANNER_CONTENT, "");

    private static final Property<Integer> PW_MIN_LENGTH = new Property<>(PROP_PW_MIN_LENGTH, 0);
    private static final Property<Boolean> PW_REQUIRE_DIGITS = new Property<>(PROP_PW_REQUIRE_DIGITS, false);
    private static final Property<Boolean> PW_REQUIRE_SPECIAL_CHARS = new Property<>(PROP_PW_REQUIRE_SPECIAL_CHARS,
            false);
    private static final Property<Boolean> PW_REQUIRE_BOTH_CASES = new Property<>(PROP_PW_REQUIRE_BOTH_CASES, false);

    private final String username;
    private final String userPassword;
    private final String appRoot;
    private final int sessionMaxInactivityInterval;
    private final boolean bannerEnabled;
    private final String bannerContent;
    private final GwtConsoleUserOptions userOptions;

    public ConsoleOptions(Map<String, Object> properties) {
        this.username = CONSOLE_USERNAME.get(properties);
        this.userPassword = CONSOLE_PASSWORD.get(properties); // TODO: to Password object?
        this.appRoot = CONSOLE_APP_ROOT.get(properties);
        this.sessionMaxInactivityInterval = SESSION_MAX_INACTIVITY_INTERVAL.get(properties);
        this.bannerEnabled = ACCESS_BANNER_ENABLED.get(properties);
        this.bannerContent = ACCESS_BANNER_CONTENT.get(properties);
        this.userOptions = extractUserOptions(properties);
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

    public GwtConsoleUserOptions getUserOptions() {
        return new GwtConsoleUserOptions(this.userOptions);
    }

    public static GwtConsoleUserOptions extractUserOptions(final Map<String, Object> properties) {
        final GwtConsoleUserOptions result = new GwtConsoleUserOptions();

        result.setPasswordMinimumLength(PW_MIN_LENGTH.get(properties));
        result.setPasswordRequireDigits(PW_REQUIRE_DIGITS.get(properties));
        result.setPasswordRequireSpecialChars(PW_REQUIRE_SPECIAL_CHARS.get(properties));
        result.setPasswordRequireBothCases(PW_REQUIRE_BOTH_CASES.get(properties));

        return result;
    }

}
