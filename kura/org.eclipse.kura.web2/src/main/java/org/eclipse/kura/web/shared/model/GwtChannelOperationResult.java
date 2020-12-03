/*******************************************************************************
 * Copyright (c) 2018, 2020 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
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
        return this.records;
    }

    public String getExceptionMessage() {
        return this.exceptionMessage;
    }

    public StackTraceElement[] getStackTrace() {
        return this.stackTrace;
    }
}
