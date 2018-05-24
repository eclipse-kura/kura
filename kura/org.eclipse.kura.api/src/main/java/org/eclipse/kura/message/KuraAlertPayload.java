/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.message;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.osgi.annotation.versioning.ProviderType;

/**
 * @since 2.0
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class KuraAlertPayload extends KuraPayload {

    public static final String CODE_METRIC_NAME = "alert_code";
    public static final String SEVERITY_METRIC_NAME = "alert_severity";
    public static final String STATUS_METRIC_NAME = "alert_status";
    public static final String MESSAGE_METRIC_NAME = "alert_message";
    public static final String CREATION_TIMESTAMP_METRIC_NAME = "alert_creation_date";

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");

    public KuraAlertPayload(final String code, final KuraAlertSeverity severity, final KuraAlertStatus status) {
        setCode(code);
        setSeverity(severity);
        setStatus(status);
    }

    public void setCode(final String code) {
        super.addMetric(CODE_METRIC_NAME, validateCode(code));
    }

    public void setSeverity(final KuraAlertSeverity severity) {
        super.addMetric(SEVERITY_METRIC_NAME, severity.name());
    }

    public void setStatus(final KuraAlertStatus status) {
        super.addMetric(STATUS_METRIC_NAME, status.name());
    }

    public void setCreationTimestamp(final Date date) {
        super.addMetric(CREATION_TIMESTAMP_METRIC_NAME, date.toString());
    }

    public void setMessage(final String message) {
        if (message == null) {
            super.removeMetric(MESSAGE_METRIC_NAME);
        } else {
            super.addMetric(MESSAGE_METRIC_NAME, message);
        }
    }

    public String getCode() {
        return (String) getMetric(CODE_METRIC_NAME);
    }

    public KuraAlertSeverity getSeverity() {
        return KuraAlertSeverity.valueOf((String) getMetric(SEVERITY_METRIC_NAME));
    }

    public KuraAlertStatus getStatus() {
        return KuraAlertStatus.valueOf((String) getMetric(STATUS_METRIC_NAME));
    }

    public String getMessage() {
        final Object rawMessage = getMetric(MESSAGE_METRIC_NAME);

        if (rawMessage instanceof String) {
            return (String) rawMessage;
        }

        return null;
    }

    public Date getCreationTimestamp() {
        try {
            return DATE_FORMAT.parse((String) getMetric(CREATION_TIMESTAMP_METRIC_NAME));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void addMetric(String name, Object value) {
        if (CODE_METRIC_NAME.equals(name)) {
            setCode((String) value);
            return;
        } else if (MESSAGE_METRIC_NAME.equals(name)) {
            setMessage((String) value);
            return;
        } else if (CREATION_TIMESTAMP_METRIC_NAME.equals(name)) {
            setCreationTimestamp((Date) value);
            return;
        } else if (SEVERITY_METRIC_NAME.equals(name)) {
            KuraAlertSeverity.valueOf((String) value);
        } else if (STATUS_METRIC_NAME.equals(name)) {
            KuraAlertStatus.valueOf((String) value);
        }

        super.addMetric(name, value);
    }

    private String validateCode(final String code) {
        final String trimmed = code.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("alert code cannot be empty");
        }
        return trimmed;
    }
}
