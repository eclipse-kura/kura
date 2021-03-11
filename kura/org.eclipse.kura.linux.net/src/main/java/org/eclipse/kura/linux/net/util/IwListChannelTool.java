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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.kura.net.wifi.WifiChannel;

public class IwListChannelTool extends IwCapabilityTool {

    private static final Logger logger = LoggerFactory.getLogger(IwListChannelTool.class);

    private IwListChannelTool() {
    }

    private static final Pattern COUNTRY_PATTERN = Pattern.compile("country (..): .*");
    private static final Pattern FREQUENCY_CHANNEL_PATTERN = Pattern.compile(".*\\* ([0-9]+) MHz \\[([0-9]*)\\] \\((.*) dBm\\)$");

    public static List<WifiChannel> probeChannels(String ifaceName, CommandExecutorService executorService)
            throws KuraException {
        try {
            List<WifiChannel> channels = new ArrayList<>();

            // ignore logical interfaces like "1-1.2"
            if (Character.isDigit(ifaceName.charAt(0))) {
               return channels;
            }
            
            final int phy = parseWiphyIndex(exec(new String[] { "iw", ifaceName, "info" }, executorService))
                    .orElseThrow(() -> new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR,
                            "failed to get phy index for " + ifaceName));
            parseWifiChannelFrequency(exec(new String[] { "iw", "phy" + String.valueOf(phy), "info" }, executorService), channels);
            return channels;
        } catch (final KuraException e) {
            throw e;
        } catch (final Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        }
    }

    private static void parseWifiChannelFrequency(final InputStream in, List<WifiChannel> channels) {
        String result = new BufferedReader(new InputStreamReader(in))
               .lines().collect(Collectors.joining("\n"));
        for (String line : result.split("\n")) {
            logger.debug(line);
            Matcher m = FREQUENCY_CHANNEL_PATTERN.matcher(line);
            if (m.matches()) {
                Integer frequency = Integer.valueOf(m.group(1));
                Integer channel = Integer.valueOf(m.group(2));
                WifiChannel wc = new WifiChannel(channel, frequency);
                channels.add(wc);
                logger.debug(wc.toString());
            }
        }
    }

    /*
     * Returns the Wifi Country Code
     */
    public static String getWifiCountryCode(CommandExecutorService executorService)
        throws KuraException {
        String[] cmd = { "iw", "reg", "get"};
        Command command = new Command(cmd);
        command.setTimeout(60);
        command.setOutputStream(new ByteArrayOutputStream());
        CommandStatus status = executorService.execute(command);
        int exitValue = status.getExitStatus().getExitCode();
        if (!status.getExitStatus().isSuccessful()) {
           logger.warn("error executing command --- iw reg get --- exit value = {}", exitValue);
           throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, String.join(" ", cmd), exitValue);
        }
        String commandOutput = new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray());
        for (String line:commandOutput.split("\n")) {
            logger.info("Get Wifi Country Code Output = " + line);
            Matcher m = COUNTRY_PATTERN.matcher(line);
            if (m.matches()) {
                String country = m.group(1);
                logger.info("Country Code = " + country);
                return country;
            }
        }
        return "Unknown";
    }
}
