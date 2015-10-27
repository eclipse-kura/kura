package org.eclipse.kura.net.modem;

import java.util.Map;

import org.osgi.service.event.Event;

public class ModemReadyEvent extends Event {

	/** Topic of the ModemRemovedEvent */
    public static final String MODEM_EVENT_READY_TOPIC = "org/eclipse/kura/net/modem/READY";
    
    public static final String IMEI = "IMEI";
    public static final String IMSI = "IMSI";
    public static final String ICCID = "ICCID";
    public static final String RSSI = "RSSI";
    
    public ModemReadyEvent(Map<String, String> properties) {
        super(MODEM_EVENT_READY_TOPIC, properties);
    }
}
