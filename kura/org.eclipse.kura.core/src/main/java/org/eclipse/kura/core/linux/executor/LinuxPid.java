/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.linux.executor;

import org.eclipse.kura.executor.Pid;

public class LinuxPid implements Pid {

    private final int pid;

    public LinuxPid(int pid) {
        this.pid = pid;
    }

    @Override
    public int getPid() {
        return this.pid;
    }

    @Override
    public String toString() {
        return String.valueOf(this.pid);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.pid;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LinuxPid other = (LinuxPid) obj;
        return this.pid != other.pid;
    }

}