/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.internal.wire.conditional;

import jdk.nashorn.api.scripting.AbstractJSObject;

public class ImmutableJSObject extends AbstractJSObject {

    private static final String READ_ONLY_ERROR_MESSAGE = "This object is read-only";

    @Override
    public void setMember(String name, Object value) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    @Override
    public void setSlot(int index, Object value) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }

    @Override
    public void removeMember(String name) {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MESSAGE);
    }
}
