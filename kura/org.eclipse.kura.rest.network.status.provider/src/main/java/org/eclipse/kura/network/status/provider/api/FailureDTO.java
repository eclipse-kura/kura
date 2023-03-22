package org.eclipse.kura.network.status.provider.api;

@SuppressWarnings("unused")
public class FailureDTO {

    private final String interfaceId;
    private final String reason;

    public FailureDTO(final String interfaceId, final String reason) {
        this.interfaceId = interfaceId;
        this.reason = reason;
    }

    public FailureDTO(String interfaceId, final Exception reason) {
        this(interfaceId, reason.getMessage() != null ? reason.getMessage() : "Unknown error");
    }

}
