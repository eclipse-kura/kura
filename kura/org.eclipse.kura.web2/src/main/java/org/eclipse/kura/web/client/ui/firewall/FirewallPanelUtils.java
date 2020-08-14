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
        boolean isInRange = false;
        try {
            String[] portRange = ports.split(":");
            if (portRange.length == 2) {
                portRange[0] = portRange[0].trim();
                portRange[1] = portRange[1].trim();
                isInRange = checkPort(portRange[0]) && checkPort(portRange[1])
                        && Integer.parseInt(portRange[0]) < Integer.parseInt(portRange[1]);
            } else if (portRange.length == 1) {
                isInRange = checkPort(portRange[0].trim());
            }
        } catch (Exception e) {
            // do nothing
        }
        return isInRange;
    }

    private static boolean checkPort(String port) {
        boolean isInRange = false;
        try {
            Integer portInt = Integer.parseInt(port);
            if (!port.startsWith("0") && portInt > 0 && portInt <= 65535) {
                isInRange = true;
            }
        } catch (Exception e) {
            // do nothing
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
