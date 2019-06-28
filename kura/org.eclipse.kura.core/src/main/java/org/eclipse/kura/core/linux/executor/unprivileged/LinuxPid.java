package org.eclipse.kura.core.linux.executor.unprivileged;

import org.eclipse.kura.executor.Pid;

public class LinuxPid implements Pid {

    private Integer pid;

    public LinuxPid(int pid) {
        this.pid = pid;
    }

    @Override
    public Object getPid() {
        return this.pid;
    }

}
