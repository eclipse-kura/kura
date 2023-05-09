/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.linux.net.dns;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.kura.net.IPAddress;
import org.junit.After;
import org.junit.Test;

public class LinuxDnsTest {

    private static final String RESOLV_FILE_PATH = "/tmp/resolv.conf";
    private static final String RESOLV_FILE_CONTENT = "search example.com\nnameserver 1.2.3.4\n";

    private LinuxDns linuxDns;

    @After
    public void tearDown() {
        deleteTemporaryFile();
    }

    @Test
    public void shouldAddOnlyNameserversInResolvFile() throws IOException {
        givenResolvFile(RESOLV_FILE_CONTENT);
        givenLinuxDns(RESOLV_FILE_PATH);

        whenSetDnServers(Arrays.asList("8.8.8.8", "8.8.4.4"));

        thenFileIsUpdated(Arrays.asList("8.8.8.8", "8.8.4.4"));
    }

    private void givenResolvFile(String lines) throws IOException {
        FileWriter fileWriter = new FileWriter(RESOLV_FILE_PATH);
        PrintWriter printWriter = new PrintWriter(fileWriter);
        printWriter.print(RESOLV_FILE_CONTENT);
        printWriter.close();
    }

    private void givenLinuxDns(String filePath) {
        this.linuxDns = new LinuxDns(filePath, new String[0]);
    }

    private void whenSetDnServers(List<String> addresses) throws UnknownHostException {
        Set<IPAddress> servers = new HashSet<>();
        for (String address : addresses) {
            servers.add(IPAddress.parseHostAddress(address));
        }
        this.linuxDns.setDnServers(servers);
    }

    private void thenFileIsUpdated(List<String> addresses) throws IOException {
        List<String> expectedLines = new ArrayList<>();
        expectedLines.add("search example.com");
        addresses.forEach(expectedLines::add);

        Path path = Paths.get(RESOLV_FILE_PATH);
        Stream<String> lines = Files.lines(path);
        String data = lines.collect(Collectors.joining("\n")).trim();
        lines.close();

        expectedLines.forEach(line -> assertTrue(data.contains(line)));
    }

    private void deleteTemporaryFile() {
        File resolvFile = new File(RESOLV_FILE_PATH);
        resolvFile.delete();
    }
}
