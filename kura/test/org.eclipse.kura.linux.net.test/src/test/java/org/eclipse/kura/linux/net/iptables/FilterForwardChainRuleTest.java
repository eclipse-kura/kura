package org.eclipse.kura.linux.net.iptables;

import static org.junit.Assert.assertEquals;

import java.net.UnknownHostException;

import org.eclipse.kura.KuraException;
import org.junit.Test;

public class FilterForwardChainRuleTest {

    @Test
    public void stringToFilterForwardRuleTest() throws KuraException, NumberFormatException, UnknownHostException {
        FilterForwardChainRule forwardRule = new FilterForwardChainRule(
                "-A forward-kura -s 172.16.0.100/32 -d 172.16.0.1/32 -i eth0 -o eth1 -p tcp -m tcp -m mac --mac-source 00:11:22:33:44:55:66 --sport 10100:10200 -j ACCEPT");

        assertEquals(32, forwardRule.getDstMask());
        assertEquals("172.16.0.1", forwardRule.getDstNetwork());
        assertEquals("eth0", forwardRule.getInputInterface());
        assertEquals("eth1", forwardRule.getOutputInterface());
        assertEquals("tcp", forwardRule.getProtocol());
        assertEquals(32, forwardRule.getSrcMask());
        assertEquals("172.16.0.100", forwardRule.getSrcNetwork());
    }

}
