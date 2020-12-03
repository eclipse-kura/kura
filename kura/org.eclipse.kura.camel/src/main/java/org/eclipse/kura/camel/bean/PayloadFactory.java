/*******************************************************************************
 * Copyright (c) 2016, 2020 Red Hat Inc and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.camel.bean;

import java.util.Date;

import org.eclipse.kura.message.KuraPayload;

public class PayloadFactory {

    public KuraPayload create(final String key, final Object value) {
        final KuraPayload result = new KuraPayload();
        result.setTimestamp(new Date());
        result.addMetric(key, value);
        return result;
    }

    public KuraPayload create(final Date timestamp) {
        final KuraPayload result = new KuraPayload();
        result.setTimestamp(timestamp);
        return result;
    }

    public KuraPayload append(final KuraPayload payload, final String key, final Object value) {
        payload.addMetric(key, value);
        return payload;
    }
}
