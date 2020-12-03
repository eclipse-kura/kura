/*******************************************************************************
 * Copyright (c) 2017, 2020 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.internal.rest.asset;

import java.util.List;

import org.eclipse.kura.rest.utils.Validable;

public class WriteRequestList implements Validable {

    private List<WriteRequest> channels;

    public List<WriteRequest> getRequests() {
        return channels;
    }

    @Override
    public boolean isValid() {
        if (channels == null) {
            return false;
        }
        for (WriteRequest request : channels) {
            if (!request.isValid()) {
                return false;
            }
        }
        return true;
    }
}
