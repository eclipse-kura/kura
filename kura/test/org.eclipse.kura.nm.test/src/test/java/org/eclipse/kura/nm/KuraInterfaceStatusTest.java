package org.eclipse.kura.nm;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class KuraInterfaceStatusTest {

    @Parameter(0)
    public KuraIpStatus ip4Status;
    @Parameter(1)
    public KuraIpStatus ip6Status;
    @Parameter(2)
    public KuraInterfaceStatus expectedResult;

    @Parameter(3)
    public Class<? extends Exception> expectedException;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] { //
                { KuraIpStatus.DISABLED, KuraIpStatus.DISABLED, KuraInterfaceStatus.DISABLED, null }, //
                { KuraIpStatus.ENABLEDLAN, KuraIpStatus.ENABLEDLAN, KuraInterfaceStatus.ENABLED, null }, //
                { KuraIpStatus.ENABLEDWAN, KuraIpStatus.ENABLEDWAN, KuraInterfaceStatus.ENABLED, null }, //
                { KuraIpStatus.ENABLEDLAN, KuraIpStatus.ENABLEDWAN, KuraInterfaceStatus.ENABLED, null }, //
                { KuraIpStatus.ENABLEDWAN, KuraIpStatus.ENABLEDLAN, KuraInterfaceStatus.ENABLED, null }, //
                { KuraIpStatus.ENABLEDLAN, KuraIpStatus.DISABLED, KuraInterfaceStatus.ENABLED, null }, //
                { KuraIpStatus.ENABLEDWAN, KuraIpStatus.DISABLED, KuraInterfaceStatus.ENABLED, null }, //
                { KuraIpStatus.DISABLED, KuraIpStatus.ENABLEDLAN, KuraInterfaceStatus.ENABLED, null }, //
                { KuraIpStatus.DISABLED, KuraIpStatus.ENABLEDWAN, KuraInterfaceStatus.ENABLED, null }, //
                { KuraIpStatus.UNMANAGED, KuraIpStatus.UNMANAGED, KuraInterfaceStatus.UNMANAGED, null }, //
                { KuraIpStatus.UNMANAGED, KuraIpStatus.DISABLED, null, IllegalArgumentException.class }, //
                { KuraIpStatus.UNMANAGED, KuraIpStatus.ENABLEDLAN, null, IllegalArgumentException.class }, //
                { KuraIpStatus.UNMANAGED, KuraIpStatus.ENABLEDWAN, null, IllegalArgumentException.class }, //
                { KuraIpStatus.UNKNOWN, KuraIpStatus.DISABLED, null, IllegalArgumentException.class }, //
                { KuraIpStatus.UNKNOWN, KuraIpStatus.ENABLEDLAN, null, IllegalArgumentException.class }, //
                { KuraIpStatus.UNKNOWN, KuraIpStatus.ENABLEDWAN, null, IllegalArgumentException.class }, //
                { KuraIpStatus.UNKNOWN, KuraIpStatus.UNMANAGED, null, IllegalArgumentException.class }, //
                { KuraIpStatus.UNKNOWN, KuraIpStatus.UNKNOWN, null, IllegalArgumentException.class }, //
                { KuraIpStatus.DISABLED, KuraIpStatus.UNKNOWN, null, IllegalArgumentException.class }, //
                { KuraIpStatus.ENABLEDLAN, KuraIpStatus.UNKNOWN, null, IllegalArgumentException.class }, //
                { KuraIpStatus.ENABLEDWAN, KuraIpStatus.UNKNOWN, null, IllegalArgumentException.class }, //
                { KuraIpStatus.UNMANAGED, KuraIpStatus.UNKNOWN, null, IllegalArgumentException.class }, //
        });
    }

    @Test
    public void test() throws IllegalArgumentException {
        if (expectedException != null) {
            thrown.expect(expectedException);
        }

        assertEquals(expectedResult, KuraInterfaceStatus.fromKuraIpStatus(ip4Status, ip6Status));
    }

}
