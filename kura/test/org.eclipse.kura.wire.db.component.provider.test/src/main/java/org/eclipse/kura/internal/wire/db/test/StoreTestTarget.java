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
package org.eclipse.kura.internal.wire.db.test;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface StoreTestTarget {

    public static final StoreTestTarget H2 = new H2();
    public static final StoreTestTarget SQLITE = new Sqlite();

    public String factoryPid();

    public Map<String, Object> getConfigurationForDatabase(final String name);

    public class H2 implements StoreTestTarget {

        @Override
        public String factoryPid() {
            return "org.eclipse.kura.core.db.H2DbService";
        }

        @Override
        public Map<String, Object> getConfigurationForDatabase(String name) {

            return Collections.singletonMap("db.connector.url", "jdbc:h2:mem:testdb-" + name);
        }

        @Override
        public String toString() {
            return "H2";
        }
    }

    public class Sqlite implements StoreTestTarget {

        @Override
        public String factoryPid() {
            return "org.eclipse.kura.db.SQLiteDbService";
        }

        @Override
        public String toString() {
            return "SQLite";
        }

        @Override
        public Map<String, Object> getConfigurationForDatabase(String name) {
            final Map<String, Object> properties = new HashMap<>();

            properties.put("db.mode", "PERSISTED");
            try {
                properties.put("db.path", Files.createTempDirectory(null).toFile().getPath() + "/" + name);
            } catch (IOException e) {
                fail("unable to create temporary directory");
            }

            return properties;
        }
    }
}
