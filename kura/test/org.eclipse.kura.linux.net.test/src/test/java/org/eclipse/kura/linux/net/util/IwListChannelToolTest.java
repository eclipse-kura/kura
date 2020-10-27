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
 *  Sterwen-Technology
 ******************************************************************************/

package org.eclipse.kura.linux.net.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.Pid;
import org.eclipse.kura.executor.Signal;
import org.eclipse.kura.net.wifi.WifiChannel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.Mock;

import java.net.UnknownHostException;

import org.eclipse.kura.KuraException;
import org.junit.Test;

class CommandExecutorServiceStub implements CommandExecutorService {
    CommandStatus returnedStatus;

    CommandExecutorServiceStub(CommandStatus returnedStatus) {
         this.returnedStatus = returnedStatus;
    }

    public CommandStatus execute(Command command) {
        return returnedStatus;
    }
    public void execute(Command command, Consumer<CommandStatus> callback) {
    }
    public boolean stop(Pid pid, Signal signal) {
    	return true;
    }
    public boolean kill(String[] commandLine, Signal signal) {
    	return true;
    }
    public boolean isRunning(Pid pid) {
    	return true;
    }
    public boolean isRunning(String[] commandLine) {
    	return true;
    }
    public Map<String, Pid> getPids(String[] commandLine) {
    	return null;
    }
    public void writeOutput(String commandOutput) {
        OutputStream out= new ByteArrayOutputStream();
        try (Writer w = new OutputStreamWriter(out, "UTF-8")) {
            w.write(commandOutput);
        } catch (Exception e) {
        }
        returnedStatus.setOutputStream(out);
    }
};

public class IwListChannelToolTest {
	protected static CommandExecutorService executorServiceMock;
    protected static final CommandStatus successStatus = new CommandStatus(new Command(new String[] {}),
            new LinuxExitStatus(0));

    @Test
    public void probeChannels_2G() throws KuraException {
        String commandOutput;
        commandOutput = "wlan0     14 channels in total; available frequencies :\n"
                + "          Channel 01 : 2.412 GHz\n"
                + "          Channel 02 : 2.417 GHz\n"
                + "          Channel 03 : 2.422 GHz\n"
                + "          Channel 04 : 2.427 GHz\n"
                + "          Channel 05 : 2.432 GHz\n"
                + "          Channel 06 : 2.437 GHz\n"
                + "          Channel 07 : 2.442 GHz\n"
                + "          Channel 08 : 2.447 GHz\n"
                + "          Channel 09 : 2.452 GHz\n"
                + "          Channel 10 : 2.457 GHz\n"
                + "          Channel 11 : 2.462 GHz\n"
                + "          Channel 12 : 2.467 GHz\n"
                + "          Channel 13 : 2.472 GHz\n"
                + "          Channel 14 : 2.484 GHz\n";
//        CommandStatus successStatus = new CommandStatus(new Command(new String[] {}),
//                new LinuxExitStatus(0));
        CommandExecutorServiceStub executorServiceStub = new CommandExecutorServiceStub(successStatus);
        executorServiceStub.writeOutput(commandOutput);
        List<WifiChannel> channels = IwListChannelTool.probeChannels("wlan0", executorServiceStub);
        assertEquals(channels.size(), 14);
    }

