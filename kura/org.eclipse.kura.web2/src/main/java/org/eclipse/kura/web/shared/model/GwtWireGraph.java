/*******************************************************************************
 * Copyright (c) 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
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
        return staticInfo;
    }

    public GwtWireGraphConfiguration getWireGraphConfiguration() {
        return wireGraphConfiguration;
    }
}
