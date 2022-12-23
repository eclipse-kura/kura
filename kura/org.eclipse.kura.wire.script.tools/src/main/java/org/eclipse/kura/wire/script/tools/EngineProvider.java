/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.wire.script.tools;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.kura.type.DataType;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;
import org.eclipse.kura.wire.WireRecord;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EngineProvider {

    private static final Logger logger = LoggerFactory.getLogger(EngineProvider.class);
    public static final String LANGUAGE_ID = "js";

    private Optional<Context> engine = Optional.empty();
    private Value bindings;
    private Optional<Value> currentResult;

    public void initEngine() {
        closeEngine();

        try {
            this.engine = Optional.of(Context.newBuilder(LANGUAGE_ID).option("engine.WarnInterpreterOnly", "false")
                    .allowHostAccess(HostAccess.ALL).build());
            createDefaultBindings();
        } catch (Exception e) {
            logger.error("Failed to initialize engine for language '" + LANGUAGE_ID + "'.", e);
            this.engine = Optional.empty();
        }
    }

    public void closeEngine() {
        if (this.engine.isPresent()) {
            this.engine.get().close(true);
            this.engine = Optional.empty();
        }
    }

    public boolean isEngineInit() {
        return this.engine.isPresent();
    }

    public void addBinding(String name, Object value) {
        this.bindings.putMember(name, value);
    }

    public Optional<List<WireRecord>> getBindingAsWireRecordList(String name) {
        Value searched = this.bindings.getMember(name);

        if (searched != null && searched.hasArrayElements()) {
            return Optional.of(valueToWireRecordList(searched));
        } else {
            logger.warn("Binding '{}' is either null or not an array.", name);
            return Optional.empty();
        }
    }

    public void evaluate(String sourceCode) {
        this.currentResult = Optional.empty();
        try {
            if (this.engine.isPresent()) {
                Source source = Source.newBuilder(LANGUAGE_ID, sourceCode, null).build();
                this.currentResult = Optional.of(this.engine.get().eval(source));
            } else {
                logger.warn("Engine is not loaded!");
            }
        } catch (PolyglotException pe) {
            logPolyglotException(pe);
        } catch (Exception e) {
            logger.warn("Failed to execute script.", e);
        }
    }

    public Optional<TypedValue<Boolean>> getResultAsBoolean() {
        if (currentResult.isPresent() && this.currentResult.get().isBoolean()) {
            return Optional.of(TypedValues.newBooleanValue(this.currentResult.get().asBoolean()));
        }
        return Optional.empty();

    }

    private List<WireRecord> valueToWireRecordList(Value value) {
        List<WireRecord> records = new LinkedList<>();

        for (int i = 0; i < value.getArraySize(); i++) {
            records.add(value.getArrayElement(i).as(WireRecord.class));
        }

        return records;
    }

    private void createDefaultBindings() {
        this.bindings = this.engine.get().getBindings(LANGUAGE_ID);

        this.bindings.removeMember("exit");
        this.bindings.removeMember("quit");

        this.bindings.putMember("logger", logger);
        this.bindings.putMember("newWireRecord", (Function<Map<String, TypedValue<?>>, WireRecord>) WireRecord::new);

        this.bindings.putMember("newBooleanValue", (Function<Boolean, TypedValue<?>>) TypedValues::newBooleanValue);
        this.bindings.putMember("newByteArrayValue", (Function<byte[], TypedValue<?>>) TypedValues::newByteArrayValue);
        this.bindings.putMember("newDoubleValue",
                (Function<Number, TypedValue<?>>) num -> TypedValues.newDoubleValue(num.doubleValue()));
        this.bindings.putMember("newFloatValue",
                (Function<Number, TypedValue<?>>) num -> TypedValues.newFloatValue(num.floatValue()));
        this.bindings.putMember("newIntegerValue",
                (Function<Number, TypedValue<?>>) num -> TypedValues.newIntegerValue(num.intValue()));
        this.bindings.putMember("newLongValue",
                (Function<Number, TypedValue<?>>) num -> TypedValues.newLongValue(num.longValue()));
        this.bindings.putMember("newStringValue",
                (Function<Object, TypedValue<?>>) obj -> TypedValues.newStringValue(obj.toString()));

        this.bindings.putMember("newByteArray", (Function<Integer, byte[]>) size -> new byte[size]);

        for (DataType type : DataType.values()) {
            this.bindings.putMember(type.name(), type);
        }
    }

    private void logPolyglotException(PolyglotException exception) {
        if (exception.getSourceLocation() != null) {
            logger.warn("Syntax error at {}:{} - {}:{}:\n{}", exception.getSourceLocation().getStartLine(),
                    exception.getSourceLocation().getStartColumn(), exception.getSourceLocation().getEndLine(),
                    exception.getSourceLocation().getEndLine(), exception.getMessage());
        } else {
            logger.warn("Runtime exception.", exception);
        }
    }

}
