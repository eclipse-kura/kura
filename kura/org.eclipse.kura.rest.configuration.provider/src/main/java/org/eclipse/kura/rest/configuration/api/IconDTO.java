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
 ******************************************************************************/
package org.eclipse.kura.rest.configuration.api;

import java.math.BigInteger;

import org.eclipse.kura.configuration.metatype.Icon;

public class IconDTO implements Icon {

    private final String resource;
    private final BigInteger size;

    public IconDTO(final Icon icon) {
        this.resource = icon.getResource();
        this.size = icon.getSize();
    }

    @Override
    public String getResource() {
        return resource;
    }

    @Override
    public BigInteger getSize() {
        return size;
    }

}
