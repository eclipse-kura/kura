/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.message;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.osgi.annotation.versioning.ProviderType;

/**
 * @noextend This class is not intended to be subclassed by clients.
 */
@ProviderType
public class KuraResponsePayload extends KuraPayload {

    public static final int RESPONSE_CODE_OK = 200;
    public static final int RESPONSE_CODE_BAD_REQUEST = 400;
    public static final int RESPONSE_CODE_NOTFOUND = 404;
    public static final int RESPONSE_CODE_ERROR = 500;

    public static final String METRIC_RESPONSE_CODE = "response.code";
    public static final String METRIC_EXCEPTION_MSG = "response.exception.message";
    public static final String METRIC_EXCEPTION_STACK = "response.exception.stack";

    public KuraResponsePayload(int responseCode) {
        super();
        addMetric(METRIC_RESPONSE_CODE, Integer.valueOf(responseCode));
    }

    public KuraResponsePayload(Throwable t) {
        this(RESPONSE_CODE_ERROR, t);
    }

    public KuraResponsePayload(int responseCode, Throwable t) {
        super();
        addMetric(METRIC_RESPONSE_CODE, Integer.valueOf(responseCode));
        setException(t);
    }

    public KuraResponsePayload(KuraPayload kuraPayload) {
        for (String name : kuraPayload.metricNames()) {
            Object value = kuraPayload.getMetric(name);
            addMetric(name, value);
        }
        setBody(kuraPayload.getBody());
    }

    public int getResponseCode() {
        return (Integer) getMetric(METRIC_RESPONSE_CODE);
    }

    public void setResponseCode(int responseCode) {
        addMetric(METRIC_RESPONSE_CODE, Integer.valueOf(responseCode));
    }

    public String getExceptionMessage() {
        return (String) getMetric(METRIC_EXCEPTION_MSG);
    }

    public void setExceptionMessage(String message) {
        if (message != null) {
            addMetric(METRIC_EXCEPTION_MSG, message);
        }
    }

    public String getExceptionStack() {
        return (String) getMetric(METRIC_EXCEPTION_STACK);
    }

    public void setExceptionStack(String stack) {
        if (stack != null) {
            addMetric(METRIC_EXCEPTION_STACK, stack);
        }
    }

    public void setException(Throwable t) {
        if (t != null) {
            addMetric(METRIC_EXCEPTION_MSG, t.getMessage());
            addMetric(METRIC_EXCEPTION_STACK, stackTraceAsString(t));
        }
    }

    private String stackTraceAsString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }
}
