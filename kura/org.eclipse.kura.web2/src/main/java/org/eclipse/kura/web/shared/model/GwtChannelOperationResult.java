/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.List;

public class GwtChannelOperationResult extends GwtBaseModel implements Serializable {

    private static final long serialVersionUID = -5814021451099568578L;

    private String exceptionMessage;
    private StackTraceElement[] stackTrace;
    private List<GwtChannelRecord> records;

    public GwtChannelOperationResult() {
    }

    public GwtChannelOperationResult(final List<GwtChannelRecord> records) {
        this.records = records;
    }

    public GwtChannelOperationResult(final Throwable exception) {
        this.exceptionMessage = exception.getMessage();
        this.stackTrace = exception.getStackTrace();
    }

    public List<GwtChannelRecord> getRecords() {
        return records;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public StackTraceElement[] getStackTrace() {
        return stackTrace;
    }
}
