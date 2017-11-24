/*******************************************************************************
 * Copyright (c) 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
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
        return configuration;
    }

    public void setConfiguration(GwtConfigComponent configuration) {
        this.configuration = configuration;
    }

    public int getInputPortCount() {
        return inputPortCount;
    }

    public void setInputPortCount(int inputPortCount) {
        this.inputPortCount = inputPortCount;
    }

    public int getOutputPortCount() {
        return outputPortCount;
    }

    public void setOutputPortCount(int outputPortCount) {
        this.outputPortCount = outputPortCount;
    }

    public double getPositionX() {
        return positionX;
    }

    public void setPositionX(double positionX) {
        this.positionX = positionX;
    }

    public double getPositionY() {
        return positionY;
    }

    public void setPositionY(double positionY) {
        this.positionY = positionY;
    }

}
