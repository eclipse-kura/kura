package org.eclipse.kura.nm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Enclosed.class)
public class KuraWifiSecurityTypeTest {

    @RunWith(Parameterized.class)
    public static class KuraWifiSecurityTypeFromStringTestErrors {

        private Exception occurredException;
        KuraWifiSecurityType securityType;
        private String inputSecurityType;

        @Parameters
        public static Collection<Object[]> SimTypeParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { "UNKNOWN" });
            params.add(new Object[] { null });
            params.add(new Object[] { "" });
            return params;
        }

        public KuraWifiSecurityTypeFromStringTestErrors(String securityType) {
            this.inputSecurityType = securityType;
        }

        @Test
        public void shouldThrowException() {
            whenFromStringIsCalledWith(this.inputSecurityType);
            thenExceptionOccurred(IllegalArgumentException.class);
        }

        public void whenFromStringIsCalledWith(String securityType) {
            try {
                this.securityType = KuraWifiSecurityType.fromString(securityType);
            } catch (Exception e) {
                this.occurredException = e;
            }
        }

        private <E extends Exception> void thenExceptionOccurred(Class<E> expectedExceptionClass) {
            assertNotNull(this.occurredException);
            assertEquals(expectedExceptionClass, this.occurredException.getClass());
        }
    }

    @RunWith(Parameterized.class)
    public static class KuraWifiSecurityTypeFromStringTest {

        private String inputSecurityType;
        private KuraWifiSecurityType expectedSecurityType;
        private KuraWifiSecurityType outputSecurityType;

        @Parameters
        public static Collection<Object[]> SimTypeParams() {
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[] { "NONE", KuraWifiSecurityType.SECURITY_NONE });
            params.add(new Object[] { "SECURITY_WEP", KuraWifiSecurityType.SECURITY_WEP });
            params.add(new Object[] { "SECURITY_WPA", KuraWifiSecurityType.SECURITY_WPA });
            params.add(new Object[] { "SECURITY_WPA2", KuraWifiSecurityType.SECURITY_WPA2 });
            params.add(new Object[] { "SECURITY_WPA_WPA2", KuraWifiSecurityType.SECURITY_WPA_WPA2 });
            return params;
        }

        public KuraWifiSecurityTypeFromStringTest(String inputSecurityType, KuraWifiSecurityType expectedSecurityType) {
            this.inputSecurityType = inputSecurityType;
            this.expectedSecurityType = expectedSecurityType;
        }

        @Test
        public void shouldWork() {
            whenFromStringIsCalledWith(this.inputSecurityType);
            thenSecurityTypeIs(this.expectedSecurityType);
        }

        public void whenFromStringIsCalledWith(String securityType) {
            this.outputSecurityType = KuraWifiSecurityType.fromString(securityType);
        }

        private void thenSecurityTypeIs(KuraWifiSecurityType securityType) {
            assertEquals(this.expectedSecurityType, this.outputSecurityType);
        }
    }
}
