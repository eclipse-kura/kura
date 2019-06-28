/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates
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

import org.eclipse.kura.executor.ExitValue;

public class LinuxExitValue implements ExitValue {

    private Integer exitValue;

    public LinuxExitValue(int exitStatus) {
        this.exitValue = exitStatus;
    }

    @Override
    public Object getExitValue() {
        return this.exitValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((exitValue == null) ? 0 : exitValue.hashCode());
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
        LinuxExitValue other = (LinuxExitValue) obj;
        if (exitValue == null) {
            if (other.exitValue != null)
                return false;
        } else if (!exitValue.equals(other.exitValue))
            return false;
        return true;
    }

}
