/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
import java.util.Date;

public class GwtLogEntry extends KuraBaseModel implements Serializable {

    private static final long serialVersionUID = 8526545631929936271L;

    public void setSourceLogReaderPid(String sourceLogReaderPid) {
        super.set("sourcePid", sourceLogReaderPid);
    }

    public String getSourceLogReaderPid() {
        return super.get("sourcePid");
    }

    public void setTimestamp(String timestamp) {
        super.set("timestamp", timestamp);
    }

    public String getSourceRealtimeTimestamp() {
        String time = (String) super.get("_SOURCE_REALTIME_TIMESTAMP");
        try {
            return new Date(Long.parseLong(time.substring(0, 13))).toString();
        } catch (Exception ex) {
            return time;
        }
    }

    public String getPid() {
        return super.get("_PID");
    }

    public String getSyslogIdentifier() {
        return super.get("SYSLOG_IDENTIFIER");
    }

    public String getTransport() {
        return super.get("_TRANSPORT");
    }

    public String getPriority() {
        return super.get("PRIORITY");
    }

    public String getMessage() {
        return super.get("MESSAGE");
    }

    public String getStacktrace() {
        return super.get("STACKTRACE");
    }

    public String getCodeLine() {
        return super.get("CODE_LINE");
    }
}