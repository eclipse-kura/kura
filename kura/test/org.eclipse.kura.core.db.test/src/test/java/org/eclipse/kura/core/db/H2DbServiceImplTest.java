/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.junit.Test;

public class H2DbServiceImplTest {

    @Test
    public void testUpdate() throws KuraException, SQLException {
        final String enc = "enc";
        char[] encPass = enc.toCharArray();
        String pass = "pass";
        String user = "USR";

        H2DbServiceImpl svc = new H2DbServiceImpl();
        svc.activate(Collections.emptyMap());

        CryptoService csMock = mock(CryptoService.class);
        svc.setCryptoService(csMock);

        when(csMock.decryptAes(encPass)).thenReturn(pass.toCharArray());

        Map<String, Object> props = new HashMap<>();
        props.put("db.user", user);
        props.put("db.password", enc);
        props.put("db.connection.pool.max.size", 10);
        props.put("db.connector.url", "jdbc:h2:mem:testdb");

        try {
            svc.getConnection();
        } catch (SQLException e) {
            assertTrue(e.getMessage().contains("not initialized"));
        }

        svc.updated(props);

        Connection conn = svc.getConnection();
        PreparedStatement statement = conn.prepareStatement("SELECT USER()");

        ResultSet rs = statement.executeQuery();
        rs.next();
        String result = rs.getString(1);

        assertEquals(user, result);

        svc.deactivate();
    }

    @Test
    public void testUpdateFailUrlPattern() throws KuraException, SQLException {
        String pass = "pass";
        String user = "USR";

        H2DbServiceImpl svc = new H2DbServiceImpl();
        svc.activate(Collections.emptyMap());

        Map<String, Object> props = new HashMap<>();
        props.put("db.user", user);
        props.put("db.password", pass);
        props.put("db.connection.pool.max.size", 10);
        props.put("db.connector.url", "jdb:h2:mem:testdb");

        try {
            svc.getConnection();
        } catch (SQLException e) {
            assertTrue(e.getMessage().contains("not initialized"));
        }

        try {
            svc.updated(props);
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid DB URL", e.getMessage());
        }
    }

    @Test
    public void testUpdateFailDriver() throws KuraException, SQLException {
        String pass = "pass";
        String user = "USR";

        H2DbServiceImpl svc = new H2DbServiceImpl();
        svc.activate(Collections.emptyMap());

        Map<String, Object> props = new HashMap<>();
        props.put("db.user", user);
        props.put("db.password", pass);
        props.put("db.connection.pool.max.size", 10);
        props.put("db.connector.url", "jdbc:h3:mem:testdb");

        try {
            svc.getConnection();
        } catch (SQLException e) {
            assertTrue(e.getMessage().contains("not initialized"));
        }

        try {
            svc.updated(props);
        } catch (IllegalArgumentException e) {
            assertEquals("JDBC driver must be h2", e.getMessage());
        }
    }

    @Test
    public void testUpdateFailRemote() throws Throwable {
        final String enc = "enc";
        char[] encPass = enc.toCharArray();
        String pass = "pass";
        String user = "USR";

        H2DbServiceImpl svc = new H2DbServiceImpl();
        svc.activate(Collections.emptyMap());

        Map<String, Object> props = new HashMap<>();
        props.put("db.user", user);
        props.put("db.password", enc);
        props.put("db.connection.pool.max.size", 10);
        props.put("db.connector.url", "jdbc:h2:rmt:test");

        try {
            svc.updated(props);
        } catch (IllegalArgumentException e) {
            assertEquals("Remote databases are not supported", e.getMessage());
        }
    }

    @Test
    public void testUpdateFailAnonymous() throws Throwable {
        final String enc = "enc";
        char[] encPass = enc.toCharArray();
        String pass = "pass";
        String user = "USR";

        H2DbServiceImpl svc = new H2DbServiceImpl();
        svc.activate(Collections.emptyMap());

        Map<String, Object> props = new HashMap<>();
        props.put("db.user", user);
        props.put("db.password", enc);
        props.put("db.connection.pool.max.size", 10);
        props.put("db.connector.url", "jdbc:h2:mem:");

        try {
            svc.updated(props);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Anonymous"));
        }
    }

    @Test
    public void testUpdateFailUrlOccupied() throws Throwable {
        final String enc = "enc";
        char[] encPass = enc.toCharArray();
        String pass = "pass";
        String user = "USR";
        String url = "jdbc:h2:mem:test";

        H2DbServiceImpl svc1 = new H2DbServiceImpl();
        Map<String, H2DbServiceImpl> activeInstances = (Map<String, H2DbServiceImpl>) TestUtil.getFieldValue(svc1,
                "activeInstances");
        activeInstances.put(url, svc1);

        H2DbServiceImpl svc = new H2DbServiceImpl();
        svc.activate(Collections.emptyMap());

        Map<String, Object> props = new HashMap<>();
        props.put("db.user", user);
        props.put("db.password", enc);
        props.put("db.connection.pool.max.size", 10);
        props.put("db.connector.url", url);

        try {
            svc.updated(props);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("Another H2DbService instance"));
        }
    }

    @Test
    public void testUpdateFile() throws Throwable {
        final String enc = "enc";
        char[] encPass = enc.toCharArray();
        String pass = "pass";
        String user = "USR";

        H2DbServiceImpl svc = new H2DbServiceImpl();
        svc.activate(Collections.emptyMap());

        CryptoService csMock = mock(CryptoService.class);
        svc.setCryptoService(csMock);

        when(csMock.decryptAes(encPass)).thenReturn(pass.toCharArray());

        Map<String, Object> props = new HashMap<>();
        props.put("db.user", user);
        props.put("db.password", enc);
        props.put("db.connection.pool.max.size", 10);
        File f = new File("/tmp/kurah2/testdb");
        props.put("db.connector.url", "jdbc:h2:file:" + f.getAbsolutePath());

        try {
            svc.getConnection();
        } catch (SQLException e) {
            assertTrue(e.getMessage().contains("not initialized"));
        }

        svc.updated(props);

        Connection conn = svc.getConnection();
        PreparedStatement statement = conn.prepareStatement("SELECT USER()");

        ResultSet rs = statement.executeQuery();
        rs.next();
        String result = rs.getString(1);

        assertEquals(user, result);

        svc.deactivate();

        // test a method and clean the files
        File[] files = f.getParentFile().listFiles();
        assertEquals(1, files.length);

        H2DbServiceOptions cfg = (H2DbServiceOptions) TestUtil.getFieldValue(svc, "configuration");
        TestUtil.invokePrivate(svc, "deleteDbFiles", cfg);

        for (File file : files) {
            assertFalse(file.exists());
        }
    }

}
