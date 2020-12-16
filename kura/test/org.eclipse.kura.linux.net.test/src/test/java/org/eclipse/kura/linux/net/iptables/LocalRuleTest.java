/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.linux.net.iptables;

import static org.junit.Assert.assertEquals;

import java.net.UnknownHostException;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.net.IP4Address;
import org.eclipse.kura.net.IPAddress;
import org.eclipse.kura.net.NetworkPair;
import org.junit.Test;

public class LocalRuleTest {

    @Test
    public void stringToLocalRuleTest() throws KuraException, NumberFormatException, UnknownHostException {
        LocalRule localRule = new LocalRule(
                "-A input-kura -p tcp -s 0.0.0.0/0 -i eth0 -m mac --mac-source 00:11:22:33:44:55:66 --sport 10100:10200 --dport 5400:5401 -j ACCEPT");

        assertEquals("5400:5401", localRule.getPortRange());
        assertEquals("eth0", localRule.getPermittedInterfaceName());
        assertEquals("00:11:22:33:44:55:66", localRule.getPermittedMAC());
        assertEquals(new NetworkPair<>((IP4Address) IPAddress.parseHostAddress("0.0.0.0"), Short.parseShort("0")),
                localRule.getPermittedNetwork());
        assertEquals("tcp", localRule.getProtocol());
        assertEquals("10100:10200", localRule.getSourcePortRange());
    }

    @Test
    public void localRuleToStringTest() throws NumberFormatException, UnknownHostException {
        LocalRule localRule = new LocalRule("1400:1401", "udp",
                new NetworkPair<>((IP4Address) IPAddress.parseHostAddress("192.168.0.0"), Short.parseShort("24")), null,
                "eth2", "00:11:22:33:44:55:66", "10100:10200");

        assertEquals(
                "-A input-kura -p udp -s 192.168.0.0/24 ! -i eth2 -m mac --mac-source 00:11:22:33:44:55:66 --sport 10100:10200 --dport 1400:1401 -j ACCEPT",
                localRule.toString());
    }
}
