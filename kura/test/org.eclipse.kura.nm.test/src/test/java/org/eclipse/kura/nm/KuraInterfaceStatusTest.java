package org.eclipse.kura.nm;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class KuraInterfaceStatusTest {

    @Test
    public void test() {
        KuraIpStatus ip4Status = KuraIpStatus.DISABLED;
        KuraIpStatus ip6Status = KuraIpStatus.DISABLED;

        assertEquals(KuraInterfaceStatus.DISABLED, KuraInterfaceStatus.fromKuraIpStatus(ip4Status, ip6Status));

    }

}
