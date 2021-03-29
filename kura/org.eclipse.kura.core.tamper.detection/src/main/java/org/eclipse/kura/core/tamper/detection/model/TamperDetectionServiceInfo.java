/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
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
package org.eclipse.kura.core.tamper.detection.model;

public class TamperDetectionServiceInfo {

    @SuppressWarnings("unused")
    private final String pid;
    @SuppressWarnings("unused")
    private final String displayName;

    public TamperDetectionServiceInfo(String pid, String displayName) {
        this.pid = pid;
        this.displayName = displayName;
    }
}
