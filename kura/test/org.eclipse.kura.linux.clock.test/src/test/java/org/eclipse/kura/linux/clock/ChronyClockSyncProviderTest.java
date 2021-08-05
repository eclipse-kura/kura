/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.eclipse.kura.core.util.IOUtil;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.junit.Test;

public class ChronyClockSyncProviderTest {

    @Test
    public void testSynch() throws NoSuchFieldException, IOException, KuraException, NoSuchAlgorithmException {

        assumeTrue("Only run this test on Linux", System.getProperty("os.name").matches("[Ll]inux"));

        InputStream journalEntry = new ByteArrayInputStream(
                IOUtil.readResource("journal-entry.json").getBytes(StandardCharsets.UTF_8));
        OutputStream statusOutputStream = new ByteArrayOutputStream();
        IOUtils.copy(journalEntry, statusOutputStream);

        CommandStatus status = new CommandStatus(new Command(new String[] {}), new LinuxExitStatus(0));
        status.setOutputStream(statusOutputStream);

        CommandExecutorService commandExecutorMock = mock(CommandExecutorService.class);
        when(commandExecutorMock.execute(anyObject())).thenReturn(status);

        CryptoService cryptoServiceMock = mock(CryptoService.class);
        when(cryptoServiceMock.sha256Hash(anyObject())).thenReturn("");

        ChronyClockSyncProvider ntsClockSyncProvider = new ChronyClockSyncProvider(commandExecutorMock,
                cryptoServiceMock);
        AtomicBoolean invoked = new AtomicBoolean(false);
        ClockSyncListener listener = (offset, update) -> {
            assertEquals(0, offset);

            invoked.set(true);
        };

        Map<String, Object> properties = new HashMap<>();
        properties.put("enabled", true);
        properties.put("clock.provider", "chrony-advanced");
        properties.put("chrony.advanced.configlocation", "placeholder_path");
        ClockServiceConfig clockServiceConfig = new ClockServiceConfig(properties);

        ntsClockSyncProvider.init(clockServiceConfig, null, listener);

        boolean synched = ntsClockSyncProvider.syncClock();

        assertTrue(synched);
        assertTrue(invoked.get());
    }
}
