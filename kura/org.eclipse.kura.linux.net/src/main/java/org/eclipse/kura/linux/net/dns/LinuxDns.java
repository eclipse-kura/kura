/*******************************************************************************
 * Copyright (c) 2011, 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *  Jens Reimann <jreimann@redhat.com> 
 *******************************************************************************/
package org.eclipse.kura.linux.net.dns;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.executor.Command;
import org.eclipse.kura.executor.CommandExecutorService;
import org.eclipse.kura.executor.CommandStatus;
import org.eclipse.kura.linux.net.dhcp.DhcpClientLeases;
import org.eclipse.kura.net.IPAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinuxDns {

    private static final Logger logger = LoggerFactory.getLogger(LinuxDns.class);

    private static final String BACKUP_DNS_FILE_NAME_SUFFIX = ".save";
    private static final int COMMAND_TIMEOUT = 60;
    private static final String NAMESERVER = "nameserver";

    private static LinuxDns linuxDns = null;
    private final String dnsFileName;
    private final String[] pppDnsFileNames;

    LinuxDns(String dnsFile, String[] pppDnsFiles) {
        dnsFileName = dnsFile;
        pppDnsFileNames = pppDnsFiles;
    }

    private LinuxDns() {
        dnsFileName = "/etc/resolv.conf";
        String[] pppFiles = { "/var/run/ppp/resolv.conf", "/etc/ppp/resolv.conf" };
        pppDnsFileNames = pppFiles;
    }

    public static synchronized LinuxDns getInstance() {
        if (linuxDns == null) {
            linuxDns = new LinuxDns();
        }

        return linuxDns;
    }

    public synchronized Set<IPAddress> getDnServers() {
        Set<IPAddress> servers = new HashSet<>();
        File f;
        try {
            f = getOrCreateDnsFile();
        } catch (final KuraException e) {
            return servers;
        }

        try (FileReader fr = new FileReader(f); BufferedReader br = new BufferedReader(fr)) {
            String line = null;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.indexOf(NAMESERVER) == 0) {
                    StringTokenizer st = new StringTokenizer(line);
                    st.nextToken();
                    servers.add(IPAddress.parseHostAddress(st.nextToken()));
                }
            }
            if (servers.isEmpty()) {
                logger.debug("No DNS servers found ...");
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        return servers;
    }

    public synchronized void setDnServers(Set<IPAddress> servers) {
        if (servers == null) {
            if (getDnServers() != null) {
                writeDnsFile(new HashSet<>());
            }
        } else if (!servers.equals(getDnServers())) {
            writeDnsFile(servers);
        }
    }

    public synchronized List<IPAddress> getPppDnServers() throws KuraException {
        ArrayList<IPAddress> serversList = new ArrayList<>();
        String pppDnsFileName = getPppDnsFileName();
        if (pppDnsFileName != null) {
            File pppDnsFile = new File(pppDnsFileName);
            try (FileReader fr = new FileReader(pppDnsFile); BufferedReader br = new BufferedReader(fr)) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.indexOf(NAMESERVER) == 0) {
                        StringTokenizer st = new StringTokenizer(line);
                        st.nextToken();
                        serversList.add(IPAddress.parseHostAddress(st.nextToken()));
                    }
                }
            } catch (Exception e) {
                throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
            }
        }
        return serversList;
    }

    public List<IPAddress> getDhcpDnsServers(String interfaceName, String address) throws KuraException {
        return DhcpClientLeases.getInstance().getDhcpDnsServers(interfaceName, address);
    }

    public List<IPAddress> getDhcpDnsServers(String interfaceName, IPAddress address) throws KuraException {
        return DhcpClientLeases.getInstance().getDhcpDnsServers(interfaceName, address);
    }

    public synchronized void removeDnsServer(IPAddress serverIpAddress) {
        try {
            if (isPppDnsSet()) {
                return;
            }
        } catch (Exception e) {
            logger.error("failed to remove DNS server ", e);
        }

        Set<IPAddress> servers = getDnServers();
        Set<IPAddress> newServers = new HashSet<>();

        for (IPAddress server : servers) {
            if (server.equals(serverIpAddress)) {
                logger.info("removed the DNS server: {}", serverIpAddress);
            } else {
                // keep it for the new Dns file
                newServers.add(server);
            }
        }

        writeDnsFile(newServers);
    }

    public synchronized void addDnsServer(IPAddress serverIpAddress) {
        try {
            if (isPppDnsSet()) {
                return;
            }
        } catch (Exception e) {
            logger.error("failed to add DNS server ", e);
        }

        Set<IPAddress> servers = getDnServers();

        if (servers == null) {
            servers = new HashSet<>();
        }
        servers.add(serverIpAddress);

        writeDnsFile(servers);
    }

    public synchronized void setPppDns(CommandExecutorService executorService) throws KuraException {
        String sPppDnsFileName = getPppDnsFileName();
        if (sPppDnsFileName == null || isPppDnsSet()) {
            return;
        }
        backupDnsFile(executorService);
        setDnsPppLink(sPppDnsFileName, executorService);
    }

    public synchronized void unsetPppDns(CommandExecutorService executorService) throws KuraException {
        if (isPppDnsSet()) {
            String pppDnsFilename = getPppDnsFileName();
            unsetDnsPppLink(pppDnsFilename, executorService);
            restoreDnsFile(executorService);

            // remove actual PPP DNS file
            File pppDnsFile = new File(pppDnsFilename);
            if (pppDnsFile.exists() && pppDnsFile.delete()) {
                logger.debug("PPP DNS file - {} has been removed", pppDnsFilename);
            }
        }
    }

    public synchronized boolean isPppDnsSet() throws KuraException {
        File file = new File(dnsFileName);
        boolean ret = false;
        if (isSymlink(file) && getRealPath(file).equals(getPppDnsFileName())) {
            return true;
        }
        return ret;
    }

    private void backupDnsFile(CommandExecutorService executorService) throws KuraException {
        File file = new File(dnsFileName);
        if (file.exists()) {
            Command command = new Command(
                    new String[] { "mv", dnsFileName, dnsFileName + BACKUP_DNS_FILE_NAME_SUFFIX });
            command.setTimeout(COMMAND_TIMEOUT);
            CommandStatus status = executorService.execute(command);
            if (!status.getExitStatus().isSuccessful()) {
                logger.error("Failed to move the {} file to {}{}. The 'mv' command has failed ...", dnsFileName,
                        dnsFileName, BACKUP_DNS_FILE_NAME_SUFFIX);
                throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, "Failed to backup DNS file " + dnsFileName);
            } else {
                logger.info("successfully backed up {}", dnsFileName);
            }
        }
    }

    private void setDnsPppLink(String sPppDnsFileName, CommandExecutorService executorService) throws KuraException {
        if (!new File(sPppDnsFileName).setReadable(true, false)) {
            logger.debug("failed to set permissions to {}", sPppDnsFileName);
        }

        Command command = new Command(new String[] { "ln", "-sf", sPppDnsFileName, dnsFileName });
        command.setTimeout(COMMAND_TIMEOUT);
        CommandStatus status = executorService.execute(command);
        if (!status.getExitStatus().isSuccessful()) {
            logger.error("failed to create symbolic link: {} -> {}", dnsFileName, sPppDnsFileName);
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR,
                    "failed to create symbolic link to " + sPppDnsFileName);
        } else {
            logger.info("DNS is set to use ppp resolv.conf");
        }
    }

    private void unsetDnsPppLink(String pppDnsFilename, CommandExecutorService executorService) throws KuraException {
        File file = new File(dnsFileName);
        if (file.exists()) {
            Command command = new Command(new String[] { "rm", dnsFileName });
            command.setTimeout(COMMAND_TIMEOUT);
            CommandStatus status = executorService.execute(command);
            if (!status.getExitStatus().isSuccessful()) {
                logger.error("failed to delete {} symlink that points to {}", dnsFileName, pppDnsFilename);
                throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR,
                        "failed to delete " + dnsFileName + " symlink that points to " + pppDnsFilename);
            } else {
                logger.info("successfully removed symlink that points to {}", pppDnsFilename);
            }
        }
    }

    private void restoreDnsFile(CommandExecutorService executorService) throws KuraException {
        File file = new File(dnsFileName + BACKUP_DNS_FILE_NAME_SUFFIX);
        if (file.exists()) {
            restoreDnsFileFromBackup(executorService);
        } else {
            createEmptyDnsFile();
        }
    }

    private void restoreDnsFileFromBackup(CommandExecutorService executorService) throws KuraException {
        Command command = new Command(new String[] { "mv", dnsFileName + BACKUP_DNS_FILE_NAME_SUFFIX, dnsFileName });
        command.setTimeout(COMMAND_TIMEOUT);
        CommandStatus status = executorService.execute(command);
        if (!status.getExitStatus().isSuccessful()) {
            logger.error("failed to restore {}{} to {}", dnsFileName, BACKUP_DNS_FILE_NAME_SUFFIX, dnsFileName);
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR,
                    "failed to restore " + dnsFileName + BACKUP_DNS_FILE_NAME_SUFFIX + " to " + dnsFileName);
        } else {
            logger.info("successfully restored {}{} from {}", dnsFileName, dnsFileName, BACKUP_DNS_FILE_NAME_SUFFIX);
        }
    }

    private void createEmptyDnsFile() throws KuraException {
        getOrCreateDnsFile();
    }

    private synchronized boolean isSymlink(File file) throws KuraException {
        boolean ret = false;
        File canon;
        try {
            if (file.getParent() == null) {
                canon = file;
            } else {
                File canonDir = file.getParentFile().getCanonicalFile();
                canon = new File(canonDir, file.getName());
            }

            ret = !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
        } catch (Exception e) {
            logger.error("isSymlink call failed", e);
            throw new KuraException(KuraErrorCode.IO_ERROR);
        }
        return ret;
    }

    private synchronized String getRealPath(File file) throws KuraException {
        String path = null;
        File canon;
        try {
            if (file.getParent() == null) {
                canon = file;
            } else {
                File canonDir = file.getParentFile().getCanonicalFile();
                canon = new File(canonDir, file.getName());
            }

            path = canon.getCanonicalFile().toString();
        } catch (Exception e) {
            logger.error("getRealPath call failed", e);
            throw new KuraException(KuraErrorCode.IO_ERROR);
        }
        return path;
    }

    private boolean isLinkToPppDnsFile() throws IOException {
        final String pppDnsFileName = getPppDnsFileName();

        if (pppDnsFileName == null) {
            return false;
        }

        return Files.readSymbolicLink(Paths.get(dnsFileName)).equals(Paths.get(pppDnsFileName));
    }

    private synchronized void writeDnsFile(Set<IPAddress> servers) {
        logger.debug("Writing DNS servers to file");
        List<String> lines = new ArrayList<>();
        try {
            if (Files.isSymbolicLink(Paths.get(dnsFileName)) && !isLinkToPppDnsFile()) {
                Files.delete(Paths.get(dnsFileName));
            }
            lines.addAll(readDnsFileExceptNameservers());
            servers.forEach(server -> lines.add("nameserver " + server.getHostAddress()));
            writeLinesToDnsFile(lines);
        } catch (IOException e) {
            logger.error("Failed to write dns servers to file {}", dnsFileName, e);
        }
    }

    private void writeLinesToDnsFile(List<String> lines) {
        try (FileOutputStream fos = new FileOutputStream(getOrCreateDnsFile());
                PrintWriter pw = new PrintWriter(fos);) {
            lines.forEach(line -> pw.write(line + "\n"));
            pw.flush();
            fos.getFD().sync();
        } catch (IOException | KuraException e) {
            logger.error("writeDnsFile() :: failed to write the {} file ", dnsFileName, e);
        }
    }

    private synchronized List<String> readDnsFileExceptNameservers() {
        File dnsFile;
        try {
            dnsFile = getOrCreateDnsFile();
        } catch (final Exception e) {
            return new ArrayList<>();
        }

        ArrayList<String> lines = new ArrayList<>();
        try (FileReader fr = new FileReader(dnsFile); BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.toLowerCase().contains(NAMESERVER)) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            logger.error("error parsing the {} file ", dnsFileName, e);
        }
        return lines;
    }

    private File getOrCreateDnsFile() throws KuraException {
        File f = new File(dnsFileName);
        if (!f.exists()) {
            try {
                if (f.createNewFile()) {
                    logger.debug("The {} doesn't exist, created new empty file ...", dnsFileName);
                }
            } catch (IOException e) {
                logger.error("Failed to create new empty {} file", dnsFileName, e);
                throw new KuraException(KuraErrorCode.IO_ERROR);
            }
        }
        if (!f.setReadable(true, false)) {
            logger.debug("failed to set permissions to {}", dnsFileName);
        }
        return f;
    }

    private String getPppDnsFileName() {

        String pppDnsFileName = null;
        for (String name : pppDnsFileNames) {
            File file = new File(name);
            if (file.exists()) {
                pppDnsFileName = name;
                break;
            }
        }

        return pppDnsFileName;
    }
}
