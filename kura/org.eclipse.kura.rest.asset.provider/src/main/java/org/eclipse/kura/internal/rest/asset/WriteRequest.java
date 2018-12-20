/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.internal.rest.asset;

import org.eclipse.kura.channel.ChannelRecord;
import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;

public class WriteRequest implements Validable {

    private String name;
    private DataType type;
    private String value;

    public String getName() {
        return this.name;
    }

    public DataType getType() {
        return this.type;
    }

    public TypedValue<?> getValue() {
        return TypedValues.parseTypedValue(this.type, this.value);
    }

    public ChannelRecord toChannelRecord() {
        return ChannelRecord.createWriteRecord(this.name, getValue());
    }

    @Override
    public String toString() {
        return "WriteRequest [name=" + this.name + ", type=" + this.type + ", value=" + getValue() + "]";
    }

    @Override
    public boolean isValid() {
        return this.name != null && this.type != null && this.value != null;
    }

}
