/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.message;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.kura.message.KuraDisconnectPayload;
import org.junit.Test;


public class KuraDisconnectPayloadTest {

    @Test
    public void testComplete() {
        String uptime = "0 days 1:2:34 hms";
        String displayName = "dname";
        KuraDisconnectPayload payload = new KuraDisconnectPayload(uptime, displayName);

        assertEquals(uptime, payload.getUptime());
        assertEquals(displayName, payload.getDisplayName());

        assertTrue(payload.toString().contains(uptime));
        assertTrue(payload.toString().contains(displayName));
    }

    @Test
    public void testCopy() {
        String uptime = "0 days 1:2:34 hms";
        String displayName = "dname";
        KuraDisconnectPayload payload = new KuraDisconnectPayload(uptime, displayName);
        byte[] bytes = { 1, 2, 3 };
        payload.setBody(bytes);

        KuraDisconnectPayload payload2 = new KuraDisconnectPayload(payload);

        assertEquals(uptime, payload2.getUptime());
        assertEquals(displayName, payload2.getDisplayName());
        assertArrayEquals(bytes, payload2.getBody());
    }

}
