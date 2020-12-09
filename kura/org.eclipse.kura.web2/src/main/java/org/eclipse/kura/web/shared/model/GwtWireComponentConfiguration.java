/*******************************************************************************
 * Copyright (c) 2016, 2020 Eurotech and/or its affiliates and others
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

/**
 * The Class GwtWireComponentConfiguration represents a POJO for every Wire
 * Component present in the system
 */
public final class GwtWireComponentConfiguration extends GwtBaseModel implements Serializable {

    /** Serial Version */
    private static final long serialVersionUID = 50782654510063453L;

    private GwtConfigComponent configuration;
    private int inputPortCount;
    private int outputPortCount;
    private double positionX;
    private double positionY;

    public GwtConfigComponent getConfiguration() {
        return this.configuration;
    }

    public void setConfiguration(GwtConfigComponent configuration) {
        this.configuration = configuration;
    }

    public int getInputPortCount() {
        return this.inputPortCount;
    }

    public void setInputPortCount(int inputPortCount) {
        this.inputPortCount = inputPortCount;
    }

    public int getOutputPortCount() {
        return this.outputPortCount;
    }

    public void setOutputPortCount(int outputPortCount) {
        this.outputPortCount = outputPortCount;
    }

    public double getPositionX() {
        return this.positionX;
    }

    public void setPositionX(double positionX) {
        this.positionX = positionX;
    }

    public double getPositionY() {
        return this.positionY;
    }

    public void setPositionY(double positionY) {
        this.positionY = positionY;
    }

}
