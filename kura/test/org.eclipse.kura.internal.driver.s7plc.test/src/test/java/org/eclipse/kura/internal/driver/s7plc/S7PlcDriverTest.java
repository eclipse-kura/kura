/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.internal.driver.s7plc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.block.BlockFactory;
import org.eclipse.kura.driver.block.task.BlockTask;
import org.eclipse.kura.driver.block.task.Mode;
import org.eclipse.kura.driver.block.task.ToplevelBlockTask;
import org.junit.Test;

import Moka7.S7;
import Moka7.S7Client;

public class S7PlcDriverTest {

    private static final String CLIENT_FIELD = "client";

    @Test
    public void testActivate() {
        // test that properties should not be null, but may be empty

        S7PlcDriver svc = new S7PlcDriver();

        try {
            svc.activate(null, null);
            fail("Exception was expected.");
        } catch (NullPointerException e) {
            // OK
        }

        svc.activate(null, new HashMap<String, Object>());
    }

    @Test
    public void testDeactivate() throws NoSuchFieldException {
        // check that deactivate tries to disconnect the client

        S7PlcDriver svc = new S7PlcDriver();

        S7Client s7Mock = mock(S7Client.class);
        TestUtil.setFieldValue(svc, CLIENT_FIELD, s7Mock);

        // deactivate a disconnected driver
        svc.deactivate(null);

        verify(s7Mock, times(0)).Disconnect();

        // deactivate a connected driver
        s7Mock.Connected = true;

        svc.deactivate(null);

        verify(s7Mock, times(1)).Disconnect();
    }

    @Test
    public void testUpdatedWithConnectionFailure() throws NoSuchFieldException {
        // update causes disconnect and reconnect of a connected client; test reconnection exception

        String address = "127.0.0.1";
        int rack = 1;
        int slot = 1;

        S7PlcDriver svc = new S7PlcDriver();

        S7Client s7Mock = mock(S7Client.class);
        TestUtil.setFieldValue(svc, CLIENT_FIELD, s7Mock);

        doAnswer(invocation -> {
            s7Mock.Connected = false; // set it to false so that connection attempt can be made later on

            return null;
        }).when(s7Mock).Disconnect();

        when(s7Mock.ConnectTo(address, rack, slot)).thenReturn(1234); // cause exception

        s7Mock.Connected = true;

        Map<String, Object> properties = new HashMap<>();
        properties.put("host.ip", address);
        properties.put("rack", rack);
        properties.put("slot", slot);

        svc.updated(properties);

        assertNotNull(TestUtil.getFieldValue(svc, "options"));

        verify(s7Mock, times(1)).Disconnect();
        verify(s7Mock, times(1)).ConnectTo(address, rack, slot);
    }

    @Test
    public void testUpdated() throws NoSuchFieldException {
        // update causes disconnect and reconnect of a connected client; test normal reconnect

        String address = "127.0.0.1";
        int rack = 1;
        int slot = 1;

        S7PlcDriver svc = new S7PlcDriver();

        S7Client s7Mock = mock(S7Client.class);
        TestUtil.setFieldValue(svc, CLIENT_FIELD, s7Mock);

        doAnswer(invocation -> {
            s7Mock.Connected = false; // set it to false so that connection attempt can be made later on

            return null;
        }).when(s7Mock).Disconnect();

        when(s7Mock.ConnectTo(address, rack, slot)).thenReturn(0);

        s7Mock.Connected = true;

        Map<String, Object> properties = new HashMap<>();
        properties.put("host.ip", address);
        properties.put("rack", rack);
        properties.put("slot", slot);

        svc.updated(properties);

        assertNotNull(TestUtil.getFieldValue(svc, "options"));

        verify(s7Mock, times(1)).Disconnect();
        verify(s7Mock, times(1)).ConnectTo(address, rack, slot);
    }

    @Test(expected = Exception.class)
    public void testAuthenticateExceptionWithPassword() throws Throwable {
        // check that an exception is thrown in case password decryption fails

        S7PlcDriver svc = new S7PlcDriver();

        String pass = "pass";
        String dec = "dec";

        S7Client s7Mock = mock(S7Client.class);
        TestUtil.setFieldValue(svc, CLIENT_FIELD, s7Mock);

        CryptoService csMock = mock(CryptoService.class);
        svc.setCryptoService(csMock);

        when(csMock.decryptAes(pass.toCharArray()))
                .thenThrow(new KuraException(KuraErrorCode.CONFIGURATION_ERROR, "test"));

        Map<String, Object> properties = new HashMap<>();
        properties.put("password", pass);
        svc.updated(properties);

        TestUtil.invokePrivate(svc, "authenticate");
    }

