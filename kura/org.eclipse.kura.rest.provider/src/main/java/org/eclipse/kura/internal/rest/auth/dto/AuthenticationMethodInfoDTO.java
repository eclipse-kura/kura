package org.eclipse.kura.internal.rest.auth.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AuthenticationMethodInfoDTO {

    private final boolean passwordAuthenticationEnabled;
    private final boolean certificateAuthenticationEnabled;
    private final List<Integer> certificateAuthenticationPorts;

    public AuthenticationMethodInfoDTO(boolean passwordAuthenticationEnabled, boolean certificateAuthenticationEnabled,
            Set<Integer> certificateAuthenticationPorts) {
        this.passwordAuthenticationEnabled = passwordAuthenticationEnabled;
        this.certificateAuthenticationEnabled = certificateAuthenticationEnabled;

        if (certificateAuthenticationPorts != null) {
            this.certificateAuthenticationPorts = new ArrayList<>(certificateAuthenticationPorts);
            this.certificateAuthenticationPorts.sort(null);
        } else {
            this.certificateAuthenticationPorts = null;
        }
    }

    public boolean isPasswordAuthenticationEnabled() {
        return passwordAuthenticationEnabled;
    }

    public boolean isCertificateAuthenticationEnabled() {
        return certificateAuthenticationEnabled;
    }

    public List<Integer> getCertificateAuthenticationPorts() {
        return certificateAuthenticationPorts;
    }

}
