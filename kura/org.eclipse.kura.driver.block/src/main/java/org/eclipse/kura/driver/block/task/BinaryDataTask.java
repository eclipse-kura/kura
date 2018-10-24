/*******************************************************************************
 * Copyright (c) 2017, 2018 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/

package org.eclipse.kura.driver.block.task;

import java.io.IOException;
import java.util.function.Function;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.driver.binary.BinaryData;
import org.eclipse.kura.driver.binary.Buffer;
import org.eclipse.kura.driver.binary.TypeUtil;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinaryDataTask<T> extends ChannelBlockTask {

    private static final Logger logger = LoggerFactory.getLogger(BinaryDataTask.class);

    private BinaryData<T> dataType;

    private Function<T, TypedValue<?>> toTypedValue;
    private Function<TypedValue<?>, T> fromTypedValue;

    @SuppressWarnings("unchecked")
    public BinaryDataTask(ChannelRecord record, int offset, BinaryData<T> dataType, Mode mode) {
        this(record, offset, dataType, TypedValues::newTypedValue, typedValue -> (T) typedValue.getValue(), mode);
    }

    public BinaryDataTask(ChannelRecord record, int offset, BinaryData<T> binaryDataType, DataType dataType,
            Mode mode) {
        this(record, offset, binaryDataType, TypeUtil.toTypedValue(binaryDataType.getValueType(), dataType),
                TypeUtil.fromTypedValue(binaryDataType.getValueType(), dataType), mode);
    }

    public BinaryDataTask(ChannelRecord record, int offset, BinaryData<T> dataType,
            Function<T, TypedValue<?>> toTypedValue, Function<TypedValue<?>, T> fromTypedValue, Mode mode) {
        super(record, offset, offset + dataType.getSize(), mode);
        this.dataType = dataType;
        this.toTypedValue = toTypedValue;
        this.fromTypedValue = fromTypedValue;
    }

    @Override
    public void run() throws IOException {
        final ToplevelBlockTask parent = getParent();
        Buffer buffer = parent.getBuffer();

        if (getMode() == Mode.READ) {
            logger.debug("Read {}: offset: {}", this.dataType.getClass().getSimpleName(), getStart());

            final T result = this.dataType.read(buffer, getStart() - parent.getStart());

            this.record.setValue(this.toTypedValue.apply(result));
            onSuccess();
        } else {
            logger.debug("Write {}: offset: {}", this.dataType.getClass().getSimpleName(), getStart());

            T value = this.fromTypedValue.apply(this.record.getValue());

            this.dataType.write(buffer, getStart() - parent.getStart(), value);
        }
    }

}
