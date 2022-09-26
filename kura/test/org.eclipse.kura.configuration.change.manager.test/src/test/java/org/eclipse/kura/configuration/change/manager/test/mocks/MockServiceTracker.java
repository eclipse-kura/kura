/*******************************************************************************
 * Copyright (c) 2022 Eurotech and/or its affiliates and others
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

package org.eclipse.kura.configuration.change.manager.test.mocks;

import org.eclipse.kura.configuration.change.manager.ServiceTrackerListener;

public class MockServiceTracker {

    ServiceTrackerListener listener;

    public void setServiceTrackerListener(ServiceTrackerListener listener) {
        this.listener = listener;
    }

    public void simulateConfigChange(String changedPid) {
        listener.onConfigurationChanged(changedPid);
    }

}
