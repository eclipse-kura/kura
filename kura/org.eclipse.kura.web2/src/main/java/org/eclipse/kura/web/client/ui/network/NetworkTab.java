/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.network;

import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;

public interface NetworkTab extends Tab {

    public void setNetInterface(GwtNetInterfaceConfig config);

    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf);
}
