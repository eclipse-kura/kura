/*******************************************************************************
 * Copyright (c) 2019, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

public class GwtWireGraph implements Serializable {

    private static final long serialVersionUID = -6374625271254988087L;

    private GwtWireComposerStaticInfo staticInfo;
    private GwtWireGraphConfiguration wireGraphConfiguration;

    public GwtWireGraph() {
    }

    public GwtWireGraph(final GwtWireComposerStaticInfo staticInfo,
            final GwtWireGraphConfiguration wireGraphConfiguration) {
        this.staticInfo = staticInfo;
        this.wireGraphConfiguration = wireGraphConfiguration;
    }

    public GwtWireComposerStaticInfo getStaticInfo() {
        return this.staticInfo;
    }

    public GwtWireGraphConfiguration getWireGraphConfiguration() {
        return this.wireGraphConfiguration;
    }
}
