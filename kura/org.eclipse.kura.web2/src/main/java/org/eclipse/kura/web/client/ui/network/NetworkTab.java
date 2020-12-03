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
package org.eclipse.kura.web.client.ui.network;

import org.eclipse.kura.web.client.ui.Tab;
import org.eclipse.kura.web.shared.model.GwtNetInterfaceConfig;

public interface NetworkTab extends Tab {

    public void setNetInterface(GwtNetInterfaceConfig config);

    public void getUpdatedNetInterface(GwtNetInterfaceConfig updatedNetIf);
}
