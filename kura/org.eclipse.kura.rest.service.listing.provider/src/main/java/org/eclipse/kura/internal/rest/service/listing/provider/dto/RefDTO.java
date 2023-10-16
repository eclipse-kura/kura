/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.internal.rest.service.listing.provider.dto;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;

public class RefDTO {

    final String pid;
    final String referenceName;

    public RefDTO(String pid, String referenceName) {
        this.pid = pid;
        this.referenceName = referenceName;
    }

    public String getPid() {
        return pid;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public void validate() throws KuraException {

        if (this.pid == null) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "pid must not be null");
        }

        if (this.pid.trim().isEmpty()) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "pid must not be empty");
        }

        if (this.referenceName == null) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "referenceName must not be null");
        }

        if (this.referenceName.trim().isEmpty()) {
            throw new KuraException(KuraErrorCode.BAD_REQUEST, "referenceName must not be empty");
        }
    }

}
