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

import java.util.Collections;
import java.util.Map;

public interface StoreTestTarget {

    public static final StoreTestTarget H2 = new H2();
    public static final StoreTestTarget SQLITE = new Sqlite();

    public String factoryPid();

    public Map<String, Object> getConfigurationForPid(final String pid);

    public class H2 implements StoreTestTarget {

        @Override
        public String factoryPid() {
            return "org.eclipse.kura.core.db.H2DbService";
        }

        @Override
        public Map<String, Object> getConfigurationForPid(String pid) {

            return Collections.singletonMap("db.connector.url", "jdbc:h2:mem:testdb-" + pid);
        }

        @Override
        public String toString() {
            return "H2";
        }
    }

    public class Sqlite implements StoreTestTarget {

        @Override
        public String factoryPid() {
            return "org.eclipse.kura.internal.db.sqlite.provider.SqliteDbServiceImpl";
        }

        @Override
        public String toString() {
            return "SQLite";
        }

        @Override
        public Map<String, Object> getConfigurationForPid(String pid) {

            return Collections.emptyMap();
        }
    }
}
