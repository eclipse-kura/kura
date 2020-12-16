package org.eclipse.kura.linux.net.iptables;

import static org.junit.Assert.assertEquals;

import java.net.UnknownHostException;

import org.eclipse.kura.KuraException;
import org.junit.Test;

public class NatPreroutingChainRuleTest {

    @Test
    public void stringToNatPreroutingRuleTest() throws KuraException, NumberFormatException, UnknownHostException {
        NatPreroutingChainRule preroutingRule = new NatPreroutingChainRule(
                "-A prerouting-kura -s 172.16.0.100/32 -i eth0 -p tcp -m mac --mac-source 00:11:22:33:44:55:66 -m tcp --sport 10100:10200 --dport 3040 -j DNAT --to-destination 172.16.0.1:4050");

        assertEquals("172.16.0.1", preroutingRule.getDstIpAddress());
        assertEquals(3040, preroutingRule.getExternalPort());
        assertEquals("eth0", preroutingRule.getInputInterface());
        assertEquals(4050, preroutingRule.getInternalPort());
        assertEquals("00:11:22:33:44:55:66", preroutingRule.getPermittedMacAddress());
        assertEquals("172.16.0.100", preroutingRule.getPermittedNetwork());
        assertEquals(32, preroutingRule.getPermittedNetworkMask());
        assertEquals("tcp", preroutingRule.getProtocol());
        assertEquals(10100, preroutingRule.getSrcPortFirst());
        assertEquals(10200, preroutingRule.getSrcPortLast());
    }

    @Test
    public void natPreroutingRuleToStringTest() throws NumberFormatException, UnknownHostException {
        NatPreroutingChainRule preroutingRule = new NatPreroutingChainRule("eth1", "udp", 123, 321, 10200, 10400,
                "1.2.3.4", "9.8.7.6", 32, "00:11:22:33:44:55:66");

        assertEquals(
                "-A prerouting-kura -s 9.8.7.6/32 -i eth1 -p udp -m mac --mac-source 00:11:22:33:44:55:66 -m udp --sport 10200:10400 --dport 123 -j DNAT --to-destination 1.2.3.4:321",
                preroutingRule.toString());
    }

}
