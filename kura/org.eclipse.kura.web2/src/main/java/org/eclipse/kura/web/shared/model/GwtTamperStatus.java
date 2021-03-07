/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

public class GwtTamperStatus implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -9128344445984113675L;

    private static final String TIMESTAMP = "timestamp";

    private boolean isTampered;
    private Long tamperInstant = null;

    public GwtTamperStatus() {
        super();
    }

    public GwtTamperStatus(final boolean isTampered, final Map<String, Object> properties) {
        this.isTampered = isTampered;
        this.tamperInstant = (Long) properties.get(TIMESTAMP);

    }

    public boolean isTampered() {
        return isTampered;
    }

    public Optional<Long> getTimestamp() {
        return Optional.ofNullable(tamperInstant);
    }
}
