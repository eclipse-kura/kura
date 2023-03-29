package org.eclipse.kura.nm;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.freedesktop.dbus.types.UInt32;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class MMMModemStateChangeReasonTest {

    @RunWith(Parameterized.class)
    public static class fromUInt32Test {

        @Parameters
        public static Collection<Object[]> MMModemStateChangeReasonParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0), MMModemStateChangeReason.MM_MODEM_STATE_CHANGE_REASON_UNKNOWN });
            params.add(new Object[] { new UInt32(1),
                    MMModemStateChangeReason.MM_MODEM_STATE_CHANGE_REASON_USER_REQUESTED });
            params.add(new Object[] { new UInt32(2), MMModemStateChangeReason.MM_MODEM_STATE_CHANGE_REASON_SUSPEND });
            params.add(new Object[] { new UInt32(3), MMModemStateChangeReason.MM_MODEM_STATE_CHANGE_REASON_FAILURE });
            params.add(new Object[] { new UInt32(66), MMModemStateChangeReason.MM_MODEM_STATE_CHANGE_REASON_UNKNOWN });
            return params;
        }

        private final UInt32 inputValue;
        private final MMModemStateChangeReason expectedValue;
        private MMModemStateChangeReason computedValue;

        public fromUInt32Test(UInt32 intValue, MMModemStateChangeReason expectedReason) {
            this.inputValue = intValue;
            this.expectedValue = expectedReason;
        }

        @Test
        public void shouldReturnExpectedDeviceStateReason() {
            whenConversionMethodIsCalledWith(this.inputValue);
            thenCalculatedValueMatches(this.expectedValue);
        }

        private void whenConversionMethodIsCalledWith(UInt32 input) {
            this.computedValue = MMModemStateChangeReason.fromUInt32(input);
        }

        private void thenCalculatedValueMatches(MMModemStateChangeReason expected) {
            assertEquals(expected, this.computedValue);
        }

    }

}
