/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.linux.clock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.junit.Test;

public class NtpdClockSyncProviderTest {

    @Test
    public void testSynch() throws KuraException, NoSuchFieldException {
        NtpdClockSyncProvider provider = new NtpdClockSyncProvider() {

            @Override
            protected SafeProcess exec(String command) throws IOException {
                SafeProcess processMock = mock(SafeProcess.class);
                when(processMock.exitValue()).thenReturn(0);
                return processMock;
            }
        };

        AtomicBoolean invoked = new AtomicBoolean(false);
        ClockSyncListener listener = new ClockSyncListener() {

            @Override
            public void onClockUpdate(long offset) {
                assertEquals(0, offset);

                invoked.set(true);
            }
        };
        TestUtil.setFieldValue(provider, "listener", listener);

        boolean synched = provider.syncClock();

        assertTrue(synched);
        assertTrue(invoked.get());
    }

    @Test
    public void testSynchError() throws KuraException, NoSuchFieldException {
        NtpdClockSyncProvider provider = new NtpdClockSyncProvider() {

            @Override
            protected SafeProcess exec(String command) throws IOException {
                SafeProcess processMock = mock(SafeProcess.class);
                when(processMock.exitValue()).thenReturn(1);
                return processMock;
            }
        };

        AtomicBoolean invoked = new AtomicBoolean(false);
        ClockSyncListener listener = new ClockSyncListener() {

            @Override
            public void onClockUpdate(long offset) {
                invoked.set(true);
            }
        };
        TestUtil.setFieldValue(provider, "listener", listener);

        boolean synched = provider.syncClock();

        assertFalse(synched);
        assertFalse(invoked.get());
    }

    @Test(expected = KuraException.class)
    public void testSynchException() throws KuraException, NoSuchFieldException {
        NtpdClockSyncProvider provider = new NtpdClockSyncProvider() {

            @Override
            protected SafeProcess exec(String command) throws IOException {
                throw new IOException("test");
            }
        };

        AtomicBoolean invoked = new AtomicBoolean(false);
        ClockSyncListener listener = new ClockSyncListener() {

            @Override
            public void onClockUpdate(long offset) {
                invoked.set(true);
            }
        };
        TestUtil.setFieldValue(provider, "listener", listener);

        provider.syncClock();
    }

}
