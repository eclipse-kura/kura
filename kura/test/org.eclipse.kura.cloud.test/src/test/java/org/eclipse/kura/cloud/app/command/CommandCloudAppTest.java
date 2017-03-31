/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.cloud.app.command;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.CloudClient;
import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.junit.Test;
import org.osgi.service.component.ComponentException;

public class CommandCloudAppTest {

    @Test
    public void testUpdatedDecryptException() throws KuraException, NoSuchFieldException {
        String passKey = "command.password.value";
        char[] password = "encpass".toCharArray();

        CommandCloudApp cca = new CommandCloudApp();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(passKey, new String(password));
        properties.put("key", "value");
        properties.put("command.enable", false);

        cca.updated(properties);

        Map<String, Object> props = (Map<String, Object>) TestUtil.getFieldValue(cca, "properties");

        assertEquals(3, props.size());
        assertEquals("value", props.get("key"));
        assertEquals(false, props.get("command.enable"));
        assertTrue(props.get(passKey) instanceof Password);
        assertArrayEquals(password, ((Password) props.get(passKey)).getPassword());
    }

    @Test
    public void testUpdatedSuperActivateExc() throws KuraException, NoSuchFieldException {
        String passKey = "command.password.value";
        char[] password = "encpass".toCharArray();
        char[] decpass = "decpass".toCharArray();

        CryptoService csMock = mock(CryptoService.class);
        when(csMock.decryptAes(password)).thenReturn(decpass);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(passKey, new String(password));
        properties.put("key", "value");
        properties.put("command.enable", true);

        CloudService clsMock = mock(CloudService.class);
        when(clsMock.newCloudClient(anyString())).thenThrow(new KuraException(KuraErrorCode.CLOSED_DEVICE));

        CommandCloudApp cca = new CommandCloudApp();
        cca.setCryptoService(csMock);
        TestUtil.setFieldValue(cca, "m_cloudService", clsMock);

        try {
            cca.updated(properties);
        } catch (ComponentException e) {
        }

        Map<String, Object> props = (Map<String, Object>) TestUtil.getFieldValue(cca, "properties");

        assertEquals(3, props.size());
        assertEquals("value", props.get("key"));
        assertEquals(true, props.get("command.enable"));
        assertTrue(props.get(passKey) instanceof Password);
        assertArrayEquals(decpass, ((Password) props.get(passKey)).getPassword());
        assertTrue((boolean) TestUtil.getFieldValue(cca, "currentStatus"));
    }

    @Test
    public void testUpdatedSuperActivate() throws KuraException, NoSuchFieldException {
        String passKey = "command.password.value";
        char[] password = "encpass".toCharArray();
        char[] decpass = "decpass".toCharArray();

        CryptoService csMock = mock(CryptoService.class);
        when(csMock.decryptAes(password)).thenReturn(decpass);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(passKey, new String(password));
        properties.put("key", "value");
        properties.put("command.enable", true);

        CloudClient ccMock = mock(CloudClient.class);

        CloudService clsMock = mock(CloudService.class);
        when(clsMock.newCloudClient(anyString())).thenReturn(ccMock);

        CommandCloudApp cca = new CommandCloudApp();
        cca.setCryptoService(csMock);
        TestUtil.setFieldValue(cca, "m_cloudService", clsMock);

        try {
            cca.updated(properties);
        } catch (ComponentException e) {
        }

        Map<String, Object> props = (Map<String, Object>) TestUtil.getFieldValue(cca, "properties");

        assertEquals(3, props.size());
        assertEquals("value", props.get("key"));
        assertEquals(true, props.get("command.enable"));
        assertTrue(props.get(passKey) instanceof Password);
        assertArrayEquals(decpass, ((Password) props.get(passKey)).getPassword());
        assertTrue((boolean) TestUtil.getFieldValue(cca, "currentStatus"));
    }

    @Test
    public void testUpdatedSuperDeactivate() throws KuraException, NoSuchFieldException {
        String passKey = "command.password.value";
        char[] password = "encpass".toCharArray();
        char[] decpass = "decpass".toCharArray();

        CryptoService csMock = mock(CryptoService.class);
        when(csMock.decryptAes(password)).thenReturn(decpass);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(passKey, new String(password));
        properties.put("key", "value");
        properties.put("command.enable", false);

        CloudClient ccMock = mock(CloudClient.class);

        CommandCloudApp cca = new CommandCloudApp();
        cca.setCryptoService(csMock);
        TestUtil.setFieldValue(cca, "m_cloudClient", ccMock);
        TestUtil.setFieldValue(cca, "currentStatus", true);

        cca.updated(properties);

        Map<String, Object> props = (Map<String, Object>) TestUtil.getFieldValue(cca, "properties");

        assertEquals(3, props.size());
        assertEquals("value", props.get("key"));
        assertEquals(false, props.get("command.enable"));
        assertTrue(props.get(passKey) instanceof Password);
        assertArrayEquals(decpass, ((Password) props.get(passKey)).getPassword());

        verify(ccMock, times(1)).release();
    }

