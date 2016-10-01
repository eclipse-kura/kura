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
package org.eclipse.kura.cloud.app.command;

import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraResponsePayload;

public class KuraCommandResponsePayload extends KuraResponsePayload {

    public static final String METRIC_STDERR = "command.stderr";
    public static final String METRIC_STDOUT = "command.stdout";
    public static final String METRIC_EXIT_CODE = "command.exit.code";
    public static final String METRIC_TIMEDOUT = "command.timedout";

    public KuraCommandResponsePayload(KuraPayload kuraPayload) {
        super(kuraPayload);
    }

    public KuraCommandResponsePayload(int responseCode, Throwable t) {
        super(responseCode, t);
    }

    public KuraCommandResponsePayload(int responseCode) {
        super(responseCode);
    }

    public KuraCommandResponsePayload(Throwable t) {
        super(t);
    }

    public String getStderr() {
        return (String) getMetric(METRIC_STDERR);
    }

    public void setStderr(String stderr) {
        if (stderr != null) {
            addMetric(METRIC_STDERR, stderr);
        }
    }

    public String getStdout() {
        return (String) getMetric(METRIC_STDOUT);
    }

    public void setStdout(String stdout) {
        if (stdout != null) {
            addMetric(METRIC_STDOUT, stdout);
        }
    }

    public Integer getExitCode() {
        return (Integer) getMetric(METRIC_EXIT_CODE);
    }

    public void setExitCode(Integer exitCode) {
        if (exitCode != null) {
            addMetric(METRIC_EXIT_CODE, exitCode);
        }
    }

    public Boolean isTimedout() {
        return (Boolean) getMetric(METRIC_TIMEDOUT);
    }

    public void setTimedout(boolean timedout) {
        addMetric(METRIC_TIMEDOUT, timedout);
    }
}
