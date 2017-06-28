/**
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *  
 */
package org.eclipse.kura.wire.script.filter.localization;

import com.github.rodionmoiseev.c10n.annotations.En;

public interface ScriptFilterMessages {

    @En("Activating Script Filter.....")
    public String activating();

    @En("ActivatingScript Filter.....Done")
    public String activatingDone();

    @En("Deactivating Script Filter.....")
    public String deactivating();

    @En("Deactivating Script Filter.....Done")
    public String deactivatingDone();

    @En("Updating Script Filter.....")
    public String updating();

    @En("Updating Script Filter.....Done")
    public String updatingDone();

    @En("Failed to get script engine")
    public String errorGettingScriptEngine();

    @En("Script source is null")
    public String errorScriptSourceNull();

    @En("Failed to compile script")
    public String errorScriptCompileFalied();

    @En("Failed to execute script")
    public String errorExecutingScript();

    @En("Added object cannot be null")
    public String errorNonNull();

    @En("Added object must be a WireRecord")
    public String errorMustBeWireRecord();

    @En("WireRecord properties must be instances of TypedValue")
    public String errorMustBeTypedValue();

    @En("This object is immutable")
    public String errorObjectImmutable();

}