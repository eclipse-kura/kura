package org.eclipse.kura.internal.rest.services.provider.dto;

import java.util.List;
import java.util.Objects;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;

public class InterfacesIdsDTO {

    private final List<String> interfacesIds;

    public InterfacesIdsDTO(final List<String> interfaceIds) {
        this.interfacesIds = interfaceIds;
    }

    public void idsValidation() throws KuraException {

        if (this.interfacesIds == null) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "interfacesIds must not be null");
        }

        if (this.interfacesIds.isEmpty()) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "interfacesIds must not be empty");
        }

        if (this.interfacesIds.stream().anyMatch(Objects::isNull)) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "none of the interfacesIds can be null");
        }

        if (this.interfacesIds.stream().anyMatch(i -> i.trim().isEmpty())) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "none of the interfacesIds can be empty");
        }
    }

    public List<String> getInterfacesIds() {
        return this.interfacesIds;
    }
}
