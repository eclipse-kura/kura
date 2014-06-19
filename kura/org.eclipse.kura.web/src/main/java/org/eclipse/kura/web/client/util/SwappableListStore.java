/**
 * Copyright (c) 2011, 2014 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.web.client.util;

import com.extjs.gxt.ui.client.data.ListLoader;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.store.ListStore;


public class SwappableListStore<M extends ModelData> extends ListStore<M> {

    public SwappableListStore(ListLoader<?> loader) {
        super(loader);
    }

    public void swapModelInstance (M oldModel, M newModel) {
        super.swapModelInstance (oldModel, newModel);
    }
}

