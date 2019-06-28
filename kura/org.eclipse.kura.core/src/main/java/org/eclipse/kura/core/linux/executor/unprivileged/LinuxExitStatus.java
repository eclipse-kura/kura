package org.eclipse.kura.core.linux.executor.unprivileged;

import org.eclipse.kura.executor.ExitStatus;

public class LinuxExitStatus implements ExitStatus {

    private Integer exitStatus;

    public LinuxExitStatus(int exitStatus) {
        this.exitStatus = exitStatus;
    }

    @Override
    public Object getExitValue() {
        return this.exitStatus;
    }

}
