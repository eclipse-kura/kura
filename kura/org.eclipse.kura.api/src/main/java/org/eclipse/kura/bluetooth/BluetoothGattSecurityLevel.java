package org.eclipse.kura.bluetooth;

/**
 * Security levels.
 *
 * @since 1.2
 * 
 * @deprecated
 * 
 */
@Deprecated
public enum BluetoothGattSecurityLevel {

    LOW,
    MEDIUM,
    HIGH,
    UNKNOWN;

    public static BluetoothGattSecurityLevel getBluetoothGattSecurityLevel(String level) {
        if (LOW.name().equalsIgnoreCase(level)) {
            return LOW;
        } else if (MEDIUM.name().equalsIgnoreCase(level)) {
            return MEDIUM;
        } else if (HIGH.name().equalsIgnoreCase(level)) {
            return HIGH;
        } else {
            return UNKNOWN;
        }

    }

}
