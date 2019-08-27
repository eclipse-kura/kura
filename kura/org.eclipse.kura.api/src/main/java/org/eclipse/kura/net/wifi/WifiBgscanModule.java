/*******************************************************************************
 * Copyright (c) 2011, 2019 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.net.wifi;

/**
 * Module for background scan
 */
public enum WifiBgscanModule {

    NONE(0x00),
    SIMPLE(0x01),
    LEARN(0x02);

    private int code;

    private WifiBgscanModule(int code) {
        this.code = code;
    }

    public static WifiBgscanModule parseCode(int code) {
        for (WifiBgscanModule module : WifiBgscanModule.values()) {
            if (module.code == code) {
                return module;
            }
        }
        return null;
    }

    public static int getCode(WifiBgscanModule modules) {
        for (WifiBgscanModule module : WifiBgscanModule.values()) {
            if (module == modules) {
                return module.code;
            }
        }
        return -1;
    }
}
