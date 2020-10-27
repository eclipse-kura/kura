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

import java.io.ByteArrayOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.Charsets;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.kura.net.wifi.WifiChannel;

public class IwListChannelTool {

    private static final Logger logger = LoggerFactory.getLogger(IwListChannelTool.class);

    private IwListChannelTool() {
    }

    static Pattern channelPattern = Pattern.compile(".*Channel ([0-9]+) : (.*) GHz.*");
    static Pattern countryPattern = Pattern.compile("country (..): .*");

    /*
     * Returns an empty capabilities set if the interface is not found or on error
     */
    public static List<WifiChannel> probeChannels(String ifaceName, CommandExecutorService executorService)
        throws KuraException {
        List<WifiChannel> channels = new ArrayList<>();

        // ignore logical interfaces like "1-1.2"
        if (Character.isDigit(ifaceName.charAt(0))) {
           return channels;
        }

        String[] cmd = { "iwlist", ifaceName, "channel" };
        Command command = new Command(cmd);
        command.setTimeout(60);
        command.setOutputStream(new ByteArrayOutputStream());
        CommandStatus status = executorService.execute(command);
        int exitValue = status.getExitStatus().getExitCode();
        if (!status.getExitStatus().isSuccessful()) {
           logger.warn("error executing command --- iwlist --- exit value = {}", exitValue);
           throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, String.join(" ", cmd), exitValue);
        }

        // get the output
        getWifiChannelsParse(channels,
            new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray(), Charsets.UTF_8));
        return channels;
    }

    private static void getWifiChannelsParse(List<WifiChannel> channels, String commandOutput) {
        for (String line : commandOutput.split("\n")) {
            // Remove all whitespace
            logger.debug(line);
            Matcher m = channelPattern.matcher(line);
            if (m.matches()) {
                Integer channel = Integer.valueOf(m.group(1));
                Float frequency = Float.valueOf(m.group(2));
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
        String[] line = commandOutput.split("\n");
        logger.info("Get Wifi Country Code Output = " + line[0]);
        Matcher m = countryPattern.matcher(line[0]);
        if (m.matches()) {
            String country = m.group(1);
            logger.info("Country Code = " + country);
            return country;
        }
        return "Unknown";
    }
}
