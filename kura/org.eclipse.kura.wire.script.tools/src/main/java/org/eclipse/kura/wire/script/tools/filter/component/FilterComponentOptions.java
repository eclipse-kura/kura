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
package org.eclipse.kura.wire.script.tools.filter.component;

import java.util.Map;
import java.util.Optional;

public class FilterComponentOptions {

    public static final String SCRIPT_KEY = "script";
    public static final String SCRIPT_CONTEXT_DROP_KEY = "script.context.drop";
    public static final boolean SCRIPT_CONTEXT_DROP_DEFAULT_VALUE = false;

    private String scriptSource;
    private boolean scriptContextDrop;

    public FilterComponentOptions(final Map<String, Object> properties) {
        this.scriptSource = (String) properties.get(SCRIPT_KEY);
        this.scriptSource = this.scriptSource == null ? "" : this.scriptSource.trim();

        this.scriptContextDrop = (boolean) properties.getOrDefault(SCRIPT_CONTEXT_DROP_KEY,
                SCRIPT_CONTEXT_DROP_DEFAULT_VALUE);
    }

    public Optional<String> getScriptSource() {
        if (this.scriptSource.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(this.scriptSource);
    }

    public boolean isScriptContextDrop() {
        return this.scriptContextDrop;
    }

}
