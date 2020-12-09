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

import java.text.ParseException;

import org.osgi.annotation.versioning.ProviderType;

/**
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class KuraRequestPayload extends KuraPayload {

    public static final String METRIC_REQUEST_ID = "request.id";
    public static final String REQUESTER_CLIENT_ID = "requester.client.id";

    public KuraRequestPayload() {
        super();
    }

    public KuraRequestPayload(KuraPayload payload) {
        super();

        for (String name : payload.metricNames()) {
            Object value = payload.getMetric(name);
            addMetric(name, value);
        }

        setBody(payload.getBody());
        setPosition(payload.getPosition());
        setTimestamp(payload.getTimestamp());
    }

    public String getRequestId() {
        return (String) getMetric(METRIC_REQUEST_ID);
    }

    public void setRequestId(String requestId) {
        addMetric(METRIC_REQUEST_ID, requestId);
    }

    public String getRequesterClientId() {
        return (String) getMetric(REQUESTER_CLIENT_ID);
    }

    public void setRequesterClientId(String requesterClientId) {
        addMetric(REQUESTER_CLIENT_ID, requesterClientId);
    }

    public static KuraRequestPayload buildFromKuraPayload(KuraPayload payload) throws ParseException {
        if (payload.getMetric(METRIC_REQUEST_ID) == null) {
            throw new ParseException("Not a valid request payload", 0);
        }
        KuraRequestPayload requestPayload = new KuraRequestPayload(payload);
        return requestPayload;
    }
}
