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
import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;

import org.eclipse.kura.KuraException;
import org.junit.Test;

public class NatPostroutingChainRuleTest {

    @Test
    public void stringToNatPostroutingRuleTest() throws KuraException, NumberFormatException, UnknownHostException {
        NatPostroutingChainRule postroutingRule = new NatPostroutingChainRule(
                "-A postrouting-kura -s 172.16.0.100/32 -d 172.16.0.1/32 -o eth2 -p tcp -j MASQUERADE");

        assertEquals("eth2", postroutingRule.getDstInterface());
        assertEquals(32, postroutingRule.getDstMask());
        assertEquals("172.16.0.1", postroutingRule.getDstNetwork());
        assertEquals("tcp", postroutingRule.getProtocol());
        assertEquals(32, postroutingRule.getSrcMask());
        assertEquals("172.16.0.100", postroutingRule.getSrcNetwork());
        assertTrue(postroutingRule.isMasquerade());
    }

    @Test
    public void natPostroutingRuleToStringTest() throws NumberFormatException, UnknownHostException {
        NatPostroutingChainRule postroutingRule = new NatPostroutingChainRule("192.168.0.0", (short) 24, "172.16.0.0",
                (short) 32, "eth6", "udp", true);

        assertEquals("-A postrouting-kura -s 172.16.0.0/32 -d 192.168.0.0/24 -o eth6 -p udp -j MASQUERADE",
                postroutingRule.toString());
    }

}