    @Test
    public void testGetDefaultWorkDirSystem() throws Throwable {
        Map<String, Object> properties = new HashMap<String, Object>();

        CommandCloudApp cca = new CommandCloudApp();
        TestUtil.setFieldValue(cca, "properties", properties);

        Object result = TestUtil.invokePrivate(cca, "getDefaultWorkDir", null);

        assertEquals(System.getProperty("java.io.tmpdir"), result);
    }

    @Test
    public void testGetDefaultWorkDirConfigured() throws Throwable {
        Map<String, Object> properties = new HashMap<String, Object>();
        String wd = "/wd";
        properties.put("command.working.directory", wd);

        CommandCloudApp cca = new CommandCloudApp();
        TestUtil.setFieldValue(cca, "properties", properties);

        Object result = TestUtil.invokePrivate(cca, "getDefaultWorkDir", null);

        assertEquals(wd, result);
    }

    @Test
    public void testGetDefaultTimeout() throws Throwable {
        Map<String, Object> properties = new HashMap<String, Object>();
        int timeout = 10;
        properties.put("command.timeout", timeout);

        CommandCloudApp cca = new CommandCloudApp();
        TestUtil.setFieldValue(cca, "properties", properties);

        Object result = TestUtil.invokePrivate(cca, "getDefaultTimeout", null);

        assertEquals(timeout, result);
    }

    @Test
    public void testGetTimeoutDefault() throws Throwable {
        Map<String, Object> properties = new HashMap<String, Object>();
        int timeout = 10;
        properties.put("command.timeout", timeout);

        CommandCloudApp cca = new CommandCloudApp();
        TestUtil.setFieldValue(cca, "properties", properties);

        KuraCommandRequestPayload payload = new KuraCommandRequestPayload("cmd");
        Object result = TestUtil.invokePrivate(cca, "getTimeout", payload);

        assertEquals(timeout, result);
    }

    @Test
    public void testGetTimeoutFromPayload() throws Throwable {
        Map<String, Object> properties = new HashMap<String, Object>();
        int timeout = 10;
        properties.put("command.timeout", timeout);

        CommandCloudApp cca = new CommandCloudApp();
        TestUtil.setFieldValue(cca, "properties", properties);

        KuraCommandRequestPayload payload = new KuraCommandRequestPayload("cmd");
        payload.setTimeout(15);
        Object result = TestUtil.invokePrivate(cca, "getTimeout", payload);

        assertEquals(15, result);
    }

    @Test
    public void testGetDefaultEnvironmentNull() throws Throwable {
        Map<String, Object> properties = new HashMap<String, Object>();

        CommandCloudApp cca = new CommandCloudApp();
        TestUtil.setFieldValue(cca, "properties", properties);

        Object result = TestUtil.invokePrivate(cca, "getDefaultEnvironment", null);

        assertNull(result);
    }

    @Test
    public void testGetDefaultEnvironment() throws Throwable {
        Map<String, Object> properties = new HashMap<String, Object>();
        String env = "e1 e2 e3";
        properties.put("command.environment", env);

        CommandCloudApp cca = new CommandCloudApp();
        TestUtil.setFieldValue(cca, "properties", properties);

        Object[] result = (Object[]) TestUtil.invokePrivate(cca, "getDefaultEnvironment", null);

        assertArrayEquals(new Object[] { "e1", "e2", "e3" }, result);
    }

    @Test
    public void testGetEnvironmentDefault() throws Throwable {
        Map<String, Object> properties = new HashMap<String, Object>();
        String env = "e1 e2 e3";
        properties.put("command.environment", env);

        CommandCloudApp cca = new CommandCloudApp();
        TestUtil.setFieldValue(cca, "properties", properties);

        KuraCommandRequestPayload payload = new KuraCommandRequestPayload("cmd");
        Object[] result = (Object[]) TestUtil.invokePrivate(cca, "getEnvironment", payload);

        assertArrayEquals(new Object[] { "e1", "e2", "e3" }, result);
    }

