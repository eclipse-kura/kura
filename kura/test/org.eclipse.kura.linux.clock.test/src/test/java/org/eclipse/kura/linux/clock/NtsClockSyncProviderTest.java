package org.eclipse.kura.linux.clock;

import static org.junit.Assume.assumeTrue;

import org.junit.Test;

public class NtsClockSyncProviderTest {

    @Test
    public void testSynch() {
        assumeTrue("Only run this test on Linux", System.getProperty("os.name").matches("[Ll]inux"));

        NtsClockSyncProvider clockSyncProvider = new NtsClockSyncProvider(null);
    }
}
