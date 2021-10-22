/*******************************************************************************
 * Copyright (c) 2019, 2021 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 *  Sterwen-Technology
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
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.net.wifi.WifiChannel;
import org.eclipse.kura.net.wifi.WifiInterface.Capability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IwCapabilityTool {

    private static final Logger logger = LoggerFactory.getLogger(IwCapabilityTool.class);

    private static final Pattern WIPHY_PATTERN = Pattern.compile("^\\twiphy (\\d+)$");
    private static final Pattern SUPPORTED_CHIPERS_PATTERN = Pattern.compile("^\\tSupported Ciphers:$");
    private static final Pattern CHIPHER_CAPABILITY_PATTERN = Pattern.compile("^\\t\\t\\* ([\\w-\\d]+).*$");
    private static final Pattern RSN_CAPABILITY_PATTERN = Pattern.compile("^\tDevice supports RSN.*$");
    private static final Pattern COUNTRY_PATTERN = Pattern.compile("country (..): .*");

    private static final Pattern FREQUENCY_CHANNEL_PATTERN = Pattern
            .compile("^\\* ([0-9]+) MHz \\[([0-9]+)\\]( \\((.+) dBm\\)){0,1}.*$");

    private enum ParseState {
        HAS_RSN,
        HAS_CHIPHERS,
        PARSE_CHIPHERS
    }

    private static final EnumSet<ParseState> DONE = EnumSet.of(ParseState.HAS_RSN, ParseState.HAS_CHIPHERS);

    protected IwCapabilityTool() {

    }

    private static Optional<Matcher> skipTo(final BufferedReader reader, final Pattern pattern) throws IOException {
        String line;

        while ((line = reader.readLine()) != null) {

            final Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                return Optional.of(matcher);
            }

        }

        return Optional.empty();
    }

    protected static Optional<Integer> parseWiphyIndex(final InputStream in) throws IOException {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            return skipTo(reader, WIPHY_PATTERN).map(matcher -> Integer.parseInt(matcher.group(1)));
        }
    }

    private static Optional<Capability> parseChipherCapability(final String cipherString) {
        if ("WEP40".contentEquals(cipherString)) {
            return Optional.of(Capability.CIPHER_WEP40);
        } else if ("WEP104".contentEquals(cipherString)) {
            return Optional.of(Capability.CIPHER_WEP104);
        } else if ("TKIP".contentEquals(cipherString)) {
            return Optional.of(Capability.CIPHER_TKIP);
        } else if (cipherString.contains("CCMP")) {
            return Optional.of(Capability.CIPHER_CCMP);
        }
        return Optional.empty();
    }

    @SuppressWarnings("checkstyle:innerAssignment")
    private static Set<Capability> parseCapabilities(final InputStream in) throws IOException {

        final EnumSet<Capability> capabilities = EnumSet.noneOf(Capability.class);
        final EnumSet<ParseState> parseState = EnumSet.noneOf(ParseState.class);

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

            String line;

            while (!parseState.containsAll(DONE) && (line = reader.readLine()) != null) {

                if (parseState.contains(ParseState.PARSE_CHIPHERS)) {
                    final Matcher matcher = CHIPHER_CAPABILITY_PATTERN.matcher(line);

                    if (matcher.matches()) {
                        parseChipherCapability(matcher.group(1)).ifPresent(capabilities::add);
                        continue;
                    } else {
                        parseState.remove(ParseState.PARSE_CHIPHERS);
                        parseState.add(ParseState.HAS_CHIPHERS);
                    }
                }

                if (!parseState.contains(ParseState.HAS_RSN) && RSN_CAPABILITY_PATTERN.matcher(line).matches()) {
                    capabilities.add(Capability.RSN);
                    parseState.add(ParseState.HAS_RSN);
                }

                if (!parseState.contains(ParseState.HAS_CHIPHERS)
                        && SUPPORTED_CHIPERS_PATTERN.matcher(line).matches()) {
                    parseState.add(ParseState.PARSE_CHIPHERS);
                }

            }

        }

        // best effort guess
        if (capabilities.contains(Capability.CIPHER_TKIP)) {
            capabilities.add(Capability.WPA);
        }

        return capabilities;
    }

    public static InputStream exec(final String[] commandLine, CommandExecutorService executorService)
            throws KuraException {
        Command command = new Command(commandLine);
        command.setOutputStream(new ByteArrayOutputStream());
        CommandStatus status = executorService.execute(command);
        final int exitValue = status.getExitStatus().getExitCode();

        if (!status.getExitStatus().isSuccessful()) {
            logger.warn("error executing command --- {} --- exit value = {}", commandLine, exitValue);
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, commandLine, exitValue);
        }

        return new ByteArrayInputStream(((ByteArrayOutputStream) status.getOutputStream()).toByteArray());

    }

    public static Set<Capability> probeCapabilities(final String interfaceName, CommandExecutorService executorService)
            throws KuraException {
        try {

            final int phy = parseWiphyIndex(exec(new String[] { "iw", interfaceName, "info" }, executorService))
                    .orElseThrow(() -> new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR,
                            "failed to get phy index for " + interfaceName));
            return parseCapabilities(exec(new String[] { "iw", "phy" + phy, "info" }, executorService));

        } catch (final KuraException e) {
            throw e;
        } catch (final Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        }
    }

    /**
     * Get the list of Wifi channels and frequencies
     * 
     * @since 2.2
     **/
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
            parseWifiChannelFrequency(exec(new String[] { "iw", "phy" + phy, "info" }, executorService), channels);
            return channels;
        } catch (final KuraException e) {
            throw e;
        } catch (final Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        }
    }

    private static void parseWifiChannelFrequency(final InputStream in, List<WifiChannel> channels) {
        String result = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        for (String line : result.split("\n")) {
            logger.debug(line);
            String trimmedLine = line.trim();
            Matcher m = FREQUENCY_CHANNEL_PATTERN.matcher(trimmedLine);
            if (m.matches()) {
                Integer frequency = Integer.valueOf(m.group(1));
                Integer channel = Integer.valueOf(m.group(2));
                Float attenuation = m.group(4) != null ? Float.valueOf(m.group(4)) : 0.0f;
                Boolean disabled = line.contains("disabled");

                Boolean noIR = line.contains("no IR");
                Boolean radarDetection = line.contains("radar detection");

                WifiChannel wc = new WifiChannel(channel, frequency);
                wc.setAttenuation(attenuation);
                wc.setDisabled(disabled);
                wc.setNoInitiatingRadiation(noIR);
                wc.setRadarDetection(radarDetection);

                channels.add(wc);
                logger.debug("Wifi channel = {}", wc);
            }
        }
    }

    /**
     * Get the Wifi Country Code
     * 
     * @since 2.2
     */
    public static String getWifiCountryCode(CommandExecutorService executorService) throws KuraException {
        String[] cmd = { "iw", "reg", "get" };
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
        for (String line : commandOutput.split("\n")) {
            logger.info("Get Wifi Country Code Output = {}", line);
            Matcher m = COUNTRY_PATTERN.matcher(line);
            if (m.matches()) {
                String country = m.group(1);
                logger.debug("Country Code = {}", country);
                return country;
            }
        }
        return "Unknown";
    }
}
