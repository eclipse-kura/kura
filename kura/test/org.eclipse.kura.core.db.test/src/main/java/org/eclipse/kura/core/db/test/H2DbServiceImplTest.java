/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.core.db.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.db.H2DbService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class H2DbServiceImplTest {

    private static ConfigurationService cfgService;
    private static H2DbService dbService;
    private static CryptoService cryptoService;
    private static CountDownLatch dependencyLatch = new CountDownLatch(3);	// initialize with number of dependencies

    private static final String PID = "org.eclipse.kura.db.H2DbService";

    @BeforeClass
    public static void setUp() throws SQLException, ClassNotFoundException {
        Class.forName("org.h2.Driver");

        // Wait for OSGi dependencies
        try {
            boolean ok = dependencyLatch.await(10, TimeUnit.SECONDS);
            assertTrue(ok);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("OSGi dependencies unfulfilled");
        }
    }

    @Before
    public void init() throws KuraException {
        // reset the url and password

        ComponentConfiguration componentConfiguration = cfgService.getComponentConfiguration(PID);

        Map<String, Object> properties = componentConfiguration.getConfigurationProperties();
        properties.put("db.password", "");
        properties.put("db.connector.url", "jdbc:h2:mem:kuradb");

        cfgService.updateConfiguration(PID, properties);
    }

    protected void bindCfgSvc(ConfigurationService cfgSvc) {
        cfgService = cfgSvc;
        dependencyLatch.countDown();
    }

    protected void unbindCfgSvc(ConfigurationService cfgSvc) {
        cfgService = null;
    }

    protected void bindCryptoSvc(CryptoService cSvc) {
        cryptoService = cSvc;
        dependencyLatch.countDown();
    }

    protected void unbindCryptoSvc(CryptoService cSvc) {
        cryptoService = null;
    }

    protected void bindDbSvc(H2DbService dbSvc) {
        dbService = dbSvc;
        dependencyLatch.countDown();
    }

    protected void unbindDbSvc(H2DbService dbSvc) {
        dbService = null;
    }

    @Test
    public void testSvcs() {
        assertNotNull(cfgService);
        assertNotNull(cryptoService);
        assertNotNull(dbService);
    }

    @Test
    public void testPasswordUpdate() throws Exception {
        // connect to DB, update password and check that connection only succeeds with the new password

        final String url = "jdbc:h2:mem:kuradb";
        final String user = "SA";
        String pass = "";
        String newPass = "newPass";

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, pass);
        } catch (Exception e) {
            e.printStackTrace();

            pass = newPass;
            newPass = "";

            conn = DriverManager.getConnection(url, user, pass);
        }

        assertNotNull(conn);

        verifyUserAndClose(user, conn);

        char[] encPass = cryptoService.encryptAes(newPass.toCharArray());

        String pid = "org.eclipse.kura.db.H2DbService";

        ComponentConfiguration componentConfiguration = cfgService.getComponentConfiguration(pid);
        Map<String, Object> properties = componentConfiguration.getConfigurationProperties();
        properties.put("db.password", new String(encPass));

        cfgService.updateConfiguration(pid, properties);

        try {
            Thread.sleep(100); // wait a bit, just in case
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        conn = null;
        try {
            conn = DriverManager.getConnection(url, user, pass);
            fail("Exception expected - password was changed.");
        } catch (Exception e) {
            conn = DriverManager.getConnection(url, user, newPass);
        }

        assertNotNull(conn);

        verifyUserAndClose(user, conn);
    }

    private void verifyUserAndClose(final String user, Connection conn) throws SQLException {
        try (PreparedStatement statement = conn.prepareStatement("SELECT USER()");
                ResultSet rs = statement.executeQuery();) {

            rs.next();
            String result = rs.getString(1);

            assertEquals(user, result);

            conn.close();
        }
    }

    @Test
    public void testUrlUpdate() throws Exception {
        // connect to DB, update url to file protocol and check that connection succeeds with the new url

        final String url = "jdbc:h2:mem:kuradb";
        File f = new File("/tmp/kuradb");
        final String newUrl = "jdbc:h2:file:" + f.getAbsolutePath(); // on windows /tmp/kuradb is not absolute!
        final String user = "SA";
        String pass = "";

        Connection conn = null;
        conn = DriverManager.getConnection(url, user, pass);
        assertNotNull(conn);

        verifyUserAndClose(user, conn);

        ComponentConfiguration componentConfiguration = cfgService.getComponentConfiguration(PID);
        Map<String, Object> properties = componentConfiguration.getConfigurationProperties();
        properties.put("db.connector.url", newUrl);

        cfgService.updateConfiguration(PID, properties);

        try {
            Thread.sleep(100); // wait a bit, just in case
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        conn = DriverManager.getConnection(newUrl, user, pass);

        assertNotNull(conn);

        verifyUserAndClose(user, conn);
    }

}
