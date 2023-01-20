package org.eclipse.kura.net2.admin;

public enum KuraInterfaceStatus {

    DISABLED,
    UNMANAGED,
    L2ONLY,
    ENABLEDLAN,
    ENABLEDWAN,
    UNKNOWN;

    public static KuraInterfaceStatus fromString(String status) {
        switch (status) {
        case "netIPv4StatusDisabled":
        case "netIPv6StatusDisabled":
            return KuraInterfaceStatus.DISABLED;
        case "netIPv4StatusUnmanaged":
        case "netIPv6StatusUnmanaged":
            return KuraInterfaceStatus.UNMANAGED;
        case "netIPv4StatusL2Only":
        case "netIPv6StatusL2Only":
            return KuraInterfaceStatus.L2ONLY;
        case "netIPv4StatusEnabledLAN":
        case "netIPv6StatusEnabledLAN":
            return KuraInterfaceStatus.ENABLEDLAN;
        case "netIPv4StatusEnabledWAN":
        case "netIPv6StatusEnabledWAN":
            return KuraInterfaceStatus.ENABLEDWAN;
        default:
            return KuraInterfaceStatus.UNKNOWN;

        }

    }

}
