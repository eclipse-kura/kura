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

public class FilterDTO {

    private List<String> names;
    private List<String> groupNames;

    public FilterDTO() {
        this.names = new ArrayList<>();
        this.groupNames = new ArrayList<>();
    }

    public List<String> getNames() {
        return this.names;
    }

    public List<String> getGroupNames() {
        return this.groupNames;
    }

}
