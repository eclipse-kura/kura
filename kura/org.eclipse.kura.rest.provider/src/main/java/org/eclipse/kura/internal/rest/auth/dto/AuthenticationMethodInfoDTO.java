package org.eclipse.kura.internal.rest.auth.dto;

public class AuthenticationMethodInfoDTO {

    private final boolean passwordAuthenticationEnabled;
    private final boolean certificateAuthenticationEnabled;
    private final Integer certificateAuthenticationPort;

    public AuthenticationMethodInfoDTO(boolean passwordAuthenticationEnabled, boolean certificateAuthenticationEnabled,
            Integer certificateAuthenticationPort) {
        this.passwordAuthenticationEnabled = passwordAuthenticationEnabled;
        this.certificateAuthenticationEnabled = certificateAuthenticationEnabled;
        this.certificateAuthenticationPort = certificateAuthenticationPort;
    }

    public boolean isPasswordAuthenticationEnabled() {
        return passwordAuthenticationEnabled;
    }

    public boolean isCertificateAuthenticationEnabled() {
        return certificateAuthenticationEnabled;
    }

    public Integer getCertificateAuthenticationPort() {
        return certificateAuthenticationPort;
    }

}
