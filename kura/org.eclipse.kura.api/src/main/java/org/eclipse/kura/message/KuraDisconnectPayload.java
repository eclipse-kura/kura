/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.message;

import java.util.Iterator;

/**
 * The KuraDisconnectPayload is an extension of {@link KuraPayload} that contains the parameters stored in a message
 * that is usually published when disconnecting from the broker.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 2.1
 */
public class KuraDisconnectPayload extends KuraPayload {

    private static final String UPTIME = "uptime";
    private static final String DISPLAY_NAME = "display_name";

    public KuraDisconnectPayload(String uptime, String displayName) {
        super();

        addMetric(UPTIME, uptime);
        addMetric(DISPLAY_NAME, displayName);
    }

    public KuraDisconnectPayload(KuraPayload kuraMessage) {
        Iterator<String> hdrIterator = kuraMessage.metricsIterator();
        while (hdrIterator.hasNext()) {
            String hdrName = hdrIterator.next();
            String hdrVal = (String) kuraMessage.getMetric(hdrName);
            addMetric(hdrName, hdrVal);
        }
        setBody(kuraMessage.getBody());
    }

    public String getUptime() {
        return (String) getMetric(UPTIME);
    }

    public String getDisplayName() {
        return (String) getMetric(DISPLAY_NAME);
    }

    @Override
    public String toString() {
        return "KuraBirthMessage [getUptime()=" + getUptime() + ", getDisplayName()=" + getDisplayName() + "]";
    }
}
