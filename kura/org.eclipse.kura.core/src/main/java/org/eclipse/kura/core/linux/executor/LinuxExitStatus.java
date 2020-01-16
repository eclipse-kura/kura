/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.linux.executor;

import org.eclipse.kura.executor.ExitStatus;

public class LinuxExitStatus implements ExitStatus {

    private int exitValue;

    public LinuxExitStatus(int exitStatus) {
        this.exitValue = exitStatus;
    }

    @Override
    public int getExitCode() {
        return this.exitValue;
    }

    @Override
    public boolean isSuccessful() {
        return this.exitValue == 0;
    }

    @Override
    public String toString() {
        return String.valueOf(this.exitValue);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + exitValue;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LinuxExitStatus other = (LinuxExitStatus) obj;
        return this.exitValue == other.exitValue;
    }

}
