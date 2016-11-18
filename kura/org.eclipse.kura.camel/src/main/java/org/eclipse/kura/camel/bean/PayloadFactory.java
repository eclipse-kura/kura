/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc
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