    @Test(expected = Exception.class)
    public void testAuthenticateWithException() throws Throwable {
        // check that an exception is thrown when session password fails to be set in the client

        S7PlcDriver svc = new S7PlcDriver();

        String pass = "pass";
        String dec = "dec";

        S7Client s7Mock = mock(S7Client.class);
        TestUtil.setFieldValue(svc, CLIENT_FIELD, s7Mock);

        when(s7Mock.SetSessionPassword(dec)).thenReturn(123);

        CryptoService csMock = mock(CryptoService.class);
        svc.setCryptoService(csMock);

        when(csMock.decryptAes(pass.toCharArray())).thenReturn(dec.toCharArray());

        Map<String, Object> properties = new HashMap<>();
        properties.put("password", pass);
        svc.updated(properties);

        TestUtil.invokePrivate(svc, "authenticate");
    }

    @Test
    public void testAuthenticate() throws Throwable {
        // test successful authentication

        S7PlcDriver svc = new S7PlcDriver();

        String pass = "pass";
        String dec = "dec";

        S7Client s7Mock = mock(S7Client.class);
        TestUtil.setFieldValue(svc, CLIENT_FIELD, s7Mock);

        when(s7Mock.SetSessionPassword(dec)).thenReturn(0);

        CryptoService csMock = mock(CryptoService.class);
        svc.setCryptoService(csMock);

        when(csMock.decryptAes(pass.toCharArray())).thenReturn(dec.toCharArray());

        Map<String, Object> properties = new HashMap<>();
        properties.put("password", pass);
        svc.updated(properties);

        TestUtil.invokePrivate(svc, "authenticate");
    }

    @Test
    public void testReadMinimumGapSizeForDomain() {
        // test that gap size is returned regardless of the domain, but a default is set

        S7PlcDriver svc = new S7PlcDriver();

        // default value
        Map<String, Object> properties = new HashMap<>();
        svc.updated(properties);

        int size = svc.getReadMinimumGapSizeForDomain(null);

        assertEquals(0, size);

        // set value from options
        int gapSize = 1234;
        properties.put("read.minimum.gap.size", gapSize);
        svc.updated(properties);

        size = svc.getReadMinimumGapSizeForDomain(null);

        assertEquals(gapSize, size);
    }

    @Test
    public void testGetTaskFactoryForDomain() {
        S7PlcDriver svc = new S7PlcDriver();

        S7PlcDomain domain = new S7PlcDomain(3);
        Mode mode = Mode.READ;
        BlockFactory<ToplevelBlockTask> factory = svc.getTaskFactoryForDomain(domain, mode);

        assertNotNull(factory);

        ToplevelBlockTask task = factory.build(1, 2);

        assertEquals(mode, task.getMode());
        assertEquals(1, task.getStart());
        assertEquals(2, task.getEnd());
    }

    @Test
    public void testGetChannelDescriptor() {
        S7PlcDriver svc = new S7PlcDriver();

        ChannelDescriptor descriptor = svc.getChannelDescriptor();

        assertNotNull(descriptor);
    }

    @Test
    public void testRunTask() throws IOException {
        S7PlcDriver svc = new S7PlcDriver();

        BlockTask task = mock(BlockTask.class);

        svc.runTask(task);

        verify(task, times(1)).run();
    }

    @Test
    public void testRunTaskWithGeneralException() throws IOException {
        // test that exceptions are 'handled' during read/write

        S7PlcDriver svc = new S7PlcDriver();

        BlockTask task = mock(BlockTask.class);
        doThrow(new IOException("test")).when(task).run();

        svc.runTask(task); // no exception is expected

        verify(task, times(1)).run();
    }

    @Test
    public void testRunTaskWithReadMoka7Exception() throws IOException, NoSuchFieldException {
        // test what happens if read returns just the right value != 0 and Moka7Exception is thrown

        S7PlcDriver svc = new S7PlcDriver();

        int db = 3;
        int offset = 0;
        byte[] data = new byte[4];

        S7Client s7Mock = mock(S7Client.class);
        TestUtil.setFieldValue(svc, CLIENT_FIELD, s7Mock);

        s7Mock.Connected = true;

        // result != 0 causes exception
        when(s7Mock.ReadArea(S7.S7AreaDB, db, offset, data.length, data)).thenReturn(4);

        BlockTask task = mock(BlockTask.class);
        doAnswer(invocation -> {
            svc.read(db, offset, data);
            return null;
        }).when(task).run();

        svc.runTask(task); // no exception is expected

        verify(task, times(1)).run();
        verify(s7Mock, times(1)).ReadArea(S7.S7AreaDB, db, offset, data.length, data);
        verify(s7Mock, times(1)).Disconnect();
    }

    @Test(expected = IOException.class)
    public void testWrite() throws NoSuchFieldException, IOException {
        // test that exception is thrown as a result of an unsuccessful write

        S7PlcDriver svc = new S7PlcDriver();

        int db = 3;
        int offset = 0;
        byte[] data = new byte[4];

        S7Client s7Mock = mock(S7Client.class);
        TestUtil.setFieldValue(svc, CLIENT_FIELD, s7Mock);

        // result != 0 causes exception
        when(s7Mock.WriteArea(S7.S7AreaDB, db, offset, data.length, data)).thenReturn(3);

        svc.write(db, offset, data);

        verify(s7Mock, times(1)).WriteArea(S7.S7AreaDB, db, offset, data.length, data);
    }
}
