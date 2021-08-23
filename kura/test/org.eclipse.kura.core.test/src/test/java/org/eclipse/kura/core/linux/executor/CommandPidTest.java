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
package org.eclipse.kura.core.linux.executor;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.core.internal.linux.executor.ExecutorUtil;
import org.eclipse.kura.core.linux.executor.privileged.PrivilegedExecutorServiceImpl;
import org.eclipse.kura.core.linux.executor.unprivileged.UnprivilegedExecutorServiceImpl;
import org.eclipse.kura.core.testutil.TestUtil;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.Pid;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CommandPidTest {

    private static CommandExecutorService executor;
    private static String[] commandLine = { "find", "/", "-name", "foo" };
    private Map<String, Pid> pids = new HashMap<>();

    public CommandPidTest(CommandExecutorService executor) {
        CommandPidTest.executor = executor;
    }

    @Parameterized.Parameters
    public static Collection<CommandExecutorService> getExecutors() {
        return Arrays.asList(new UnprivilegedExecutorServiceImpl(), new PrivilegedExecutorServiceImpl());
    }

    @Test
    public void shouldRetrievePidsFromCommandLine() {
        givenCommandExecutor();

        whenRetrievePids(commandLine);

        thenPidsAreNotEmpty();
    }

    private void givenCommandExecutor() {
        Map<String, Pid> p = new HashMap<>();
        p.put(String.join(" ", commandLine), new LinuxPid(1234));
        ExecutorUtil euMock = mock(ExecutorUtil.class);
        when(euMock.getPids(commandLine)).thenReturn(p);
        try {
            TestUtil.setFieldValue(executor, "executorUtil", euMock);
        } catch (NoSuchFieldException e) {
            // Do nothing...
        }
    }

    private void whenRetrievePids(String[] commandLine) {
        this.pids = executor.getPids(commandLine);
    }

    private void thenPidsAreNotEmpty() {
        assertFalse(this.pids.isEmpty());
    }

}