    @Test
    public void testGetEnvironmentFromPayload() throws Throwable {
        Map<String, Object> properties = new HashMap<String, Object>();
        String env = "e1 e2 e3";
        properties.put("command.environment", env);

        CommandCloudApp cca = new CommandCloudApp();
        TestUtil.setFieldValue(cca, "properties", properties);

        KuraCommandRequestPayload payload = new KuraCommandRequestPayload("cmd");
        String[] environmentPairs = new String[] { "env1", "env2" };
        payload.setEnvironmentPairs(environmentPairs);
        Object[] result = (Object[]) TestUtil.invokePrivate(cca, "getEnvironment", payload);

        assertArrayEquals(environmentPairs, result);
    }

    @Test
    public void testVerifyPasswords() throws Throwable {
        CommandCloudApp cca = new CommandCloudApp();

        Password commandPassword = null;
        String receivedPassword = null;
        boolean result = (boolean) TestUtil.invokePrivate(cca, "verifyPasswords", commandPassword, receivedPassword);
        assertTrue(result);

        commandPassword = null;
        receivedPassword = "";
        result = (boolean) TestUtil.invokePrivate(cca, "verifyPasswords", commandPassword, receivedPassword);
        assertTrue(result);

        commandPassword = null;
        receivedPassword = "foo";
        result = (boolean) TestUtil.invokePrivate(cca, "verifyPasswords", commandPassword, receivedPassword);
        assertFalse(result);

        commandPassword = new Password("");
        receivedPassword = null;
        result = (boolean) TestUtil.invokePrivate(cca, "verifyPasswords", commandPassword, receivedPassword);
        assertTrue(result);

        commandPassword = new Password("");
        receivedPassword = "bar";
        result = (boolean) TestUtil.invokePrivate(cca, "verifyPasswords", commandPassword, receivedPassword);
        assertFalse(result);

        commandPassword = new Password("");
        receivedPassword = "";
        result = (boolean) TestUtil.invokePrivate(cca, "verifyPasswords", commandPassword, receivedPassword);
        assertTrue(result);

        commandPassword = new Password("foo");
        receivedPassword = "foo";
        result = (boolean) TestUtil.invokePrivate(cca, "verifyPasswords", commandPassword, receivedPassword);
        assertTrue(result);

        commandPassword = new Password("foo");
        receivedPassword = "bar";
        result = (boolean) TestUtil.invokePrivate(cca, "verifyPasswords", commandPassword, receivedPassword);
        assertFalse(result);
    }

    @Test
    public void testPrepareesponseNoTimeoutNoException() throws Throwable {
        String err = "err";
        String out = "out";
        int exitVal = 10;

        KuraCommandResponsePayload resp = new KuraCommandResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);
        ProcessMonitorThread pmtMock = mock(ProcessMonitorThread.class);
        when(pmtMock.getException()).thenReturn(null);
        when(pmtMock.isTimedOut()).thenReturn(false);
        when(pmtMock.getExitValue()).thenReturn(exitVal);
        when(pmtMock.getStderr()).thenReturn(err);
        when(pmtMock.getStdout()).thenReturn(out);

        CommandCloudApp cca = new CommandCloudApp();

        String[] result = (String[]) TestUtil.invokePrivate(cca, "prepareResponseNoTimeout", resp, pmtMock);

