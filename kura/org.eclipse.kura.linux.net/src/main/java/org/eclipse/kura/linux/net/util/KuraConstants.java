/*******************************************************************************
 * Copyright (c) 2011, 2018 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Red Hat Inc
 *******************************************************************************/
package org.eclipse.kura.linux.net.util;

public enum KuraConstants {
    // image name, version
    Reliagate_10_20("reliagate-10-20"),
    ReliaGATE_10_05("reliagate-10-05"),
    Reliagate_10_11("reliagate-10-11"),
    Reliagate_10_12("reliagate-10-12"),
    Reliagate_20_25("reliagate-20-25"),
    BoltGATE_20_25("boltgate-20-25"),
    Reliagate_20_26("reliagate-20-26");

    private String targetName;

    private KuraConstants(String targetName) {
        this.targetName = targetName;
    }

    public String getTargetName() {
        return this.targetName;
    }

}
