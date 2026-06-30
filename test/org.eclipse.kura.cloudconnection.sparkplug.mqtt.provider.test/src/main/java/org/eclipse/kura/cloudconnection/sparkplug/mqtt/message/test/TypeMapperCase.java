/*******************************************************************************
 * Copyright (c) 2024 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.cloudconnection.sparkplug.mqtt.message.test;

import java.util.Optional;

import org.eclipse.tahu.protobuf.SparkplugBProto.DataType;

class TypeMapperCase {

    private String name;
    private Object value;
    private long timestamp;
    private DataType expectedDataType;
    private Exception expectedException;

    public TypeMapperCase(String name, Object value, long timestamp, DataType expectedDataType) {
        this.name = name;
        this.value = value;
        this.timestamp = timestamp;
        this.expectedDataType = expectedDataType;
    }

    public TypeMapperCase(String name, Object value, long timestamp, DataType expectedDataType, Exception expectedException) {
        this(name, value, timestamp, expectedDataType);
        this.expectedException = expectedException;
    }

    public String getName() {
        return this.name;
    }

    public Object getValue() {
        return this.value;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public DataType getExpectedDataType() {
        return this.expectedDataType;
    }

    public Optional<Exception> getExpectedException() {
        return Optional.ofNullable(this.expectedException);
    }

}
