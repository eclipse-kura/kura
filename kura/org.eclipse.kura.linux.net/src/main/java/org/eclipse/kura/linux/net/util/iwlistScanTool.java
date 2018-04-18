/*******************************************************************************
 * Copyright (c) 2011, 2017 Eurotech and/or its affiliates
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.linux.util.LinuxProcessUtil;
import org.eclipse.kura.core.net.WifiAccessPointImpl;
import org.eclipse.kura.core.net.util.NetworkUtil;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.net.wifi.WifiAccessPoint;
import org.eclipse.kura.net.wifi.WifiMode;
import org.eclipse.kura.net.wifi.WifiSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class iwlistScanTool implements IScanTool {

    private static final Logger logger = LoggerFactory.getLogger(iwlistScanTool.class);

    private static final String SCAN_THREAD_NAME = "iwlistScanThread";

    private static final Object lock = new Object();
    private String ifaceName;
    private int timeout;

    // FIXME:MC Is this process always closed?
    private SafeProcess process;
    private boolean status;
    private String errmsg;

    protected iwlistScanTool() {
        this.timeout = 20;
    }

    protected iwlistScanTool(String ifaceName) {
        this();
        this.ifaceName = ifaceName;
        this.errmsg = "";
        this.status = false;
    }

    protected iwlistScanTool(String ifaceName, int tout) {
        this(ifaceName);
        this.timeout = tout;
    }

    @Override
    public List<WifiAccessPoint> scan() throws KuraException {

        StringBuilder sb = new StringBuilder();
        sb.append("ifconfig ").append(this.ifaceName).append(" up");
        SafeProcess proc = null;
        try {
            proc = ProcessUtil.exec(sb.toString());
            proc.waitFor();
        } catch (Exception e) {
            logger.error("failed to execute the {} command ", sb.toString(), e);
        } finally {
            if (proc != null) {
                proc.destroy();
            }
        }

        List<WifiAccessPoint> wifiAccessPoints = new ArrayList<>();
        synchronized (lock) {
            long timerStart = System.currentTimeMillis();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> task = executor.submit(new Runnable() {

                @Override
                public void run() {
                    Thread.currentThread().setName(SCAN_THREAD_NAME);
                    int stat = -1;
                    iwlistScanTool.this.process = null;
                    StringBuilder sb = new StringBuilder();
                    sb.append("iwlist ").append(iwlistScanTool.this.ifaceName).append(" scanning");
                    logger.info("scan() :: executing: {}", sb.toString());
                    iwlistScanTool.this.status = false;
                    try {
                        iwlistScanTool.this.process = ProcessUtil.exec(sb.toString());
                        stat = iwlistScanTool.this.process.waitFor();
                        logger.info("scan() :: " + sb.toString() + " command returns status=" + stat + " - process="
                                + iwlistScanTool.this.process);
                        if (stat == 0) {
                            iwlistScanTool.this.status = true;
                        } else {
                            logger.error("scan() :: failed to execute {} error code is {}", sb.toString(), stat);
                        }
                    } catch (Exception e) {
                        iwlistScanTool.this.errmsg = "exception executing scan command";
                        logger.error("failed to execute the {} command ", sb.toString(), e);
                    }
                }
            });

            while (!task.isDone()) {
                if (System.currentTimeMillis() > timerStart + this.timeout * 1000) {
                    logger.warn("scan() :: scan timeout");
                    sb = new StringBuilder();
                    sb.append("iwlist ").append(this.ifaceName).append(" scanning");
                    try {
                        int pid = LinuxProcessUtil.getPid(sb.toString());
                        if (pid >= 0) {
                            logger.warn("scan() :: scan timeout :: killing pid {}", pid);
                            LinuxProcessUtil.kill(pid);
                        }
                    } catch (Exception e) {
                        logger.error("failed to get pid of the {} process ", sb.toString(), e);
                    }
                    task.cancel(true);
                    task = null;
                    this.errmsg = "timeout executing scan command";
                    break;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            if (!this.status || this.process == null) {
                throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, this.errmsg);
            }

            logger.info("scan() :: the 'iw scan' command executed successfully, parsing output ...");
            try {
                wifiAccessPoints = parse();
            } catch (Exception e) {
                throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e, "error parsing scan results");
            } finally {
                logger.info("scan() :: destroing scan proccess ...");
                if (this.process != null) {
                    ProcessUtil.destroy(this.process);
                }
                this.process = null;

                logger.info("scan() :: Terminating {} ...", SCAN_THREAD_NAME);
                executor.shutdownNow();
                try {
                    executor.awaitTermination(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted " + e);
                }
                logger.info("scan() :: 'iw scan' thread terminated? - {}", executor.isTerminated());
                executor = null;
            }
        }
        return wifiAccessPoints;
    }

    private List<WifiAccessPoint> parse() throws KuraException {

        List<WifiAccessPoint> wifiAccessPoints = new ArrayList<>();

        // get the output
        String line = null;
        String ssid = null;
        List<Long> bitrate = null;
        long frequency = -1;
        byte[] hardwareAddress = null;
        WifiMode mode = null;
        EnumSet<WifiSecurity> rsnSecurity = null;
        int strength = -1;
        EnumSet<WifiSecurity> wpaSecurity = null;
        try (InputStreamReader isr = new InputStreamReader(this.process.getInputStream());
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
                    bitrate = null;
                    frequency = -1;
                    hardwareAddress = null;
                    mode = null;
                    rsnSecurity = null;
                    strength = -1;
                    wpaSecurity = null;

                    // parse out the MAC
                    StringTokenizer st = new StringTokenizer(line, " ");
                    st.nextToken(); // eat Cell
                    st.nextToken(); // eat Cell #
                    st.nextToken(); // eat '-'
                    st.nextToken(); // eat 'Address:'
                    String macAddressString = st.nextToken();
                    if (macAddressString != null) {
                        hardwareAddress = NetworkUtil.macToBytes(macAddressString);
                    }
                } else if (line.startsWith("ESSID:")) {
                    ssid = line.substring("ESSID:".length() + 1, line.length() - 1);
                } else if (line.startsWith("Quality=")) {
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

                } else if (line.startsWith("Mode:")) {
                    line = line.substring("Mode:".length());
                    if ("Master".equals(line)) {
                        mode = WifiMode.MASTER;
                    }
                } else if (line.startsWith("Frequency:")) {
                    line = line.substring("Frequency:".length(), line.indexOf(' '));
                    frequency = (long) (Float.parseFloat(line) * 1000);
                } else if (line.startsWith("Bit Rates:")) {
                    if (bitrate == null) {
                        bitrate = new ArrayList<>();
                    }
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
}
