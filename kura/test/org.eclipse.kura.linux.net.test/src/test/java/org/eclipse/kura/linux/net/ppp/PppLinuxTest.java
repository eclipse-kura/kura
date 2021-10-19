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
package org.eclipse.kura.linux.net.ppp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.executor.LinuxSignal;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.Pid;
import org.eclipse.kura.executor.Signal;
import org.junit.Test;
import org.mockito.Mockito;

public class PppLinuxTest {

    @Test
    public void shouldTerminatePppdWithSigterm() {
        givenProcess("/usr/sbin/pppd /dev/ttyACM0 call ppp1", 10, LinuxSignal.SIGTERM);

        whenPppdIsStoppedFor("ppp1", "/dev/ttyACM0");

        thenProcessReceivedSignals(LinuxSignal.SIGTERM);
        thenProcessIsStopped();
    }

    @Test
    public void shouldTerminatePppdWithSigkill() {
        givenProcess("/usr/sbin/pppd /dev/ttyACM0 call ppp1", 10, LinuxSignal.SIGKILL);

        whenPppdIsStoppedFor("ppp1", "/dev/ttyACM0");

        thenProcessReceivedSignals(LinuxSignal.SIGTERM, LinuxSignal.SIGKILL);
        thenProcessIsStopped();
    }

    private final CommandExecutorService commandExecutorService = Mockito.mock(CommandExecutorService.class);
    private final PppLinux pppLinux = new PppLinux(commandExecutorService);
    private Optional<Process> process = Optional.empty();

    public PppLinuxTest() {
        Mockito.when(commandExecutorService.getPids(Mockito.any())).thenAnswer(i -> {

            final String[] requestedCommand = i.getArgumentAt(0, String[].class);

            final String requestedCommandConcat = concat(requestedCommand, " ");

            return process.map(Collections::singletonList)
                    .orElseThrow(() -> new IllegalStateException("process not configured")).stream()
                    .filter(p -> p.commandLine.contains(requestedCommandConcat) && p.stopped == false)
                    .collect(Collectors.toMap(p -> p.commandLine, p -> p.pid));
        });

        Mockito.when(commandExecutorService.stop(Mockito.any(), Mockito.any())).then(i -> {
            final Pid pid = i.getArgumentAt(0, Pid.class);
            final Signal signal = i.getArgumentAt(1, Signal.class);

            final Optional<Process> targetProcess = process.filter(p -> p.pid.getPid() == pid.getPid());

            if (targetProcess.isPresent()) {
                targetProcess.get().sendSignal(signal);
                return true;
            } else {
                return false;
            }
        });

        Mockito.when(commandExecutorService.isRunning(Mockito.any(Pid.class))).thenAnswer(i -> {
            final Pid pid = i.getArgumentAt(0, Pid.class);

            return process.filter(p -> p.pid.getPid() == pid.getPid() && !p.stopped).isPresent();
        });
    }

    private void thenProcessReceivedSignals(final Signal... signals) {
        final Process currentProcess = process.orElseThrow(() -> new IllegalStateException("process not configured"));

        assertEquals(Arrays.asList(signals), currentProcess.receivedSignals);
    }

    private void thenProcessIsStopped() {
        final Process currentProcess = process.orElseThrow(() -> new IllegalStateException("process not configured"));

        assertTrue("process is not stopped", currentProcess.stopped);
    }

    private void givenProcess(final String command, final int pid, final Signal requiredStopSignal) {
        process = Optional.of(new Process(command, () -> pid, requiredStopSignal));
    }

    private void whenPppdIsStoppedFor(final String iface, final String port) {
        try {
            pppLinux.disconnect(iface, port);
        } catch (final KuraException e) {
            fail("failed to disconnect " + e);
        }
    }

    private String concat(final String[] value, final String delim) {
        return String.join(delim, value);
    }

    private static class Process {

        private final String commandLine;
        private final Pid pid;
        private final Signal stopSignal;
        private List<Signal> receivedSignals = new ArrayList<>();
        private boolean stopped = false;

        public Process(final String commandLine, final Pid pid, final Signal stopSignal) {
            this.commandLine = commandLine;
            this.pid = pid;
            this.stopSignal = stopSignal;
        }

        private void sendSignal(final Signal signal) {
            receivedSignals.add(signal);
            if (signal == stopSignal) {
                stopped = true;
            }
        }

    }
}
