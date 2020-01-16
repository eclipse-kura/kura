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

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.apache.commons.io.Charsets;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.net.NetInterfaceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpAddrShow {

    private static final Logger logger = LoggerFactory.getLogger(IpAddrShow.class);

    private String ifaceName;
    private CommandExecutorService executorService;
    private final ArrayList<LinuxIfconfig> configs = new ArrayList<>();

    public IpAddrShow(CommandExecutorService executoService) {
        this.executorService = executoService;
    }

    public IpAddrShow(String interfaceName, CommandExecutorService executoService) {
        this.ifaceName = interfaceName;
        this.executorService = executoService;
    }

    // ifconfig sucks. ip sucks too but at least the output is consistent.
    // Unfortunately 'ip' does not have the equivalent of 'ifconfig -a':
    // 'ip addr show' does not show interfaces that are DOWN (meaning 'ifdown')
    // 'ip link show' or 'ip -0 addr show' does not show inet addresses
    // this means that we need to call 'ip' twice to get all the information:
    // 'ip link show' followed by 'ip -4 addr show' and merge the two outputs.
    public LinuxIfconfig[] exec() throws KuraException {
        execLink();
        execInet();
        return this.configs.toArray(new LinuxIfconfig[0]);
    }

    private void execLink() throws KuraException {
        StringBuilder sb = new StringBuilder("ip -o link show");
        if (this.ifaceName != null) {
            sb.append(" dev ").append(this.ifaceName);
        }
        String[] cmd = sb.toString().split("\\s+");
        Command command = new Command(cmd);
        command.setTimeout(60);
        command.setOutputStream(new ByteArrayOutputStream());
        CommandStatus status = this.executorService.execute(command);
        if (!status.getExitStatus().isSuccessful()) {
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, sb.toString(),
                    status.getExitStatus().getExitCode());
        }
        parseExecLink(new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray(), Charsets.UTF_8));
    }

    private void parseExecLink(String commandOutput) {
        for (String line : commandOutput.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            logger.debug(line);

            String ifname = findIfname(line);
            logger.debug("ifname: '{}'", ifname);
            if (ifname == null) {
                continue;
            }
            LinuxIfconfig config = new LinuxIfconfig(ifname);

            String[] flags = findFlags(line);
            boolean multicast = isMulticast(flags);
            logger.debug("multicast: '{}'", multicast);
            config.setMulticast(multicast);

            boolean up = isUp(flags);
            logger.debug("up: '{}'", up);
            config.setUp(up);

            String smtu = findValue(line, "mtu", " ");
            logger.debug("mtu: '{}'", smtu);
            if (smtu != null) {
                int mtu = Integer.parseInt(smtu);
                config.setMtu(mtu);
            }

            String linkState = findValue(line, "state", " ");
            logger.debug("link state: '{}'", linkState);

            // Some interfaces, like ppp0 report the link state as UNKNOWN.
            // In this case we consider the link up.
            config.setLinkUp(!"DOWN".equals(linkState));

            String link = findValue(line, "link", "/| ");
            logger.debug("link: '{}'", link);
            if ("loopback".equals(link)) {
                config.setType(NetInterfaceType.LOOPBACK);
            } else if ("ether".equals(link)) {
                config.setType(NetInterfaceType.ETHERNET);
            } else if ("ppp".equals(link)) {
                config.setType(NetInterfaceType.MODEM);
            } else {
                config.setType(NetInterfaceType.UNKNOWN);
            }

            String hwaddr = findValue(line, "link/" + link, " ");
            logger.debug("hwaddr: '{}'", hwaddr);
            config.setMacAddress(hwaddr); // can be null

            this.configs.add(config);
        }
    }

    private void execInet() throws KuraException {
        StringBuilder sb = new StringBuilder("ip -o -4 addr show");
        if (this.ifaceName != null) {
            sb.append(" dev ").append(this.ifaceName);
        }
        String[] cmd = sb.toString().split("\\s+");
        Command command = new Command(cmd);
        command.setTimeout(60);
        command.setOutputStream(new ByteArrayOutputStream());
        CommandStatus status = this.executorService.execute(command);
        if (!status.getExitStatus().isSuccessful()) {
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, sb.toString(),
                    status.getExitStatus().getExitCode());
        }
        parseExecInet(new String(((ByteArrayOutputStream) status.getOutputStream()).toByteArray(), Charsets.UTF_8));
    }

    private void parseExecInet(String commandOutput) throws KuraException {
        // FIXME: Note that one interface might have multiple inet addresses
        // while this implementation assumes a single inet address.
        try {
            for (String line : commandOutput.split("\n")) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                logger.debug(line);

                String ifname = findIfname(line);
                logger.debug("ifname: '{}'", ifname);
                if (ifname == null) {
                    logger.warn("Interface name not found");
                    continue;
                }

                LinuxIfconfig config = null;
                for (LinuxIfconfig conf : this.configs) {
                    if (conf.getName().equals(ifname)) {
                        config = conf;
                        break;
                    }
                }

                if (config == null) {
                    logger.warn("Config for interface {} not found", ifname);
                    continue;
                }

                String inet = findValue(line, "inet", " ");
                logger.debug("inet: '{}'", inet);

                if (inet != null) {
                    parseInet(config, inet);
                }

                String bcast = findValue(line, "brd", " ");
                logger.debug("bcast: '{}'", bcast);
                config.setInetBcast(bcast);

                String peer = findValue(line, "peer", " ");
                logger.debug("peer: '{}'", peer);
                config.setPeerInetAddr(peer);
            }
        } catch (Exception e) {
            throw new KuraException(KuraErrorCode.PROCESS_EXECUTION_ERROR, e);
        }
    }

    private void parseInet(LinuxIfconfig config, String inet) throws UnknownHostException {
        String[] parts = inet.split("/");
        if (parts.length > 0) {
            String inetaddr = parts[0];
            config.setInetAddress(inetaddr);
            String sprefix = null;
            if (parts.length > 1) {
                sprefix = parts[1];
            }
            int prefix = 32;
            if (sprefix != null) {
                prefix = Integer.valueOf(sprefix);
            }
            String inetmask = prefix2inetmask(prefix);
            config.setInetMask(inetmask);
        }
    }

    private String findIfname(String input) {
        int start = input.indexOf(':');
        if (start < 0) {
            return null;
        }
        int end = input.indexOf(' ', start + 2);
        if (end < 0) {
            return null;
        }
        if (input.charAt(end - 1) == ':') {
            end--;
        }
        return input.substring(start + 2, end);
    }

    private String findValue(String input, String name, String regex) {
        int start = input.indexOf(name);
        if (start < 0) {
            return null;
        }
        String[] parts = input.substring(start).split(regex);
        if (parts.length > 1) {
            return parts[1];
        }
        return null;
    }

    private String[] findFlags(String input) {
        int start = input.indexOf('<');
        if (start < 0) {
            return new String[0];
        }
        int end = input.indexOf('>', start + 1);
        if (end < 0) {
            return new String[0];
        }
        String flags = input.substring(start + 1, end).trim();
        return flags.split(",");
    }

    private boolean isMulticast(String[] flags) {
        boolean multicast = false;
        for (String flag : flags) {
            if ("MULTICAST".equals(flag)) {
                multicast = true;
                break;
            }
        }
        return multicast;
    }

    private boolean isUp(String[] flags) {
        boolean up = false;
        for (String flag : flags) {
            if ("UP".equals(flag)) {
                up = true;
                break;
            }
        }
        return up;
    }

    private static String prefix2inetmask(int prefix) throws UnknownHostException {
        int mask = 0xffffffff << 32 - prefix;

        int value = mask;
        byte[] bytes = new byte[] { (byte) (value >>> 24), (byte) (value >> 16 & 0xff), (byte) (value >> 8 & 0xff),
                (byte) (value & 0xff) };

        InetAddress netAddr = InetAddress.getByAddress(bytes);
        return netAddr.toString().substring(1); // strip the leading '/'
    }
}
