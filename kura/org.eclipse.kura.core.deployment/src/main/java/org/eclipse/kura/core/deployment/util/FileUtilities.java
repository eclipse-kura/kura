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

package org.eclipse.kura.core.deployment.util;

public class FileUtilities {

    public static String getFileName(String dpName, String dpVersion, String extension) {
        return getFileName(dpName, dpVersion, extension, "-");
    }

    public static String getFileName(String dpName, String dpVersion, String extension, String separator) {
        return dpName + separator + dpVersion + extension;
    }
}
