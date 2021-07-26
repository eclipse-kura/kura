/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.linux.clock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.junit.Test;

public class NtpdClockSyncProviderTest {

    @Test
    public void testSynch() throws KuraException, NoSuchFieldException {

        CommandStatus status = new CommandStatus(new Command(new String[] {}), new LinuxExitStatus(0));
        CommandExecutorService serviceMock = mock(CommandExecutorService.class);
        when(serviceMock.execute(anyObject())).thenReturn(status);
        NtpdClockSyncProvider provider = new NtpdClockSyncProvider(serviceMock);

        AtomicBoolean invoked = new AtomicBoolean(false);
        ClockSyncListener listener = new ClockSyncListener() {

            @Override
            public void onClockUpdate(long offset, boolean update) {
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

        CommandStatus status = new CommandStatus(new Command(new String[] {}), new LinuxExitStatus(1));
        CommandExecutorService serviceMock = mock(CommandExecutorService.class);
        when(serviceMock.execute(anyObject())).thenReturn(status);
        NtpdClockSyncProvider provider = new NtpdClockSyncProvider(serviceMock);

        AtomicBoolean invoked = new AtomicBoolean(false);
        ClockSyncListener listener = new ClockSyncListener() {

            @Override
            public void onClockUpdate(long offset, boolean update) {
                invoked.set(true);
            }
        };
        TestUtil.setFieldValue(provider, "listener", listener);

        boolean synched = provider.syncClock();

        assertFalse(synched);
        assertFalse(invoked.get());
    }

}
