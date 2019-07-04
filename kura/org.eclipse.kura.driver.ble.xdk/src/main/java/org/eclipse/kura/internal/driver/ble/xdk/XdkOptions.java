package org.eclipse.kura.internal.driver.ble.xdk;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.Map;

public class XdkOptions {
	
    private static final String INAME = "iname";
    private final Map<String, Object> properties;
    
    /**
     * Instantiates a new BLE SensorTag options.
     *
     * @param properties
     *            the properties
     * @throws NullPointerException
     *             if any of the arguments is null
     */
    XdkOptions(final Map<String, Object> properties) {
        requireNonNull(properties, "Properties cannot be null");

        this.properties = properties;
    }

    /**
     * Returns the Bluetooth Interface Name to be used
     *
     * @return the Bluetooth Adapter name (i.e. hci0)
     */
    String getBluetoothInterfaceName() {
        String interfaceName = null;
        final Object iname = this.properties.get(INAME);
        if (nonNull(iname) && (iname instanceof String)) {
            interfaceName = iname.toString();
        }
        return interfaceName;
    }

}
