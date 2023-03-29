package org.eclipse.kura.nm;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.freedesktop.dbus.types.UInt32;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

public class NMDeviceStateReasonTest {

    @RunWith(Parameterized.class)
    public static class fromUInt32Test {

        @Parameters
        public static Collection<Object[]> NMDeviceStateReasonParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { new UInt32(0), NMDeviceStateReason.NM_DEVICE_STATE_REASON_NONE });
            return params;
        }

        private final UInt32 inputValue;
        private final NMDeviceStateReason expectedValue;
        private NMDeviceStateReason computedValue;

        public fromUInt32Test(UInt32 intValue, NMDeviceStateReason expectedReason) {
            this.inputValue = intValue;
            this.expectedValue = expectedReason;
        }

        @Test
        public void shouldReturnExpectedDeviceStateReason() {
            whenConversionMethodIsCalledWith(this.inputValue);
            thenCalculatedValueMatches(this.expectedValue);
        }

        private void whenConversionMethodIsCalledWith(UInt32 input) {
            this.computedValue = NMDeviceStateReason.fromUInt32(input);
        }

        private void thenCalculatedValueMatches(NMDeviceStateReason expected) {
            assertEquals(expected, this.computedValue);
        }

    }

}
