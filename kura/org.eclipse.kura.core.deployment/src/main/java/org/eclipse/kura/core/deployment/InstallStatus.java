package org.eclipse.kura.core.deployment;

public enum InstallStatus {
    IDLE("IDLE"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED"),
    ALREADY_DONE("ALREADY DONE");

    private final String status;

    InstallStatus(String status) {
        this.status = status;
    }

    public String getStatusString() {
        return this.status;
    }
}