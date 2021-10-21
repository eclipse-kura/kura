/*******************************************************************************
 * Copyright (c) 2021 Sterwen-Technology and others
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.executor.LinuxExitStatus;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.executor.Pid;
import org.eclipse.kura.executor.Signal;
import org.eclipse.kura.net.wifi.WifiChannel;
import org.eclipse.kura.net.wifi.WifiConfig;
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
        OutputStream out = new ByteArrayOutputStream();
        try (Writer w = new OutputStreamWriter(out, "UTF-8")) {
            w.write(commandOutput);
        } catch (Exception e) {
        }
        returnedStatus.setOutputStream(out);
    }
};

public class IwCapabilityToolTest {

    protected static final CommandStatus successStatus = new CommandStatus(new Command(new String[] {}),
            new LinuxExitStatus(0));

    static private final int PHY0 = 0;
    static private final int WIFI_2G_CHANNELS = 14;
    static private final int WIFI_5G_CHANNELS = 29;
    static private final String COUNTRY_CODE_00 = "00";
    static private final String COUNTRY_CODE_FR = "FR";

    private String getWifiCountryCode00() {
        return "global\n" + "country 00: DFS-UNSET\n" + "        (2402 - 2472 @ 40), (N/A, 20), (N/A)\n"
                + "        (2457 - 2482 @ 20), (N/A, 20), (N/A), AUTO-BW, PASSIVE-SCAN\n"
                + "        (2474 - 2494 @ 20), (N/A, 20), (N/A), NO-OFDM, PASSIVE-SCAN\n"
                + "        (5170 - 5250 @ 80), (N/A, 20), (N/A), AUTO-BW, PASSIVE-SCAN\n"
                + "        (5250 - 5330 @ 80), (N/A, 20), (0 ms), DFS, AUTO-BW, PASSIVE-SCAN\n"
                + "        (5490 - 5730 @ 160), (N/A, 20), (0 ms), DFS, PASSIVE-SCAN\n"
                + "        (5735 - 5835 @ 80), (N/A, 20), (N/A), PASSIVE-SCAN\n"
                + "        (57240 - 63720 @ 2160), (N/A, 0), (N/A)\n";
    }

    private String getWifiCountryCodeFR() {
        return "global\n" + "country FR: DFS-ETSI\n" + "        (2402 - 2482 @ 40), (N/A, 20), (N/A)\n"
                + "        (5170 - 5250 @ 80), (N/A, 20), (N/A), AUTO-BW\n"
                + "        (5250 - 5330 @ 80), (N/A, 20), (0 ms), DFS, AUTO-BW\n"
                + "        (5490 - 5710 @ 160), (N/A, 27), (0 ms), DFS\n"
                + "        (57000 - 66000 @ 2160), (N/A, 40), (N/A)\n";
    }

    private String getPhyInfo() {
        return "Interface wlan0\n" + "\tifindex 3\n" + "\twdev 0x1\n" + "\taddr b8:27:eb:86:d5:0e\n"
                + "\tssid kura_gateway_B8:27:EB:D3:80:5B\n" + "\ttype AP\n" + "\twiphy 0\n"
                + "\tchannel 1 (2412 MHz), width: 20 MHz, center1: 2412 MHz\n" + "\ttxpower 31.00 dBm\n";
    }

    private String getWifi2GInfo() {
        return "                Frequencies:\n" + "                        * 2412 MHz [1] (20.0 dBm)\n"
                + "                        * 2417 MHz [2] (20.0 dBm)\n"
                + "                        * 2422 MHz [3] (20.0 dBm)\n"
                + "                        * 2427 MHz [4] (20.0 dBm)\n"
                + "                        * 2432 MHz [5] (20.0 dBm)\n"
                + "                        * 2437 MHz [6] (20.0 dBm)\n"
                + "                        * 2442 MHz [7] (20.0 dBm)\n"
                + "                        * 2447 MHz [8] (20.0 dBm)\n"
                + "                        * 2452 MHz [9] (20.0 dBm)\n"
                + "                        * 2457 MHz [10] (20.0 dBm)\n"
                + "                        * 2462 MHz [11] (20.0 dBm)\n"
                + "                        * 2467 MHz [12] (20.0 dBm)\n"
                + "                        * 2472 MHz [13] (20.0 dBm)\n"
                + "                        * 2484 MHz [14] (disabled)\n";
    }

    private String getWifi5GInfo() {
        return "                Frequencies:\n" + "                        * 5170 MHz [34] (disabled)\n"
                + "                        * 5180 MHz [36] (20.0 dBm)\n"
                + "                        * 5190 MHz [38] (disabled)\n"
                + "                        * 5200 MHz [40] (20.0 dBm)\n"
                + "                        * 5210 MHz [42] (disabled)\n"
                + "                        * 5220 MHz [44] (20.0 dBm)\n"
                + "                        * 5230 MHz [46] (disabled)\n"
                + "                        * 5240 MHz [48] (20.0 dBm)\n"
                + "                        * 5260 MHz [52] (20.0 dBm) (no IR, radar detection)\n"
                + "                        * 5280 MHz [56] (20.0 dBm) (no IR, radar detection)\n"
                + "                        * 5300 MHz [60] (20.0 dBm) (no IR, radar detection)\n"
                + "                        * 5320 MHz [64] (20.0 dBm) (no IR, radar detection)\n"
                + "                        * 5500 MHz [100] (20.0 dBm) (no IR, radar detection)\n"
                + "                        * 5520 MHz [104] (20.0 dBm) (no IR, radar detection)\n"
                + "                        * 5540 MHz [108] (20.0 dBm) (no IR, radar detection)\n"
                + "                        * 5560 MHz [112] (20.0 dBm) (no IR, radar detection)\n"
                + "                        * 5580 MHz [116] (20.0 dBm) (no IR, radar detection)\n"
                + "                        * 5600 MHz [120] (20.0 dBm) (no IR, radar detection)\n"
                + "                        * 5620 MHz [124] (20.0 dBm) (no IR, radar detection)\n"
                + "                        * 5640 MHz [128] (20.0 dBm) (no IR, radar detection)\n"
                + "                        * 5660 MHz [132] (20.0 dBm) (no IR, radar detection)\n"
                + "                        * 5680 MHz [136] (20.0 dBm) (no IR, radar detection)\n"
                + "                        * 5700 MHz [140] (20.0 dBm) (no IR, radar detection)\n"
                + "                        * 5720 MHz [144] (20.0 dBm) (no IR, radar detection)\n"
                + "                        * 5745 MHz [149] (20.0 dBm)\n"
                + "                        * 5765 MHz [153] (20.0 dBm)\n"
                + "                        * 5785 MHz [157] (20.0 dBm)\n"
                + "                        * 5805 MHz [161] (20.0 dBm)\n"
                + "                        * 5825 MHz [165] (20.0 dBm)\n";
    }

    @Test
    public void parsePhy() throws KuraException, java.io.IOException {
        String commandOutput = getPhyInfo();
        CommandExecutorServiceStub executorServiceStub = new CommandExecutorServiceStub(successStatus);
        executorServiceStub.writeOutput(commandOutput);
        final int phy = IwCapabilityTool
                .parseWiphyIndex(IwCapabilityTool.exec(new String[] { "iw", "wlan0", "info" }, executorServiceStub))
                .orElseThrow(() -> new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR,
                        "failed to get phy index for " + "wlan0"));
        assertEquals(PHY0, phy);
    }

    @Test
    public void probeChannels_2G() throws KuraException {
        String commandOutput = getPhyInfo() + getWifi2GInfo();
        CommandExecutorServiceStub executorServiceStub = new CommandExecutorServiceStub(successStatus);
        executorServiceStub.writeOutput(commandOutput);
        List<WifiChannel> channels = IwCapabilityTool.probeChannels("wlan0", executorServiceStub);
        assertEquals(WIFI_2G_CHANNELS, channels.size());
    }

    @Test
    public void probeChannels_5G() throws KuraException {
        String commandOutput = getPhyInfo() + getWifi5GInfo();
        CommandExecutorServiceStub executorServiceStub = new CommandExecutorServiceStub(successStatus);
        executorServiceStub.writeOutput(commandOutput);
        List<WifiChannel> channels = IwCapabilityTool.probeChannels("wlan0", executorServiceStub);
        assertEquals(WIFI_5G_CHANNELS, channels.size());
    }

    @Test
    public void getWifiCountryCode_Unknown() throws KuraException {
        String commandOutput = getWifiCountryCode00();
        CommandExecutorServiceStub executorServiceStub = new CommandExecutorServiceStub(successStatus);
        executorServiceStub.writeOutput(commandOutput);
        String countryCode = IwCapabilityTool.getWifiCountryCode(executorServiceStub);
        assertEquals(COUNTRY_CODE_00, countryCode);
    }

    @Test
    public void getWifiCountryCode_FR() throws KuraException {
        String commandOutput = getWifiCountryCodeFR();
        CommandExecutorServiceStub executorServiceStub = new CommandExecutorServiceStub(successStatus);
        executorServiceStub.writeOutput(commandOutput);
        String countryCode = IwCapabilityTool.getWifiCountryCode(executorServiceStub);
        assertEquals(COUNTRY_CODE_FR, countryCode);
    }

    @Test
    public void checkWifiConfig() throws KuraException {
        String commandOutput = getPhyInfo() + getWifi2GInfo() + getWifi5GInfo();
        CommandExecutorServiceStub executorServiceStub = new CommandExecutorServiceStub(successStatus);
        executorServiceStub.writeOutput(commandOutput);
        List<WifiChannel> channels = IwCapabilityTool.probeChannels("wlan0", executorServiceStub);
        assertEquals(WIFI_2G_CHANNELS + WIFI_5G_CHANNELS, channels.size());
        commandOutput = getWifiCountryCodeFR();
        executorServiceStub.writeOutput(commandOutput);
        String countryCode = IwCapabilityTool.getWifiCountryCode(executorServiceStub);
        assertEquals(COUNTRY_CODE_FR, countryCode);
        WifiConfig config = new WifiConfig();
        config.setChannelFrequencies(channels);
        config.setWifiCountryCode(countryCode);
        String output = config.toString();
        assertTrue(output.indexOf("countryCode: FR") >= 0);
        assertTrue(output.indexOf("channelFrequencies:") >= 0);
    }
}
