package org.eclipse.kura.nm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KuraWifiSecurityTypeTest {

    KuraWifiSecurityType securityType;
    String stringSecurityType;
    private Exception occurredException;

    @Test
    public void conversionThrowsForTypeUnknown() {
        whenFromStringIsCalledWith("UNKNOWN");
        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void conversionThrowsForTypeNull() {
        whenFromStringIsCalledWith(null);
        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void conversionThrowsForTypeEmpty() {
        whenFromStringIsCalledWith("");
        thenExceptionOccurred(IllegalArgumentException.class);
    }

    @Test
    public void conversionWorksForTypeNone() {
        whenFromStringIsCalledWith("NONE");
        thenSecurityTypeIs(KuraWifiSecurityType.SECURITY_NONE);
    }

    @Test
    public void conversionWorksForTypeWep() {
        whenFromStringIsCalledWith("SECURITY_WEP");
        thenSecurityTypeIs(KuraWifiSecurityType.SECURITY_WEP);
    }

    @Test
    public void conversionWorksForTypeWpa() {
        whenFromStringIsCalledWith("SECURITY_WPA");
        thenSecurityTypeIs(KuraWifiSecurityType.SECURITY_WPA);
    }

    @Test
    public void conversionWorksForTypeWpa2() {
        whenFromStringIsCalledWith("SECURITY_WPA2");
        thenSecurityTypeIs(KuraWifiSecurityType.SECURITY_WPA2);
    }

    @Test
    public void conversionWorksForTypeWpaWpa2() {
        whenFromStringIsCalledWith("SECURITY_WPA_WPA2");
        thenSecurityTypeIs(KuraWifiSecurityType.SECURITY_WPA_WPA2);
    }

    public void whenFromStringIsCalledWith(String securityType) {
        try {
            this.securityType = KuraWifiSecurityType.fromString(securityType);
        } catch (Exception e) {
            this.occurredException = e;
        }
    }

    private void thenSecurityTypeIs(KuraWifiSecurityType securityType) {
        assertEquals(securityType, this.securityType);
    }

    private <E extends Exception> void thenExceptionOccurred(Class<E> expectedExceptionClass) {
        assertNotNull(this.occurredException);
        assertEquals(expectedExceptionClass, this.occurredException.getClass());
    }
}
