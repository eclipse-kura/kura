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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.osgi.annotation.versioning.ProviderType;

/**
 * KuraPayload defines the recommended payload structure for the messages sent to a remote cloud platform.
 * It was defined as an open format that is flexible from the aspect of data modeling
 * yet is efficient when it comes to bandwidth conservation. The same payload model could be used by the REST API
 * - in which case it is serialized into XML or JSON as requested by the client - or uses the efficient
 * Google ProtoBuf when sent over an MQTT connection when the bandwidth is very important.
 * The KuraPayload contains the following fields: sentOn timestamp, an optional set of metrics represented as
 * name-value pairs, an optional position field to capture a GPS position, and an optional binary body.
 * <ul>
 * <li>sentOn: it is the timestamp when the data was captured and sent to the remote cloud platform.
 * <li>metrics: a metric is a data structure composed of the name, a value, and the type of the value.
 * When used with the REST API valid metric types are: string, double, int, float, long, boolean, base64Binary.
 *
 * Each payload can have zero or more metrics.
 * <li>position: it is an optional field used to capture a geo position associated to this payload.
 * <li>body: it is an optional part of the payload that allows additional information to be transmitted in any format
 * determined by the user.
 * </ul>
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class KuraPayload {

    /**
     * Timestamp when the data was captured and sent to the remote cloud platform.
     */
    private Date timestamp;

    /**
     * It is an optional field used to capture a geo position associated to this payload.
     */
    private KuraPosition position;

    /**
     * A metric is a data structure composed of the name, a value, and the type of the value.
     * When used with the REST API valid metric types are: string, double, int, float, long, boolean, base64Binary.
     * Each payload can have zero or more metrics.
     */
    private final Map<String, Object> metrics;

    /**
     * It is an optional part of the payload that allows additional information to be transmitted in any format
     * determined by the user.
     */
    private byte[] body;

    public KuraPayload() {
        this.metrics = new HashMap<>();
        this.body = null;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public KuraPosition getPosition() {
        return this.position;
    }

    public void setPosition(KuraPosition position) {
        this.position = position;
    }

    public Object getMetric(String name) {
        return this.metrics.get(name);
    }

    public void addMetric(String name, Object value) {
        this.metrics.put(name, value);
    }

    public void removeMetric(String name) {
        this.metrics.remove(name);
    }

    public void removeAllMetrics() {
        this.metrics.clear();
    }

    public Set<String> metricNames() {
        return Collections.unmodifiableSet(this.metrics.keySet());
    }

    public Iterator<String> metricsIterator() {
        return this.metrics.keySet().iterator();
    }

    public Map<String, Object> metrics() {
        return Collections.unmodifiableMap(this.metrics);
    }

    public byte[] getBody() {
        return this.body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }
}
