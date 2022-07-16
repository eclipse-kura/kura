/*******************************************************************************
 * Copyright (c) 2021, 2022 Eurotech and/or its affiliates and others
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

public class GwtLogEntry extends KuraBaseModel implements Serializable {

    private static final long serialVersionUID = 8526545631929936271L;
    private int id;
    private String timestamp;

    public enum LogEntryKeys {

        SOURCE_LOGPROVIDER_PID("SOURCE_LOGPROVIDER_PID"),
        PID("_PID"),
        SYSLOG_ID("SYSLOG_IDENTIFIER"),
        TRANSPORT("_TRANSPORT"),
        PRIORITY("PRIORITY"),
        MESSAGE("MESSAGE"),
        STACKTRACE("STACKTRACE"),
        CODE_LINE("CODE_LINE");

        private final String fieldKey;

        LogEntryKeys(String key) {
            this.fieldKey = key;
        }

        public String getKey() {
            return this.fieldKey;
        }
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setSourceLogProviderPid(String sourceLogProviderPid) {
        super.set(LogEntryKeys.SOURCE_LOGPROVIDER_PID.getKey(), sourceLogProviderPid);
    }

    public int getId() {
        return this.id;
    }

    public String getSourceLogProviderPid() {
        return super.get(LogEntryKeys.SOURCE_LOGPROVIDER_PID.getKey());
    }

    public String getSourceRealtimeTimestamp() {
        return this.timestamp;
    }

    public String getPid() {
        return super.get(LogEntryKeys.PID.getKey());
    }

    public String getSyslogIdentifier() {
        return super.get(LogEntryKeys.SYSLOG_ID.getKey());
    }

    public String getTransport() {
        return super.get(LogEntryKeys.TRANSPORT.getKey());
    }

    public String getPriority() {
        return super.get(LogEntryKeys.PRIORITY.getKey());
    }

    public String getMessage() {
        return super.get(LogEntryKeys.MESSAGE.getKey());
    }

    public String getStacktrace() {
        String stacktrace = super.get(LogEntryKeys.STACKTRACE.getKey());
        if (stacktrace == null || stacktrace.equals("undefined")) {
            return "";
        }
        return stacktrace;
    }

    public String prettyPrint(boolean includeMoreInfo, boolean includeStacktrace) {
        setUnescaped(true);
        StringBuilder prettyEntry = new StringBuilder();
        prettyEntry.append(getSourceRealtimeTimestamp());
        prettyEntry.append("\t[priority: ");
        prettyEntry.append(getPriority());

        if (includeMoreInfo) {
            prettyEntry.append(" - PID: ");
            prettyEntry.append(getPid());
            prettyEntry.append(" - syslog ID: ");
            prettyEntry.append(getSyslogIdentifier());
            prettyEntry.append(" - source: ");
            prettyEntry.append(getTransport());
        }

        prettyEntry.append("]\nMessage: ");
        prettyEntry.append(getMessage());

        if (includeStacktrace && !getStacktrace().isEmpty()) {
            prettyEntry.append("\nStacktrace: ");
            prettyEntry.append(getStacktrace());
        }

        prettyEntry.append("\n\n");
        return prettyEntry.toString();
    }
}