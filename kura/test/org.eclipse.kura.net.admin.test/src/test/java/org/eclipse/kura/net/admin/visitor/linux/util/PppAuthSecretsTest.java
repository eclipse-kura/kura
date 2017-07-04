/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.kura.net.admin.visitor.linux.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.kura.core.testutil.TestUtil;
import org.junit.Test;


public class PppAuthSecretsTest {

    @Test
    public void testReadAndAdd() throws Exception {
        String file = "/tmp/ppp/secrets";
        File f = new File(file);
        f.getParentFile().mkdirs();

        // create a small sample file
        String str = "#client server secret IP addresses Provider\nmobileweb * password * #o2\nuser * pass * #orange\n";

        Files.write(Paths.get(file), str.getBytes());

        // init
        PppAuthSecrets pas = new PppAuthSecrets(file);

        f.delete();

        // check the contents
        List<String> clients = (List<String>) TestUtil.getFieldValue(pas, "clients");
        assertEquals(2, clients.size());

        assertTrue(pas.checkForEntry("orange", "user", "*", "pass", "*"));
        assertTrue(pas.checkForEntry("o2", "mobileweb", "*", "password", "*"));
        assertFalse(pas.checkForEntry("o2", "web", "*", "password", "*"));
        assertFalse(pas.checkForEntry("o2", "mobileweb", "*", "pass", "*"));

        assertEquals("pass", pas.getSecret("orange", "user", "*", "*"));
        assertEquals("password", pas.getSecret("o2", "mobileweb", "*", "*"));

        // add a new entry
        pas.addEntry("provider", "client", "server", "secret", "ipAddress");

        assertEquals(3, clients.size());
        assertTrue(pas.checkForEntry("provider", "client", "server", "secret", "ipAddress"));

        // update an old entry
        pas.addEntry("o2", "client", "server", "secret", "ipAddress");

        assertEquals(3, clients.size());
        assertTrue(pas.checkForEntry("o2", "client", "server", "secret", "ipAddress"));
        assertFalse(pas.checkForEntry("o2", "mobileweb", "*", "password", "*"));

        assertTrue(f.exists());

        List<String> lines = Files.readAllLines(Paths.get(file));
        assertEquals(5, lines.size());

        // remove first entry
        pas.removeEntry(0);

        assertFalse(pas.checkForEntry("o2", "client", "server", "secret", "ipAddress"));
        assertTrue(pas.checkForEntry("provider", "client", "server", "secret", "ipAddress"));
        assertTrue(pas.checkForEntry("orange", "user", "*", "pass", "*"));

        lines = Files.readAllLines(Paths.get(file));
        assertEquals(4, lines.size());

        // add multiple entries and remove by various types
        pas.addEntry("provider1", "client1", "server1", "secret1", "ipAddress1");
        pas.addEntry("provider2", "client2", "server2", "secret2", "ipAddress2");
        pas.addEntry("provider3", "client3", "server3", "secret3", "ipAddress3");
        pas.addEntry("provider4", "client4", "server4", "secret4", "ipAddress4");
        pas.addEntry("provider5", "client5", "server5", "secret5", "ipAddress5");
        pas.addEntry("provider6", "client6", "server6", "secret6", "ipAddress6");
        pas.addEntry("provider7", "client7", "server7", "secret7", "ipAddress7");
        pas.addEntry("provider8", "client7", "server8", "secret8", "ipAddress8");
        pas.addEntry("provider9", "client8", "server8", "secret8", "ipAddress8");
        pas.addEntry("provider10", "client8", "server8", "secret8", "ipAddress8");

        assertTrue(pas.checkForEntry("provider1", "client1", "server1", "secret1", "ipAddress1"));
        assertTrue(pas.checkForEntry("provider2", "client2", "server2", "secret2", "ipAddress2"));
        assertTrue(pas.checkForEntry("provider3", "client3", "server3", "secret3", "ipAddress3"));
        assertTrue(pas.checkForEntry("provider4", "client4", "server4", "secret4", "ipAddress4"));
        assertTrue(pas.checkForEntry("provider5", "client5", "server5", "secret5", "ipAddress5"));
        assertTrue(pas.checkForEntry("provider6", "client6", "server6", "secret6", "ipAddress6"));
        // same client
        assertTrue(pas.checkForEntry("provider7", "client7", "server7", "secret7", "ipAddress7"));
        assertTrue(pas.checkForEntry("provider8", "client7", "server8", "secret8", "ipAddress8"));
        // same server
        assertTrue(pas.checkForEntry("provider9", "client8", "server8", "secret8", "ipAddress8"));
        assertTrue(pas.checkForEntry("provider10", "client8", "server8", "secret8", "ipAddress8"));

        pas.removeEntry("provider", "provider1");
        pas.removeEntry("client", "client2");
        pas.removeEntry("server", "server3");
        pas.removeEntry("secret", "secret4");
        pas.removeEntry("ipAddress", "ipAddress5");
        pas.removeEntry("client", "client7");
        pas.removeEntry("server", "server8");

        assertTrue(pas.checkForEntry("provider6", "client6", "server6", "secret6", "ipAddress6"));
        assertFalse(pas.checkForEntry("provider1", "client1", "server1", "secret1", "ipAddress1"));
        assertFalse(pas.checkForEntry("provider2", "client2", "server2", "secret2", "ipAddress2"));
        assertFalse(pas.checkForEntry("provider3", "client3", "server3", "secret3", "ipAddress3"));
        assertFalse(pas.checkForEntry("provider4", "client4", "server4", "secret4", "ipAddress4"));
        assertFalse(pas.checkForEntry("provider5", "client5", "server5", "secret5", "ipAddress5"));
        assertFalse(pas.checkForEntry("provider7", "client7", "server7", "secret7", "ipAddress7"));
        assertFalse(pas.checkForEntry("provider8", "client7", "server8", "secret8", "ipAddress8"));
        assertFalse(pas.checkForEntry("provider9", "client8", "server8", "secret8", "ipAddress8"));
        assertFalse(pas.checkForEntry("provider10", "client8", "server8", "secret8", "ipAddress8"));

        lines = Files.readAllLines(Paths.get(file));
        assertEquals(5, lines.size());
    }

}
