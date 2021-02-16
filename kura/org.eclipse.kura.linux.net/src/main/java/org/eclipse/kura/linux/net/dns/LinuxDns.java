/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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

    private static final String DNS_FILE_NAME = "/etc/resolv.conf";
    private static final String[] PPP_DNS_FILES = { "/var/run/ppp/resolv.conf", "/etc/ppp/resolv.conf" };
    private static final String BACKUP_DNS_FILE_NAME = "/etc/resolv.conf.save";
    private static final int COMMAND_TIMEOUT = 60;
    private static final String NAMESERVER = "nameserver";

    private static LinuxDns linuxDns = null;

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
                writeDnsFile(new HashSet<IPAddress>());
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
        File file = new File(DNS_FILE_NAME);
        boolean ret = false;
        if (isSymlink(file) && getRealPath(file).equals(getPppDnsFileName())) {
            return true;
        }
        return ret;
    }

    private void backupDnsFile(CommandExecutorService executorService) throws KuraException {
        File file = new File(DNS_FILE_NAME);
        if (file.exists()) {
            Command command = new Command(new String[] { "mv", DNS_FILE_NAME, BACKUP_DNS_FILE_NAME });
            command.setTimeout(COMMAND_TIMEOUT);
            CommandStatus status = executorService.execute(command);
            if (!status.getExitStatus().isSuccessful()) {
                logger.error("Failed to move the {} file to {}. The 'mv' command has failed ...", DNS_FILE_NAME,
                        BACKUP_DNS_FILE_NAME);
                throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR, "Failed to backup DNS file " + DNS_FILE_NAME);
            } else {
                logger.info("successfully backed up {}", DNS_FILE_NAME);
            }
        }
    }

    private void setDnsPppLink(String sPppDnsFileName, CommandExecutorService executorService) throws KuraException {
        if (!new File(sPppDnsFileName).setReadable(true, false)) {
            logger.debug("failed to set permissions to {}", sPppDnsFileName);
        }

        Command command = new Command(new String[] { "ln", "-sf", sPppDnsFileName, DNS_FILE_NAME });
        command.setTimeout(COMMAND_TIMEOUT);
        CommandStatus status = executorService.execute(command);
        if (!status.getExitStatus().isSuccessful()) {
            logger.error("failed to create symbolic link: {} -> {}", DNS_FILE_NAME, sPppDnsFileName);
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR,
                    "failed to create symbolic link to " + sPppDnsFileName);
        } else {
            logger.info("DNS is set to use ppp resolv.conf");
        }
    }

    private void unsetDnsPppLink(String pppDnsFilename, CommandExecutorService executorService) throws KuraException {
        File file = new File(DNS_FILE_NAME);
        if (file.exists()) {
            Command command = new Command(new String[] { "rm", DNS_FILE_NAME });
            command.setTimeout(COMMAND_TIMEOUT);
            CommandStatus status = executorService.execute(command);
            if (!status.getExitStatus().isSuccessful()) {
                logger.error("failed to delete {} symlink that points to {}", DNS_FILE_NAME, pppDnsFilename);
                throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR,
                        "failed to delete " + DNS_FILE_NAME + " symlink that points to " + pppDnsFilename);
            } else {
                logger.info("successfully removed symlink that points to {}", pppDnsFilename);
            }
        }
    }

    private void restoreDnsFile(CommandExecutorService executorService) throws KuraException {
        File file = new File(BACKUP_DNS_FILE_NAME);
        if (file.exists()) {
            restoreDnsFileFromBackup(executorService);
        } else {
            createEmptyDnsFile();
        }
    }

    private void restoreDnsFileFromBackup(CommandExecutorService executorService) throws KuraException {
        Command command = new Command(new String[] { "mv", BACKUP_DNS_FILE_NAME, DNS_FILE_NAME });
        command.setTimeout(COMMAND_TIMEOUT);
        CommandStatus status = executorService.execute(command);
        if (!status.getExitStatus().isSuccessful()) {
            logger.error("failed to restore {} to {}", BACKUP_DNS_FILE_NAME, DNS_FILE_NAME);
            throw new KuraException(KuraErrorCode.OS_COMMAND_ERROR,
                    "failed to restore " + BACKUP_DNS_FILE_NAME + " to " + DNS_FILE_NAME);
        } else {
            logger.info("successfully restored {} from {}", DNS_FILE_NAME, BACKUP_DNS_FILE_NAME);
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
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
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
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
        }
        return path;
    }

    private boolean isLinkToPppDnsFile() throws IOException {
        final String pppDnsFileName = getPppDnsFileName();

        if (pppDnsFileName == null) {
            return false;
        }

        return Files.readSymbolicLink(Paths.get(DNS_FILE_NAME)).equals(Paths.get(pppDnsFileName));
    }

    private synchronized void writeDnsFile(Set<IPAddress> servers) {
        logger.debug("Writing DNS servers to file");
        try {
            if (Files.isSymbolicLink(Paths.get(DNS_FILE_NAME)) && !isLinkToPppDnsFile()) {
                Files.delete(Paths.get(DNS_FILE_NAME));
            }
        } catch (IOException e) {
            logger.error("Cannot read symlink", e);
        }

        try (FileOutputStream fos = new FileOutputStream(getOrCreateDnsFile());
                PrintWriter pw = new PrintWriter(fos);) {
            String[] existingFile = getModifiedFile();
            for (String element : existingFile) {
                pw.write(element + "\n");
            }
            pw.write("\n");
            for (IPAddress server : servers) {
                pw.write("nameserver " + server.getHostAddress() + "\n");
            }
            pw.flush();
            fos.getFD().sync();
        } catch (Exception e1) {
            logger.error("writeDnsFile() :: failed to write the {} file ", DNS_FILE_NAME, e1);
        }
    }

    private synchronized String[] getModifiedFile() {
        File dnsFile;
        try {
            dnsFile = getOrCreateDnsFile();
        } catch (final Exception e) {
            return new String[0];
        }

        ArrayList<String> linesWithoutServers = new ArrayList<>();
        try (FileReader fr = new FileReader(dnsFile); BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.indexOf(NAMESERVER) != 0) {
                    linesWithoutServers.add(line);
                }
            }
        } catch (Exception e) {
            logger.error("error parsing the {} file ", DNS_FILE_NAME, e);
        }

        String[] lines = new String[linesWithoutServers.size()];
        for (int i = 0; i < linesWithoutServers.size(); i++) {
            lines[i] = linesWithoutServers.get(i);
        }
        return lines;
    }

    private File getOrCreateDnsFile() throws KuraException {
        File f = new File(DNS_FILE_NAME);
        if (!f.exists()) {
            try {
                if (f.createNewFile()) {
                    logger.debug("The {} doesn't exist, created new empty file ...", DNS_FILE_NAME);
                }
            } catch (Exception e) {
                logger.error("Failed to create new empty {} file", DNS_FILE_NAME, e);
                throw new KuraException(KuraErrorCode.IO_ERROR);
            }
        }
        if (!f.setReadable(true, false)) {
            logger.debug("failed to set permissions to {}", DNS_FILE_NAME);
        }
        return f;
    }

    private String getPppDnsFileName() {

        String pppDnsFileName = null;
        for (String name : PPP_DNS_FILES) {
            File file = new File(name);
            if (file.exists()) {
                pppDnsFileName = name;
                break;
            }
        }

        return pppDnsFileName;
    }
}
