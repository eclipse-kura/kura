package org.eclipse.kura.nm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KuraWifiSecurityTypeTest {

    KuraWifiSecurityType securityType;
    String stringSecurityType;
    private boolean exceptionWasThrown;

    @Test
    public void conversionThrowsForTypeUnknown() {
        whenFromStringIsCalledWith("UNKNOWN");
        thenExceptionWasThrown();
    }

    @Test
    public void conversionThrowsForTypeNull() {
        whenFromStringIsCalledWith(null);
        thenExceptionWasThrown();
    }

    @Test
    public void conversionThrowsForTypeEmpty() {
        whenFromStringIsCalledWith("");
        thenExceptionWasThrown();
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
        } catch (IllegalArgumentException e) {
            this.exceptionWasThrown = true;
        }
    }

    private void thenSecurityTypeIs(KuraWifiSecurityType securityType) {
        assertEquals(securityType, this.securityType);
    }

    private void thenExceptionWasThrown() {
        assertTrue(this.exceptionWasThrown);
    }
}
