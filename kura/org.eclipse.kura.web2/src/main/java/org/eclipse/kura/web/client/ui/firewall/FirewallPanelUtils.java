/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.firewall;

import java.util.Comparator;

import org.eclipse.kura.web.client.util.TextFieldValidator.FieldType;
import org.eclipse.kura.web.shared.model.GwtFirewallOpenPortEntry;

public class FirewallPanelUtils {

    public static final int INTERFACE_NAME_MAX_LENGTH = 15;

    private FirewallPanelUtils() {
        // Not needed
    }

    public static boolean isPortInRange(String ports) {
        boolean result = false;
        try {
            String[] portRange = ports.split(":");
            if (portRange.length == 2) {
                int lowerPort = Integer.parseInt(portRange[0].trim());
                int upperPort = Integer.parseInt(portRange[1].trim());
                result = isInRange(lowerPort) && isInRange(upperPort) && lowerPort < upperPort;
            } else if (portRange.length == 1) {
                int port = Integer.parseInt(portRange[0].trim());
                result = isInRange(port);
            }
        } catch (NumberFormatException e) {
            // do nothing
        }
        return result;
    }

    private static boolean isInRange(int port) {
        boolean isInRange = false;
        if (port > 0 && port <= 65535) {
            isInRange = true;
        }
        return isInRange;
    }

    public static boolean checkPortRegex(String ports) {
        return ports.trim().matches(FieldType.PORT.getRegex());
    }

    public static boolean checkPortRangeRegex(String ports) {
        return ports.trim().matches(FieldType.PORT_RANGE.getRegex());
    }

    public static class PortSorting implements Comparator<GwtFirewallOpenPortEntry> {

        @Override
        public int compare(GwtFirewallOpenPortEntry o1, GwtFirewallOpenPortEntry o2) {
            if (o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return 1;
            }
            if (o2 == null) {
                return -1;
            }
            Integer o1Port = Integer.parseInt(o1.getPortRange().split(":")[0]);
            Integer o2Port = Integer.parseInt(o2.getPortRange().split(":")[0]);
            return o1Port.compareTo(o2Port);
        }
    }

}
