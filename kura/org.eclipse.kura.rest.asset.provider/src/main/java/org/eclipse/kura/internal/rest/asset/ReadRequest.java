/*******************************************************************************
 * Copyright (c) 2017, 2019 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.kura.internal.rest.asset;

import java.util.Set;

import org.eclipse.kura.rest.utils.Validable;

public class ReadRequest implements Validable {

    private Set<String> channels;

    public Set<String> getChannelNames() {
        return channels;
    }

    @Override
    public boolean isValid() {
        return channels != null;
    }
}
