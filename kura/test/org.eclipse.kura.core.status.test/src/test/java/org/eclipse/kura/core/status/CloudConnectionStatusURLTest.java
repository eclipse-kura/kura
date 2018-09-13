package org.eclipse.kura.core.status;

import static org.eclipse.kura.core.status.CloudConnectionStatusURL.NOTIFICATION_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

public class CloudConnectionStatusURLTest {

    private static final String LINUX_LED_TEST_UPPER_CASE_PATH = "/sys/class/led/UPPER_CASE";
    private static final String LINUX_LED_TEST_UPPER_CASE = "linux_led:" + LINUX_LED_TEST_UPPER_CASE_PATH;
    private static final String LINUX_LED_LED1_GREEN_PATH = "/sys/class/led/led1_green";
    private static final String LINUX_LED_LED1_GREEN = "linux_led:" + LINUX_LED_LED1_GREEN_PATH;
    private static final String LED_44 = "led:44";
    private static final String CCS_PREFIX = "ccs:";
    private static final String INVERTED = ":inverted";

    @Before
    public void setUp() throws Exception {
    }

    @Test(expected = NullPointerException.class)
    public void testParseUrlNullUrl() {
        CloudConnectionStatusURL.parseURL(null);
    }

    @Test
    public void testParseUrlEmptyUrl() {
        Properties props = CloudConnectionStatusURL.parseURL("");
        assertNotNull(props);
        assertEquals(StatusNotificationTypeEnum.NONE, props.get(NOTIFICATION_TYPE));
    }

    @Test
    public void testParseNonCCSStartingUrl() {
        Properties props = CloudConnectionStatusURL.parseURL("ppp");
        assertNotNull(props);
        assertEquals(StatusNotificationTypeEnum.NONE, props.get(NOTIFICATION_TYPE));
    }

    @Test
    public void testParseUrlCcsNone() {
        Properties props = CloudConnectionStatusURL.parseURL("ccs:none");
        assertNotNull(props);
        assertEquals(StatusNotificationTypeEnum.NONE, props.get(NOTIFICATION_TYPE));
    }

    @Test
    public void testParseUrlCcsLog() {
        Properties props = CloudConnectionStatusURL.parseURL("ccs:log");
        assertNotNull(props);
        assertEquals(StatusNotificationTypeEnum.LOG, props.get(NOTIFICATION_TYPE));
    }

    @Test
    public void testParseUrlCcsGpioLed() {
        Properties props = CloudConnectionStatusURL.parseURL(CCS_PREFIX + LED_44);
        assertNotNull(props);
        assertEquals(StatusNotificationTypeEnum.LED, props.get(NOTIFICATION_TYPE));
        assertEquals(44, props.get("led"));
        assertEquals(false, props.get("inverted"));
        assertEquals(4, props.size());
    }

    @Test
    public void testParseUrlCcsGpioLedInverted() {
        Properties props = CloudConnectionStatusURL.parseURL(CCS_PREFIX + LED_44 + INVERTED);
        assertNotNull(props);
        assertEquals(StatusNotificationTypeEnum.LED, props.get(NOTIFICATION_TYPE));
        assertEquals(44, props.get("led"));
        assertEquals(4, props.size());
        assertEquals(true, props.get("inverted"));
    }
    
    @Test
    public void testParseUrlCcsLinuxLed() {
        Properties props = CloudConnectionStatusURL.parseURL(CCS_PREFIX + LINUX_LED_LED1_GREEN);
        assertNotNull(props);
        assertEquals(StatusNotificationTypeEnum.LED, props.get(NOTIFICATION_TYPE));
        assertNull(props.get("led"));
        assertEquals(LINUX_LED_LED1_GREEN_PATH, props.get("linux_led"));
        assertEquals(3, props.size());
    }

    @Test
    public void testParseUrlCcsLinuxGpioLed() {
        Properties props = CloudConnectionStatusURL
                .parseURL(CCS_PREFIX + LINUX_LED_LED1_GREEN + ";" + CCS_PREFIX + LED_44);
        assertNotNull(props);
        assertEquals(StatusNotificationTypeEnum.LED, props.get(NOTIFICATION_TYPE));
        assertEquals(44, props.get("led"));
        assertEquals(LINUX_LED_LED1_GREEN_PATH, props.get("linux_led"));
        assertEquals(false, props.get("inverted"));
        assertEquals(5, props.size());
    }
    
    @Test
    public void testParseUrlCcsLinuxWrongGpioLed() {
        Properties props = CloudConnectionStatusURL
                .parseURL(CCS_PREFIX + LINUX_LED_LED1_GREEN + ";" + CCS_PREFIX + "led:test");
        assertNotNull(props);
        assertEquals(StatusNotificationTypeEnum.LED, props.get(NOTIFICATION_TYPE));
        assertEquals(LINUX_LED_LED1_GREEN_PATH, props.get("linux_led"));
        assertEquals(4, props.size());
    }

    @Test
    public void testParseUrlCcsGpioLinuxLed() {
        Properties props = CloudConnectionStatusURL
                .parseURL(CCS_PREFIX + LED_44 + ";" + CCS_PREFIX + LINUX_LED_LED1_GREEN);
        assertNotNull(props);
        assertEquals(StatusNotificationTypeEnum.LED, props.get(NOTIFICATION_TYPE));
        assertEquals(44, props.get("led"));
        assertEquals(LINUX_LED_LED1_GREEN_PATH, props.get("linux_led"));
        assertEquals(false, props.get("inverted"));
        assertEquals(5, props.size());
    }
    
    @Test
    public void testParseUrlCcsLinuxLedUpperCasePath() {
        Properties props = CloudConnectionStatusURL.parseURL(CCS_PREFIX + LINUX_LED_TEST_UPPER_CASE);
        assertNotNull(props);
        assertEquals(StatusNotificationTypeEnum.LED, props.get(NOTIFICATION_TYPE));
        assertNull(props.get("led"));
        assertEquals(LINUX_LED_TEST_UPPER_CASE_PATH, props.get("linux_led"));
        assertEquals(3, props.size());
    }
}
