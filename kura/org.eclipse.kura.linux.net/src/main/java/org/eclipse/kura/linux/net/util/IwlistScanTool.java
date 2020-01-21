/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.linux.net.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.io.Charsets;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.net.WifiAccessPointImpl;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IwlistScanTool implements IScanTool {

    private static final Logger logger = LoggerFactory.getLogger(IwlistScanTool.class);

    private static final Object lock = new Object();
    private String ifaceName;
    private int timeout;
    private final CommandExecutorService executorService;

    private InputStream scanOutput;
    private boolean status;
    private String errmsg;

    protected IwlistScanTool(CommandExecutorService executorService) {
        this.timeout = 20;
        this.executorService = executorService;
    }

    protected IwlistScanTool(String ifaceName, CommandExecutorService executorService) {
        this(executorService);
        this.ifaceName = ifaceName;
        this.errmsg = "";
        this.status = false;
    }

    protected IwlistScanTool(String ifaceName, int tout, CommandExecutorService executorService) {
        this(ifaceName, executorService);
        this.timeout = tout;
    }

    @Override
    public List<WifiAccessPoint> scan() throws KuraException {

        String[] cmdIfconfig = { "ifconfig", this.ifaceName, "up" };
        Command ifconfigCommand = new Command(cmdIfconfig);
        ifconfigCommand.setErrorStream(new ByteArrayOutputStream());
        CommandStatus ifconfigCommandStatus = this.executorService.execute(ifconfigCommand);
        if (!ifconfigCommandStatus.getExitStatus().isSuccessful() && logger.isErrorEnabled()) {
            logger.error("failed to execute the {} command {}", String.join(" ", cmdIfconfig), new String(
                    ((ByteArrayOutputStream) ifconfigCommandStatus.getErrorStream()).toByteArray(), Charsets.UTF_8));
        }

        List<WifiAccessPoint> wifiAccessPoints;
        synchronized (lock) {

            String[] cmdIwList = formIwlistScanCommand(IwlistScanTool.this.ifaceName);
            if (logger.isInfoEnabled()) {
                logger.info("scan() :: executing: {}", String.join(" ", cmdIwList));
            }
            IwlistScanTool.this.status = false;
            Command iwListCommand = new Command(cmdIwList);
            iwListCommand.setTimeout(IwlistScanTool.this.timeout);
            iwListCommand.setOutputStream(new ByteArrayOutputStream());
            CommandStatus iwListCommandStatus = this.executorService.execute(iwListCommand);
            int exitValue = iwListCommandStatus.getExitStatus().getExitCode();
            if (logger.isInfoEnabled()) {
                logger.info("scan() :: {} command returns status = {}", String.join(" ", cmdIwList), exitValue);
            }
            if (iwListCommandStatus.getExitStatus().isSuccessful()) {
                IwlistScanTool.this.status = true;
                IwlistScanTool.this.scanOutput = new ByteArrayInputStream(
                        ((ByteArrayOutputStream) iwListCommandStatus.getOutputStream()).toByteArray());
            } else {
                if (logger.isErrorEnabled()) {
                    logger.error("scan() :: failed to execute {} error code is {}", String.join(" ", cmdIwList),
                            exitValue);
                }
            }

            if (!this.status) {
                throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, this.errmsg);
            }

            logger.info("scan() :: the 'iw scan' command executed successfully, parsing output ...");
            try {
                wifiAccessPoints = parse();
            } catch (Exception e) {
                throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, "error parsing scan results");
            }
        }
        return wifiAccessPoints;
    }

    private List<WifiAccessPoint> parse() throws KuraException {

        List<WifiAccessPoint> wifiAccessPoints = new ArrayList<>();

        // get the output
        String line = null;
        String ssid = null;
        List<Long> bitrate = new ArrayList<>();
        long frequency = -1;
        byte[] hardwareAddress = null;
        WifiMode mode = null;
        EnumSet<WifiSecurity> rsnSecurity = null;
        int strength = -1;
        EnumSet<WifiSecurity> wpaSecurity = null;
        try (InputStreamReader isr = new InputStreamReader(this.scanOutput);
                BufferedReader br = new BufferedReader(isr)) {
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("Cell")) {
                    // new AP
                    if (ssid != null) {
                        WifiAccessPointImpl wifiAccessPoint = new WifiAccessPointImpl(ssid);
                        wifiAccessPoint.setBitrate(bitrate);
                        wifiAccessPoint.setFrequency(frequency);
                        wifiAccessPoint.setHardwareAddress(hardwareAddress);
                        wifiAccessPoint.setMode(mode);
                        wifiAccessPoint.setRsnSecurity(rsnSecurity);
                        wifiAccessPoint.setStrength(strength);
                        wifiAccessPoint.setWpaSecurity(wpaSecurity);
                        wifiAccessPoints.add(wifiAccessPoint);
                    }

                    // reset
                    ssid = null;
                    bitrate = new ArrayList<>();
                    frequency = -1;
                    hardwareAddress = null;
                    mode = null;
                    rsnSecurity = null;
                    strength = -1;
                    wpaSecurity = null;

                    // parse out the MAC
                    hardwareAddress = parseHardwareAddress(line);
                } else if (line.startsWith("ESSID:")) {
                    ssid = line.substring("ESSID:".length() + 1, line.length() - 1);
                } else if (line.startsWith("Quality=")) {
                    strength = parseStrength(line);
                } else if (line.startsWith("Mode:")) {
                    mode = parseMode(line);
                } else if (line.startsWith("Frequency:")) {
                    line = line.substring("Frequency:".length(), line.indexOf(' '));
                    frequency = (long) (Float.parseFloat(line) * 1000);
                } else if (line.startsWith("Bit Rates:")) {
                    bitrate.addAll(parseBitRate(line));
                } else if (line.contains("IE: IEEE 802.11i/WPA2")) {
                    rsnSecurity = setWifiSecurity(br);
                } else if (line.contains("IE: WPA Version")) {
                    wpaSecurity = setWifiSecurity(br);
                }
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        }

        // store the last one
        if (ssid != null) {
            WifiAccessPointImpl wifiAccessPoint = new WifiAccessPointImpl(ssid);
            wifiAccessPoint.setBitrate(bitrate);
            wifiAccessPoint.setFrequency(frequency);
            wifiAccessPoint.setHardwareAddress(hardwareAddress);
            wifiAccessPoint.setMode(mode);
            wifiAccessPoint.setRsnSecurity(rsnSecurity);
            wifiAccessPoint.setStrength(strength);
            wifiAccessPoint.setWpaSecurity(wpaSecurity);
            wifiAccessPoints.add(wifiAccessPoint);
        }
        return wifiAccessPoints;
    }

    private byte[] parseHardwareAddress(String line) {
        byte[] hardwareAddress = null;
        StringTokenizer st = new StringTokenizer(line, " ");
        st.nextToken(); // eat Cell
        st.nextToken(); // eat Cell #
        st.nextToken(); // eat '-'
        st.nextToken(); // eat 'Address:'
        String macAddressString = st.nextToken();
        if (macAddressString != null) {
            hardwareAddress = NetworkUtil.macToBytes(macAddressString);
        }
        return hardwareAddress;
    }

    private WifiMode parseMode(String line) {
        WifiMode mode = null;
        line = line.substring("Mode:".length());
        if ("Master".equals(line)) {
            mode = WifiMode.MASTER;
        }
        return mode;
    }

    private List<Long> parseBitRate(String line) {
        List<Long> bitrate = new ArrayList<>();
        line = line.substring("Bit Rates:".length());
        String[] bitRates = line.split(";");
        for (String rate : bitRates) {
            if (rate != null) {
                rate = rate.trim();
                if (rate.length() > 0) {
                    rate = rate.substring(0, rate.indexOf(' '));
                    bitrate.add((long) (Float.parseFloat(rate) * 1000000));
                }
            }
        }
        return bitrate;
    }

    private int parseStrength(String line) {
        int strength = -1;
        StringTokenizer st = new StringTokenizer(line, " ");
        st.nextToken(); // eat 'Quality='
        st.nextToken(); // eat 'Signal'
        String signalLevel = st.nextToken();
        if (signalLevel != null) {
            signalLevel = signalLevel.substring(signalLevel.indexOf('=') + 1);
            if (signalLevel.contains("/")) {
                // Could also be of format 39/100
                final String[] parts = signalLevel.split("/");
                strength = (int) Float.parseFloat(parts[0]);
                strength = SignalStrengthConversion.getRssi(strength);
            } else {
                strength = (int) Float.parseFloat(signalLevel);
            }
            strength = Math.abs(strength);
        }
        return strength;
    }

    private EnumSet<WifiSecurity> setWifiSecurity(BufferedReader br) throws IOException {
        EnumSet<WifiSecurity> wifiWpaSecurity = EnumSet.noneOf(WifiSecurity.class);
        boolean foundGroup = false;
        boolean foundPairwise = false;
        boolean foundAuthSuites = false;
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.contains("Group Cipher")) {
                foundGroup = true;
                setGroupCiphers(line, wifiWpaSecurity);
            } else if (line.contains("Pairwise Ciphers")) {
                foundPairwise = true;
                setPairwiseCiphers(line, wifiWpaSecurity);
            } else if (line.contains("Authentication Suites")) {
                foundAuthSuites = true;
                if (line.contains("802_1X")) {
                    wifiWpaSecurity.add(WifiSecurity.KEY_MGMT_802_1X);
                }
                if (line.contains("PSK")) {
                    wifiWpaSecurity.add(WifiSecurity.KEY_MGMT_PSK);
                }
            } else {
                logger.debug("Ignoring line in WPA/RSN: {}", line);
            }

            if (foundGroup && foundPairwise && foundAuthSuites) {
                break;
            }
        }
        return wifiWpaSecurity;
    }

    private void setGroupCiphers(String line, EnumSet<WifiSecurity> wifiWpaSecurity) {
        if (line.contains("CCMP")) {
            wifiWpaSecurity.add(WifiSecurity.GROUP_CCMP);
        }
        if (line.contains("TKIP")) {
            wifiWpaSecurity.add(WifiSecurity.GROUP_TKIP);
        }
        if (line.contains("WEP104")) {
            wifiWpaSecurity.add(WifiSecurity.GROUP_WEP104);
        }
        if (line.contains("WEP40")) {
            wifiWpaSecurity.add(WifiSecurity.GROUP_WEP40);
        }
    }

    private void setPairwiseCiphers(String line, EnumSet<WifiSecurity> wifiWpaSecurity) {
        if (line.contains("CCMP")) {
            wifiWpaSecurity.add(WifiSecurity.PAIR_CCMP);
        }
        if (line.contains("TKIP")) {
            wifiWpaSecurity.add(WifiSecurity.PAIR_TKIP);
        }
        if (line.contains("WEP104")) {
            wifiWpaSecurity.add(WifiSecurity.PAIR_WEP104);
        }
        if (line.contains("WEP40")) {
            wifiWpaSecurity.add(WifiSecurity.PAIR_WEP40);
        }
    }

    private String[] formIwlistScanCommand(String interfaceName) {
        return new String[] { "iwlist", interfaceName, "scanning" };
    }
}
