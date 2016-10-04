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
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

public class GwtCommandResponse implements Serializable {

    private static final long serialVersionUID = -6187902237651876015L;

    private String m_command;
    private String m_stdout;
    private int m_exitValue;

    public GwtCommandResponse() {
    }

    public String getCommand() {
        return this.m_command;
    }

    public void setCommand(String command) {
        this.m_command = command;
    }

    public String getStdout() {
        return this.m_stdout;
    }

    public void setStdout(String stdout) {
        this.m_stdout = stdout;
    }

    public int getExitValue() {
        return this.m_exitValue;
    }

    public void setExitValue(int value) {
        this.m_exitValue = value;
    }
}
