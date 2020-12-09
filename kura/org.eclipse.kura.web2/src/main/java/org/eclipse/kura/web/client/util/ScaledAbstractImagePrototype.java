/*******************************************************************************
 * Copyright (c) 2011, 2020 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.web.client.util;

import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Image;

public class ScaledAbstractImagePrototype extends AbstractImagePrototype {

    private final AbstractImagePrototype m_aip;

    public ScaledAbstractImagePrototype(AbstractImagePrototype aip) {
        this.m_aip = aip;
    }

    @Override
    public void applyTo(Image image) {
        this.m_aip.applyTo(image);
    }

    @Override
    public Image createImage() {
        Image img = this.m_aip.createImage();
        return new Image(img.getUrl());
    }

    @Override
    public ImagePrototypeElement createElement() {
        ImagePrototypeElement imgElement = this.m_aip.createElement();
        imgElement.getStyle().setProperty("backgroundSize", "100%");
        return imgElement;
    }
}
