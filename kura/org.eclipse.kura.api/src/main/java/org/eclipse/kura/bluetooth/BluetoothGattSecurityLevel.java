package org.eclipse.kura.bluetooth;

/**
 * Security levels.
 *
 * @since {@link org.eclipse.kura.bluetooth} 1.4.0
 */
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
