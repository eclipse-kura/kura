/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.db;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class H2DbServiceOptions {

    private static final Property<String> CONNECTOR_URL_PROP = new Property<>("db.connector.url", "jdbc:h2:mem:kuradb");
    private static final Property<String> USER_PROP = new Property<>("db.user", "SA");
    private static final Property<String> PASSWORD_PROP = new Property<String>("db.password", "");
    private static final Property<Integer> CHECKPOINT_INTERVAL_SECONDS_PROP = new Property<>(
            "db.checkpoint.interval.seconds", 900);
    private static final Property<Integer> DEFRAG_INTERVAL_MINUTES_PROP = new Property<>("db.defrag.interval.minutes",
            20);
    private static final Property<Integer> CONNECTION_POOL_MAX_SIZE = new Property<>("db.connection.pool.max.size", 10);

    private static final Pattern FILE_LOG_LEVEL_PATTERN = generatePatternForProperty("trace_level_file");
    private static final Pattern USER_PATTERN = generatePatternForProperty("user");
    private static final Pattern PASSWORD_PATTERN = generatePatternForProperty("password");

    private static final Pattern JDBC_URL_PARSE_PATTERN = Pattern.compile("jdbc:([^:]+):(([^:]+):)?([^;]*)(;.*)?");

    private final String dbUrl;
    private final String user;
    private final char[] password;
    private final long checkpointIntervalSeconds;
    private final long defragIntervalMinutes;
    private final int maxConnectionPoolSize;

    private boolean isInMemory;
    private boolean isFileBased;
    private boolean isZipBased;
    private boolean isRemote;
    private boolean isFileBasedLogLevelSpecified;

    private String baseUrl;
    private String dbDirectory;
    private String dbName;

    public H2DbServiceOptions(Map<String, Object> properties) {
        this.password = PASSWORD_PROP.get(properties).toCharArray();
        this.user = USER_PROP.get(properties);
        this.checkpointIntervalSeconds = CHECKPOINT_INTERVAL_SECONDS_PROP.get(properties);
        this.defragIntervalMinutes = DEFRAG_INTERVAL_MINUTES_PROP.get(properties);
        this.maxConnectionPoolSize = CONNECTION_POOL_MAX_SIZE.get(properties);

        String dbUrl = CONNECTOR_URL_PROP.get(properties);

        dbUrl = USER_PATTERN.matcher(dbUrl).replaceAll("");
        dbUrl = PASSWORD_PATTERN.matcher(dbUrl).replaceAll("");

        this.dbUrl = dbUrl;
        computeUrlParts();
    }

    private static Pattern generatePatternForProperty(String property) {
        StringBuilder patternStringBuilder = new StringBuilder();
        patternStringBuilder.append(';');
        for (int i = 0; i < property.length(); i++) {
            final char c = property.charAt(i);
            patternStringBuilder.append('[').append(Character.toLowerCase(c)).append(Character.toUpperCase(c))
                    .append(']');
        }
        patternStringBuilder.append("=[^;]*");
        return Pattern.compile(patternStringBuilder.toString());
    }

    private void computeUrlParts() {
        final Matcher jdbcUrlMatcher = JDBC_URL_PARSE_PATTERN.matcher(this.dbUrl);

        if (!jdbcUrlMatcher.matches()) {
            throw new IllegalArgumentException("Invalid DB URL");
        }

        String driver = jdbcUrlMatcher.group(1);
        if (driver == null || !"h2".equals(driver)) {
            throw new IllegalArgumentException("JDBC driver must be h2");
        }

        String protocol = jdbcUrlMatcher.group(3);
        String url = jdbcUrlMatcher.group(4);
        if (protocol == null && ".".equals(url)) {
            // jdbc:h2:. is a shorthand for jdbc:h2:mem:
            protocol = "mem";
            url = "";
        } else {
            if (protocol == null) {
                protocol = "file";
            }
            if (url == null) {
                url = "";
            }
        }

        if ("mem".equals(protocol)) {
            this.isInMemory = true;
        } else if ("file".equals(protocol)) {
            this.isFileBased = true;
        } else if ("zip".equals(protocol)) {
            this.isZipBased = true;
        } else {
            this.isRemote = true;
        }

        this.baseUrl = "jdbc:h2:" + protocol + ':' + url;

        if (this.isFileBased) {
            File file = new File(url);
            this.dbDirectory = file.getParent();
            if (this.dbDirectory == null) {
                this.dbDirectory = ".";
            }
            this.dbName = file.getName();
        }

        this.isFileBasedLogLevelSpecified = FILE_LOG_LEVEL_PATTERN.matcher(this.dbUrl).find();
    }

    public String getDbUrl() {
        return this.dbUrl;
    }

    public boolean isFileBased() {
        return this.isFileBased;
    }

    public boolean isInMemory() {
        return this.isInMemory;
    }

    public boolean isZipBased() {
        return this.isZipBased;
    }

    public boolean isRemote() {
        return this.isRemote;
    }

    public String getDbDirectory() {
        return this.dbDirectory;
    }

    public String getDatabaseName() {
        return this.dbName;
    }

    public long getCheckpointIntervalSeconds() {
        return this.checkpointIntervalSeconds;
    }

    public long getDefragIntervalMinutes() {
        return this.defragIntervalMinutes;
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public String getUser() {
        return this.user;
    }

    public char[] getEncryptedPassword() {
        return this.password;
    }

    public int getConnectionPoolMaxSize() {
        return this.maxConnectionPoolSize;
    }

    public boolean isFileBasedLogLevelSpecified() {
        return this.isFileBasedLogLevelSpecified;
    }

    private static class Property<T> {

        private final String key;
        private final T defaultValue;

        public Property(String key, T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        @SuppressWarnings("unchecked")
        public T get(Map<String, Object> properties) {
            final Object value = properties.get(this.key);
            if (this.defaultValue.getClass().isInstance(value)) {
                return (T) value;
            }
            return defaultValue;
        }
    }
}
