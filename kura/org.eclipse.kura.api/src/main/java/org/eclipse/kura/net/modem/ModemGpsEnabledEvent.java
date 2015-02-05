package org.eclipse.kura.net.modem;

import java.util.Map;

import org.osgi.service.event.Event;

public class ModemGpsEnabledEvent extends Event {

	/** Topic of the ModemGpsEnabledEvent */
    public static final String MODEM_EVENT_GPS_ENABLED_TOPIC = "org/eclipse/kura/net/modem/gps/ENABLED";
    
    public static final String Port = "port";
    public static final String BaudRate = "baudRate";
    public static final String DataBits = "bitsPerWord";
    public static final String StopBits = "stopBits";
    public static final String Parity = "parity";
    
    public ModemGpsEnabledEvent(Map<String, Object> properties) {
        super(MODEM_EVENT_GPS_ENABLED_TOPIC, properties);
    }
}
