/*******************************************************************************
 * Copyright (c) 2011, 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.linux.util;

import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.kura.core.util.SafeProcess;

/**
 * @deprecated since {@link org.eclipse.kura.core.util} version 1.3
 */
@Deprecated
public class ProcessStats {

    private final SafeProcess process;

    public ProcessStats(SafeProcess proc) {
        this.process = proc;
    }

    public SafeProcess getProcess() {
        return this.process;
    }

    public OutputStream getOutputStream() {
        return this.process.getOutputStream();
    }

    public InputStream getInputStream() {
        return this.process.getInputStream();
    }

    public InputStream getErrorStream() {
        return this.process.getErrorStream();
    }

    public int getReturnValue() {
        return this.process.exitValue();
    }
}
