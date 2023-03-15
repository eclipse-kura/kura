/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.db.sqlite.provider;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class SqliteDbServiceOptionsTest {

    @Test
    public void shouldSupportDefaultPath() {
        givenConfigurationProperty("kura.service.pid", "foo");

        whenOptionsAreCreated();

        thenDbPathIs("/opt/mydb.sqlite");
        thenDbUrlIs("jdbc:sqlite:file:foo?mode=memory&cache=shared");
    }

    @Test
    public void shouldSupportPersistedMode() {
        givenConfigurationProperty("kura.service.pid", "foo");
        givenConfigurationProperty("db.mode", "PERSISTED");
        givenConfigurationProperty("db.path", "/tmp/foo");

        whenOptionsAreCreated();

        thenDbPathIs("/tmp/foo");
        thenDbUrlIs("jdbc:sqlite:file:/tmp/foo");
    }

    @Test
    public void shouldIgnoreURIParameters() {
        givenConfigurationProperty("kura.service.pid", "foo");
        givenConfigurationProperty("db.mode", "PERSISTED");
        givenConfigurationProperty("db.path", "/tmp/foo?baz=bar");

        whenOptionsAreCreated();

        thenDbPathIs("/tmp/foo");
        thenDbUrlIs("jdbc:sqlite:file:/tmp/foo");
    }

    @Test
    public void shouldIgnoreURIParametersDoubleQuestionMark() {
        givenConfigurationProperty("kura.service.pid", "foo");
        givenConfigurationProperty("db.mode", "PERSISTED");
        givenConfigurationProperty("db.path", "/tmp/foo?baz=bar?foo");

        whenOptionsAreCreated();

        thenDbPathIs("/tmp/foo");
        thenDbUrlIs("jdbc:sqlite:file:/tmp/foo");
    }

    private final Map<String, Object> properties = new HashMap<>();
    private SqliteDbServiceOptions options;

    private void givenConfigurationProperty(final String key, final Object value) {
        properties.put(key, value);
    }

    private void whenOptionsAreCreated() {
        this.options = new SqliteDbServiceOptions(properties);
    }

    private void thenDbPathIs(final String expectedPath) {
        assertEquals(expectedPath, this.options.getPath());
    }

    private void thenDbUrlIs(final String expectedDbUrl) {
        assertEquals(expectedDbUrl, this.options.getDbUrl());
    }

}
