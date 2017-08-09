/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.internal.rest.asset;

import java.util.List;

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
