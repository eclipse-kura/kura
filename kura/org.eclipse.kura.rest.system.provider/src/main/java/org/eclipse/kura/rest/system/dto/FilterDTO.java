/*******************************************************************************
 * Copyright (c) 2023 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.rest.system.dto;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.kura.rest.configuration.api.FailureHandler;
import org.eclipse.kura.rest.configuration.api.Validable;

public class FilterDTO implements Validable {

    private List<String> names;

    public FilterDTO() {
        this.names = new ArrayList<>();
    }

    public List<String> getNames() {
        return this.names;
    }

    @Override
    public void validate() {
        FailureHandler.requireParameter(this.names, "names");
    }

}
