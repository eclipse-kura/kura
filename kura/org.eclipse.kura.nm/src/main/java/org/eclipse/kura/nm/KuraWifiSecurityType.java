package org.eclipse.kura.nm;

public enum KuraWifiSecurityType {

    SECURITY_NONE,
    SECURITY_WEP,
    SECURITY_WPA,
    SECURITY_WPA2,
    SECURITY_WPA_WPA2;

    public static KuraWifiSecurityType fromString(String securityType) {
        switch (securityType) {
        case "NONE":
            return KuraWifiSecurityType.SECURITY_NONE;
        case "SECURITY_WEP":
            return KuraWifiSecurityType.SECURITY_WEP;
        case "SECURITY_WPA":
            return KuraWifiSecurityType.SECURITY_WPA;
        case "SECURITY_WPA2":
            return KuraWifiSecurityType.SECURITY_WPA2;
        case "SECURITY_WPA_WPA2":
            return KuraWifiSecurityType.SECURITY_WPA_WPA2;
        default:
            throw new IllegalArgumentException("Invalid security type: " + securityType);
        }
    }
}
