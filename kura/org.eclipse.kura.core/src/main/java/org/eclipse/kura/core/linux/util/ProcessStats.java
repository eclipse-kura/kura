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
package org.eclipse.kura.core.linux.util;

import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.kura.core.util.SafeProcess;

public class ProcessStats {

    private final SafeProcess m_process;

    public ProcessStats(SafeProcess proc) {
        this.m_process = proc;
    }

    public SafeProcess getProcess() {
        return this.m_process;
    }

    public OutputStream getOutputStream() {
        return this.m_process.getOutputStream();
    }

    public InputStream getInputStream() {
        return this.m_process.getInputStream();
    }

    public InputStream getErrorStream() {
        return this.m_process.getErrorStream();
    }

    public int getReturnValue() {
        return this.m_process.exitValue();
    }
}
