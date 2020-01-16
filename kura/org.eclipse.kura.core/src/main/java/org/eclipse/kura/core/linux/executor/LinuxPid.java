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

import org.eclipse.kura.executor.Pid;

public class LinuxPid implements Pid {

    private int pid;

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
        result = prime * result + pid;
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
        LinuxPid other = (LinuxPid) obj;
        return this.pid != other.pid;
    }

}