        assertEquals("Response expected not to be changed", KuraResponsePayload.RESPONSE_CODE_OK,
                resp.getResponseCode());
        assertEquals(err, resp.getStderr());
        assertEquals(out, resp.getStdout());
        assertFalse(resp.isTimedout());
        assertEquals(exitVal, resp.getExitCode().intValue());
    }

    @Test
    public void testPrepareesponseNoTimeoutExecption() throws Throwable {
        String err = "err";
        String out = "out";
        int exitVal = 10;
        Exception exc = new Exception("test");

        KuraCommandResponsePayload resp = new KuraCommandResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);
        ProcessMonitorThread pmtMock = mock(ProcessMonitorThread.class);
        when(pmtMock.getException()).thenReturn(exc);
        when(pmtMock.isTimedOut()).thenReturn(false);
        when(pmtMock.getExitValue()).thenReturn(exitVal);
        when(pmtMock.getStderr()).thenReturn(err);
        when(pmtMock.getStdout()).thenReturn(out);

        CommandCloudApp cca = new CommandCloudApp();

        String[] result = (String[]) TestUtil.invokePrivate(cca, "prepareResponseNoTimeout", resp, pmtMock);

        assertEquals("Response expected to be changed", KuraResponsePayload.RESPONSE_CODE_ERROR,
                resp.getResponseCode());
        assertEquals(err, resp.getStderr());
        assertEquals(out, resp.getStdout());
        assertEquals("test", resp.getExceptionMessage());
    }

    @Test
    public void testPrepareTimeoutResponse() throws Throwable {
        String err = "err";
        String out = "out";

        KuraCommandResponsePayload resp = new KuraCommandResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);
        ProcessMonitorThread pmtMock = mock(ProcessMonitorThread.class);
        when(pmtMock.getStderr()).thenReturn(err);
        when(pmtMock.getStdout()).thenReturn(out);

        CommandCloudApp cca = new CommandCloudApp();

        String[] result = (String[]) TestUtil.invokePrivate(cca, "prepareTimeoutResponse", resp, pmtMock);

        assertEquals("Response expected not to be changed", KuraResponsePayload.RESPONSE_CODE_OK,
                resp.getResponseCode());
        assertEquals(err, resp.getStderr());
        assertEquals(out, resp.getStdout());
        assertTrue(resp.isTimedout());
    }

    @Test
    public void testPrepareCommandArray() throws Throwable {
        CommandCloudApp cca = new CommandCloudApp();

        String cmd = "cmd";
        KuraCommandRequestPayload payload = new KuraCommandRequestPayload(cmd);
        String[] arguments = new String[] { "arg1", "arg2" };
        payload.setArguments(arguments);
        String[] result = (String[]) TestUtil.invokePrivate(cca, "prepareCommandArray", payload, cmd);

        assertArrayEquals(new String[] { "cmd", "arg1", "arg2" }, result);
    }

    @Test
    public void testDoExecNoResources() throws Throwable {
        CommandCloudApp cca = new CommandCloudApp();

        String appTopic = "EXEC";
        CloudletTopic reqTopic = CloudletTopic.parseAppTopic(appTopic);
        KuraRequestPayload reqPayload = null;
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_ERROR);

        cca.doExec(reqTopic, reqPayload, respPayload);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST, respPayload.getResponseCode());
    }

    @Test
    public void testDoExecTooManyResources() throws Throwable {
        CommandCloudApp cca = new CommandCloudApp();

        String appTopic = "EXEC/command/test";
        CloudletTopic reqTopic = CloudletTopic.parseAppTopic(appTopic);
        KuraRequestPayload reqPayload = null;
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_ERROR);

        cca.doExec(reqTopic, reqPayload, respPayload);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST, respPayload.getResponseCode());
    }

    @Test
    public void testDoExecWrongCommand() throws Throwable {
        CommandCloudApp cca = new CommandCloudApp();

        String appTopic = "EXEC/cmd";
        CloudletTopic reqTopic = CloudletTopic.parseAppTopic(appTopic);
        KuraRequestPayload reqPayload = null;
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_ERROR);

        cca.doExec(reqTopic, reqPayload, respPayload);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_NOTFOUND, respPayload.getResponseCode());
    }

    @Test
    public void testDoExec() throws Throwable {
        KuraCommandResponsePayload kcrp = new KuraCommandResponsePayload(KuraResponsePayload.RESPONSE_CODE_OK);
        byte[] body = "testbody".getBytes();
        kcrp.setBody(body);

        CommandCloudApp cca = new CommandCloudApp() {

            @Override
            public KuraCommandResponsePayload execute(KuraRequestPayload reqPayload) {
                return kcrp;
            }
        };

        String appTopic = "EXEC/command";
        CloudletTopic reqTopic = CloudletTopic.parseAppTopic(appTopic);
        KuraRequestPayload reqPayload = null;
        KuraResponsePayload respPayload = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_ERROR);

        cca.doExec(reqTopic, reqPayload, respPayload);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, respPayload.getResponseCode());
        assertArrayEquals(body, respPayload.getBody());
    }

    @Test
    public void testExecuteDisabled() throws KuraException {
        CommandCloudApp cca = new CommandCloudApp();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("command.enable", false);

        cca.updated(properties);

        String cmd = "command";
        String pass = "pass";

        try {
            cca.execute(cmd, pass);
            fail("Exception expected - command execution not allowed");
        } catch (KuraException e) {
            assertEquals(KuraErrorCode.OPERATION_NOT_SUPPORTED, e.getCode());
        }
    }

    @Test
    public void testExecute() throws KuraException, IOException {
        String wd = "/tmp";
        String cmd = wd + "/command";
        String pass = "pass";

        if (System.getProperty("os.name").contains("Windows")) {
            cmd += ".bat";
        } else {
            cmd += ".sh";
        }

        CommandCloudApp cca = new CommandCloudApp();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("command.password.value", "pass");
        properties.put("command.enable", true);
        properties.put("command.working.directory", wd);
        properties.put("command.timeout", 10);

        try {
            cca.updated(properties);
        } catch (Exception e) {
            // ignore the expected exception
        }

        File f = new File(cmd);
        f.createNewFile();
        f.setExecutable(true);
        f.deleteOnExit();

        FileWriter fw = new FileWriter(f);
        fw.write("sleep 1\necho OK");
        fw.close();

        String out = cca.execute(cmd, pass);

        assertTrue(out.trim().endsWith("OK"));
    }

    @Test
    public void testExecuteTimeout() throws KuraException, IOException {
        String wd = "/tmp";
        String cmd = wd + "/command_timeout";
        String pass = "pass";

        if (System.getProperty("os.name").contains("Windows")) {
            cmd += ".bat";
        } else {
            cmd += ".sh";
        }

        CommandCloudApp cca = new CommandCloudApp();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("command.password.value", "pass");
        properties.put("command.enable", true);
        properties.put("command.working.directory", wd);
        properties.put("command.timeout", 1);

        try {
            cca.updated(properties);
        } catch (Exception e) {
            // ignore the expected exception
        }

        File f = new File(cmd);
        f.createNewFile();
        f.setExecutable(true);
        f.deleteOnExit();

        FileWriter fw = new FileWriter(f);
        fw.write("echo NOK 1>&2\nsleep 2\necho OK");
        fw.close();

        String out = cca.execute(cmd, pass);

        assertEquals("NOK", out.trim());
    }

    @Test
    public void testExecutePayloadNoCommand() throws KuraException, IOException {
        String wd = "/tmp";

        CommandCloudApp cca = new CommandCloudApp();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("command.password.value", "pass");
        properties.put("command.enable", true);
        properties.put("command.working.directory", wd);
        properties.put("command.timeout", 1);

        try {
            cca.updated(properties);
        } catch (Exception e) {
            // ignore the expected exception
        }

        KuraRequestPayload payload = new KuraRequestPayload();
        payload.addMetric("command.password", "pass");
        payload.addMetric("command.command", "");

        KuraCommandResponsePayload response = cca.execute(payload);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST, response.getResponseCode());
    }

    @Test
    public void testExecutePayloadPasswordError() throws KuraException, IOException {
        String wd = "/tmp";

        CommandCloudApp cca = new CommandCloudApp();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("command.password.value", "pass");
        properties.put("command.enable", true);
        properties.put("command.working.directory", wd);

        try {
            cca.updated(properties);
        } catch (Exception e) {
            // ignore the expected exception
        }

        KuraRequestPayload payload = new KuraRequestPayload();
        payload.addMetric("command.password", "pss");
        payload.addMetric("command.command", "sleep");

        KuraCommandResponsePayload response = cca.execute(payload);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_ERROR, response.getResponseCode());
        assertEquals("Password missing or not correct", response.getExceptionMessage());
    }

    @Test
    public void testExecutePayload() throws KuraException, IOException {
        String wd = "/tmp";
        String cmd = wd + "/command_payload";
        String pass = "pass";

        if (System.getProperty("os.name").contains("Windows")) {
            cmd += ".bat";
        } else {
            cmd += ".sh";
        }

        CommandCloudApp cca = new CommandCloudApp();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("command.password.value", "pass");
        properties.put("command.enable", true);
        properties.put("command.working.directory", wd);
        properties.put("command.timeout", 2);

        try {
            cca.updated(properties);
        } catch (Exception e) {
            // ignore the expected exception
        }

        File f = new File(cmd);
        f.createNewFile();
        f.setExecutable(true);
        f.deleteOnExit();

        FileWriter fw = new FileWriter(f);
        fw.write("sleep 1\necho OK");
        fw.close();

        KuraRequestPayload payload = new KuraRequestPayload();
        payload.addMetric("command.password", "pass");
        payload.addMetric("command.command", cmd);
        payload.setBody("zip".getBytes()); // try unzip for good measure...

        KuraCommandResponsePayload response = cca.execute(payload);

        assertEquals(KuraResponsePayload.RESPONSE_CODE_OK, response.getResponseCode());
        assertTrue(response.getStdout().trim().endsWith("OK"));
    }
}