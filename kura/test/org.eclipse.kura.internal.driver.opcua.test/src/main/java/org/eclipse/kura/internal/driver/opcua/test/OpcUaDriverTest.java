/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.driver.opcua.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.channel.ChannelFlag;
import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.Driver.ConnectionException;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.IntegerValue;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfigBuilder;
import org.eclipse.milo.opcua.stack.core.application.CertificateManager;
import org.eclipse.milo.opcua.stack.core.application.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.application.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.application.DefaultCertificateValidator;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.structured.ResponseHeader;
import org.eclipse.milo.opcua.stack.core.types.structured.TestStackRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.TestStackResponse;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class OpcUaDriverTest {

    private static CountDownLatch dependencyLatch = new CountDownLatch(1);

    private static ConfigurationService cfgsvc;

    private static Driver driver;
    private Object driverLock = new Object();

    private static OpcUaServer server;

    @BeforeClass
    public static void setup() throws KuraException {
        startServer();

        try {
            boolean ok = dependencyLatch.await(10, TimeUnit.SECONDS);
            assertTrue(ok);
        } catch (InterruptedException e) {
            fail("Dependencies should have been injected.");
        }
    }

    private static void startServer() {
        CertificateManager certificateManager = new DefaultCertificateManager();
        CertificateValidator certificateValidator = new DefaultCertificateValidator(new File("/tmp"));
        List<String> bindAddresses = new ArrayList<>();
        bindAddresses.add("0.0.0.0");
        OpcUaServerConfig config = new OpcUaServerConfigBuilder().setBindPort(12685).setApplicationUri("opcsvr")
                .setBindAddresses(bindAddresses).setServerName("opcsvr")
                .setApplicationName(LocalizedText.english("opcsvr")).setCertificateManager(certificateManager)
                .setCertificateValidator(certificateValidator)
                .setUserTokenPolicies(Arrays.asList(OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS)).build();
        server = new OpcUaServer(config);
        server.startup();

        server.getServer().addRequestHandler(TestStackRequest.class, service -> {
            TestStackRequest request = service.getRequest();

            ResponseHeader header = service.createResponseHeader();

            service.setResponse(new TestStackResponse(header, request.getInput()));
        });
    }

    @AfterClass
    public static void tearDown() {
        if (server != null) {
            server.shutdown();
        }
    }

    @Before
    public void init() throws InterruptedException {
        if (driver == null) {
            synchronized (driverLock) {
                driverLock.wait(3000);
            }
        }
    }

    protected void activate() throws KuraException {
        if (cfgsvc != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("endpoint.ip", "localhost");
            props.put("endpoint.port", 12685);
            props.put("server.name", "opcsvr");

            props.put("security.policy", "http://opcfoundation.org/UA/SecurityPolicy#None");

            props.put("application.uri", "opcsvr");
            props.put("request.timeout", 1500);
            props.put("session.timeout", 2000);

            props.put("keystore.client.alias", "");
            props.put("keystore.password", "");
            props.put("keystore.server.alias", "");
            props.put("keystore.type", "");
            props.put("certificate.location", "");

            cfgsvc.createFactoryConfiguration("org.eclipse.kura.driver.opcua", "opcuadriver", props, false);
        }
    }

    @Test
    public void testSvcs() {
        assertNotNull(cfgsvc);
        assertNotNull(driver);
    }

    @Test
    public void testConnect() throws ConnectionException {
        // test connection to the server; it is OK if no exception is thrown

        driver.connect();
    }

    @Test
    public void testRead() throws ConnectionException {
        List<ChannelRecord> records = new ArrayList<>();

        ChannelRecord record = ChannelRecord.createReadRecord("ch1", DataType.INTEGER);
        Map<String, Object> channelConfig = new HashMap<>();
        channelConfig.put("node.namespace.index", "1");
        channelConfig.put("node.id.type", "NUMERIC");
        channelConfig.put("node.id", "1");
        record.setChannelConfig(channelConfig);
        records.add(record);

        driver.read(records);

        assertEquals(ChannelFlag.FAILURE, record.getChannelStatus().getChannelFlag()); // read fails - no server config
    }

    @Test
    public void testWrite() throws ConnectionException {
        List<ChannelRecord> records = new ArrayList<>();

        IntegerValue value = new IntegerValue(10);
        ChannelRecord record = ChannelRecord.createWriteRecord("ch1", value);
        Map<String, Object> channelConfig = new HashMap<>();
        channelConfig.put("node.namespace.index", "1");
        channelConfig.put("node.id.type", "NUMERIC");
        channelConfig.put("node.id", "1");
        record.setChannelConfig(channelConfig);
        records.add(record);

        driver.write(records);

        assertEquals(ChannelFlag.FAILURE, record.getChannelStatus().getChannelFlag()); // read fails - no server config
    }

    public void bindCfgSvc(ConfigurationService cfgSvc) {
        OpcUaDriverTest.cfgsvc = cfgSvc;
        dependencyLatch.countDown();
    }

    public void unbindCfgSvc(ConfigurationService cfgSvc) {
        OpcUaDriverTest.cfgsvc = null;
    }

    public void bindDriver(Driver driver) {
        OpcUaDriverTest.driver = driver;

        synchronized (driverLock) {
            driverLock.notifyAll();
        }
    }

    public void unbindDriver(Driver driver) {
        OpcUaDriverTest.driver = null;
    }

}
