package org.eclipse.kura.net.admin.event;

import java.util.Map;

import org.osgi.service.event.Event;

public class FirewallConfigurationChangeEvent extends Event  {
	/** Topic of the FirewallConfigurationChangeEvent */
    public static final String FIREWALL_EVENT_CONFIG_CHANGE_TOPIC = "org/eclipse/kura/net/admin/event/FIREWALL_EVENT_CONFIG_CHANGE_TOPIC";

	public FirewallConfigurationChangeEvent(Map<String, ?> properties) {
		 super(FIREWALL_EVENT_CONFIG_CHANGE_TOPIC, properties);
	}
}