    @Test
    public void probeChannels_5G() throws KuraException {
        String commandOutput;
        commandOutput = "wlan0     32 channels in total; available frequencies :\n"
                + "          Channel 01 : 2.412 GHz\n"
                + "          Channel 02 : 2.417 GHz\n"
                + "          Channel 03 : 2.422 GHz\n"
                + "          Channel 04 : 2.427 GHz\n"
                + "          Channel 05 : 2.432 GHz\n"
                + "          Channel 06 : 2.437 GHz\n"
                + "          Channel 07 : 2.442 GHz\n"
                + "          Channel 08 : 2.447 GHz\n"
                + "          Channel 09 : 2.452 GHz\n"
                + "          Channel 10 : 2.457 GHz\n"
                + "          Channel 11 : 2.462 GHz\n"
                + "          Channel 12 : 2.467 GHz\n"
                + "          Channel 13 : 2.472 GHz\n"
                + "          Channel 14 : 2.484 GHz\n"
                + "          Channel 36 : 5.18 GHz\n"
                + "          Channel 38 : 5.19 GHz\n"
                + "          Channel 40 : 5.2 GHz\n"
                + "          Channel 42 : 5.21 GHz\n"
                + "          Channel 44 : 5.22 GHz\n"
                + "          Channel 46 : 5.23 GHz\n"
                + "          Channel 48 : 5.24 GHz\n"
                + "          Channel 52 : 5.26 GHz\n"
                + "          Channel 56 : 5.28 GHz\n"
                + "          Channel 60 : 5.3 GHz\n"
                + "          Channel 64 : 5.32 GHz\n"
                + "          Channel 100 : 5.5 GHz\n"
                + "          Channel 104 : 5.52 GHz\n"
                + "          Channel 108 : 5.54 GHz\n"
                + "          Channel 112 : 5.56 GHz\n"
                + "          Channel 116 : 5.58 GHz\n"
                + "          Channel 120 : 5.6 GHz\n"
                + "          Channel 124 : 5.62 GHz\n";
//        CommandStatus successStatus = new CommandStatus(new Command(new String[] {}),
//                new LinuxExitStatus(0));
        CommandExecutorServiceStub executorServiceStub = new CommandExecutorServiceStub(successStatus);
        executorServiceStub.writeOutput(commandOutput);
        List<WifiChannel> channels = IwListChannelTool.probeChannels("wlan0", executorServiceStub);
        assertEquals(channels.size(), 32);
    }

    @Test
    public void getWifiCountryCode_Unknown() throws KuraException {
        String commandOutput;
        commandOutput = "country 00: DFS-UNSET\n"
                + "        (2402 - 2472 @ 40), (N/A, 20), (N/A)\n"
                + "        (2457 - 2482 @ 20), (N/A, 20), (N/A), NO-IR\n"
                + "        (2474 - 2494 @ 20), (N/A, 20), (N/A), NO-OFDM, NO-IR\n"
                + "        (5170 - 5250 @ 80), (N/A, 20), (N/A), NO-IR\n"
                + "        (5250 - 5330 @ 80), (N/A, 20), (0 ms), DFS, NO-IR\n"
                + "        (5490 - 5730 @ 160), (N/A, 20), (0 ms), DFS, NO-IR\n"
                + "        (5735 - 5835 @ 80), (N/A, 20), (N/A), NO-IR\n"
                + "        (57240 - 63720 @ 2160), (N/A, 0), (N/A)";
        CommandExecutorServiceStub executorServiceStub = new CommandExecutorServiceStub(successStatus);
        executorServiceStub.writeOutput(commandOutput);
        String countryCode = IwListChannelTool.getWifiCountryCode(executorServiceStub);
        assertEquals(countryCode, "00");
    }

    @Test
    public void getWifiCountryCode_FR() throws KuraException {
        String commandOutput;
        commandOutput = "country FR: DFS-ETSI\n"
                + "        (2402 - 2482 @ 40), (N/A, 20), (N/A)\n"
                + "        (5170 - 5250 @ 80), (N/A, 20), (N/A)\n"
                + "        (5250 - 5330 @ 80), (N/A, 20), (0 ms), DFS\n"
                + "        (5490 - 5710 @ 160), (N/A, 27), (0 ms), DFS\n"
                + "        (57000 - 66000 @ 2160), (N/A, 40), (N/A)";
        CommandExecutorServiceStub executorServiceStub = new CommandExecutorServiceStub(successStatus);
        executorServiceStub.writeOutput(commandOutput);
        String countryCode = IwListChannelTool.getWifiCountryCode(executorServiceStub);
        assertEquals(countryCode, "FR");
    }
}
