/*******************************************************************************
 * Copyright (c) 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.driver.binary.adapter;

import org.eclipse.kura.driver.binary.BinaryData;
import org.eclipse.kura.driver.binary.Buffer;
import org.eclipse.kura.driver.binary.Endianness;

public class ToBoolean implements BinaryData<Boolean> {

    final BinaryData<?> wrapped;

    public <T extends Number> ToBoolean(final BinaryData<T> wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void write(final Buffer buf, final int offset, final Boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean read(final Buffer buf, int offset) {
        return ((Number) wrapped.read(buf, offset)).doubleValue() != 0;
    }

    @Override
    public Class<Boolean> getValueType() {
        return Boolean.class;
    }

    @Override
    public Endianness getEndianness() {
        return wrapped.getEndianness();
    }

    @Override
    public int getSize() {
        return wrapped.getSize();
    }

}
